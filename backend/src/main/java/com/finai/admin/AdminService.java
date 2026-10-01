package com.finai.admin;

import com.finai.ai.AiChatService;
import com.finai.audit.AuditAction;
import com.finai.audit.AuditService;
import com.finai.category.Category;
import com.finai.category.CategoryRepository;
import com.finai.category.CategoryType;
import com.finai.common.ApiException;
import com.finai.common.PageResponse;
import com.finai.feedback.Feedback;
import com.finai.feedback.FeedbackCategory;
import com.finai.feedback.FeedbackRepository;
import com.finai.feedback.FeedbackStatus;
import com.finai.security.AuthPrincipal;
import com.finai.security.CurrentUserService;
import com.finai.transaction.TransactionRepository;
import com.finai.user.Role;
import com.finai.user.RoleRepository;
import com.finai.user.User;
import com.finai.user.UserRepository;
import com.finai.user.UserSpecifications;
import com.finai.user.UserStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Administrative operations.
 *
 * Two rules are enforced here rather than trusted to callers:
 *  - an administrator may never lock, deactivate or delete their own account,
 *    which would risk leaving the system with no way back in;
 *  - every mutation is written to the audit trail together with the actor,
 *    the target and the outcome.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final FeedbackRepository feedbackRepository;
    private final AiChatService aiChatService;
    private final AuditService auditService;
    private final SystemMetricsService systemMetricsService;
    private final CurrentUserService currentUserService;

    public AdminService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        CategoryRepository categoryRepository,
                        TransactionRepository transactionRepository,
                        FeedbackRepository feedbackRepository,
                        AiChatService aiChatService,
                        AuditService auditService,
                        SystemMetricsService systemMetricsService,
                        CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.feedbackRepository = feedbackRepository;
        this.aiChatService = aiChatService;
        this.auditService = auditService;
        this.systemMetricsService = systemMetricsService;
        this.currentUserService = currentUserService;
    }

    // ------------------------------------------------------------------ users

    public record AdminUserView(
            Long id,
            String email,
            String username,
            String fullName,
            String role,
            UserStatus status,
            long transactionCount,
            Instant lastLoginAt,
            Instant createdAt
    ) {
    }

    public record UpdateUserStatusRequest(
            UserStatus status,
            String reason
    ) {
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserView> listUsers(String search, UserStatus status, int page, int size) {
        Specification<User> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("fullName"), "")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<User> result = userRepository.findAll(specification,
                PageRequest.of(Math.max(page, 0), size <= 0 ? 20 : Math.min(size, 100),
                        Sort.by(Sort.Order.desc("createdAt"))));
        return PageResponse.of(result, this::toAdminUserView);
    }

    @Transactional(readOnly = true)
    public AdminUserView getUser(Long id) {
        return userRepository.findById(id)
                .map(this::toAdminUserView)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND",
                        "No user with id " + id + "."));
    }

    @Transactional
    public AdminUserView updateUserStatus(Long id, UpdateUserStatusRequest request) {
        AuthPrincipal admin = currentUserService.requirePrincipal();
        if (request.status() == null) {
            throw ApiException.badRequest("STATUS_REQUIRED", "A target status is required.");
        }
        if (id.equals(admin.getId())) {
            auditService.record(admin.getId(), admin.getUsername(), AuditAction.ADMIN_DENIED,
                    "USER", id, "FAILURE", "Administrator attempted to change their own status");
            throw ApiException.badRequest("CANNOT_MODIFY_SELF",
                    "You cannot change the status of your own account.");
        }

        User target = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND",
                        "No user with id " + id + "."));

        String roleName = roleRepository.findById(target.getRoleId())
                .map(Role::getName).orElse(Role.USER);
        if (Role.ADMIN.equals(roleName)) {
            auditService.record(admin.getId(), admin.getUsername(), AuditAction.ADMIN_DENIED,
                    "USER", id, "FAILURE", "Refused to change status of another administrator");
            throw ApiException.badRequest("CANNOT_MODIFY_OTHER_ADMIN",
                    "The status of another administrator account cannot be changed here.");
        }

        UserStatus previous = target.getStatus();
        if (previous == request.status()) {
            return toAdminUserView(target);
        }

        target.changeStatus(request.status());
        userRepository.save(target);

        String action = switch (request.status()) {
            case LOCKED -> AuditAction.ADMIN_LOCK_USER;
            case ACTIVE -> AuditAction.ADMIN_UNLOCK_USER;
            case DEACTIVATED -> AuditAction.ADMIN_UPDATE_USER_STATUS;
        };
        String detail = "status " + previous + " -> " + request.status()
                + (request.reason() == null || request.reason().isBlank()
                ? "" : "; reason: " + request.reason().trim());
        auditService.record(admin.getId(), admin.getUsername(), action, "USER", id, "SUCCESS", detail);

        return toAdminUserView(target);
    }

    // ------------------------------------------------------------- categories

    public record AdminCategoryView(
            Long id,
            String name,
            String code,
            CategoryType type,
            String icon,
            String color,
            boolean active,
            long usageCount
    ) {
    }

    public record UpsertSystemCategoryRequest(
            String name,
            String code,
            CategoryType type,
            String icon,
            String color,
            Boolean active
    ) {
    }

    @Transactional(readOnly = true)
    public List<AdminCategoryView> listSystemCategories() {
        return categoryRepository.findAll().stream()
                .filter(Category::isSystemCategory)
                .map(category -> new AdminCategoryView(category.getId(), category.getName(),
                        category.getCode(), category.getType(), category.getIcon(), category.getColor(),
                        category.isActive(), transactionCountFor(category.getId())))
                .toList();
    }

    @Transactional
    public AdminCategoryView createSystemCategory(UpsertSystemCategoryRequest request) {
        AuthPrincipal admin = currentUserService.requirePrincipal();
        String name = requireText(request.name(), "name");
        String code = requireText(request.code(), "code").toUpperCase().replaceAll("[^A-Z0-9]+", "_");
        if (request.type() == null) {
            throw ApiException.badRequest("CATEGORY_TYPE_REQUIRED", "A category type is required.");
        }
        if (categoryRepository.existsByCodeIgnoreCaseAndSystemCategoryTrue(code)) {
            throw ApiException.conflict("CATEGORY_CODE_EXISTS",
                    "A system category with code " + code + " already exists.");
        }

        Category category = categoryRepository.save(Category.system(
                name, code, request.type(), request.icon(), request.color()));
        auditService.record(admin.getId(), admin.getUsername(), AuditAction.ADMIN_CREATE_CATEGORY,
                "CATEGORY", category.getId(), "SUCCESS", "created system category " + code);

        return new AdminCategoryView(category.getId(), category.getName(), category.getCode(),
                category.getType(), category.getIcon(), category.getColor(), category.isActive(), 0L);
    }

    /**
     * Renames or deactivates a system category. It is never deleted: doing so would
     * either break the foreign key from transactions or force a decision about
     * other users' historical data.
     */
    @Transactional
    public AdminCategoryView updateSystemCategory(Long id, UpsertSystemCategoryRequest request) {
        AuthPrincipal admin = currentUserService.requirePrincipal();
        Category category = categoryRepository.findById(id)
                .filter(Category::isSystemCategory)
                .orElseThrow(() -> ApiException.notFound("SYSTEM_CATEGORY_NOT_FOUND",
                        "No system category with id " + id + "."));

        if (request.name() != null && !request.name().isBlank()) {
            category.changeDetails(request.name().trim(),
                    request.type() == null ? category.getType() : request.type(),
                    request.icon(), request.color());
        }
        if (request.active() != null) {
            category.changeActive(request.active());
        }
        categoryRepository.save(category);

        auditService.record(admin.getId(), admin.getUsername(), AuditAction.ADMIN_UPDATE_CATEGORY,
                "CATEGORY", id, "SUCCESS",
                "updated system category; active=" + category.isActive());

        return new AdminCategoryView(category.getId(), category.getName(), category.getCode(),
                category.getType(), category.getIcon(), category.getColor(), category.isActive(),
                transactionCountFor(category.getId()));
    }

    // --------------------------------------------------------------- feedback

    public record AdminFeedbackView(
            Long id,
            Long userId,
            String userEmail,
            String title,
            String content,
            FeedbackCategory category,
            FeedbackStatus status,
            String adminReply,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record UpdateFeedbackRequest(
            FeedbackStatus status,
            String adminReply
    ) {
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminFeedbackView> listFeedback(FeedbackStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0),
                size <= 0 ? 20 : Math.min(size, 100), Sort.by(Sort.Order.desc("createdAt")));
        Page<Feedback> result = status == null
                ? feedbackRepository.findAllByOrderByCreatedAtDesc(pageable)
                : feedbackRepository.findByStatusOrderByCreatedAtDesc(status, pageable);

        Map<Long, User> users = userRepository.findAllById(
                        result.getContent().stream().map(Feedback::getUserId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, user -> user));

        return PageResponse.of(result, feedback -> {
            User author = users.get(feedback.getUserId());
            return new AdminFeedbackView(feedback.getId(), feedback.getUserId(),
                    author == null ? null : author.getEmail(), feedback.getTitle(),
                    feedback.getContent(), feedback.getCategory(), feedback.getStatus(),
                    feedback.getAdminReply(), feedback.getCreatedAt(), feedback.getUpdatedAt());
        });
    }

    @Transactional
    public AdminFeedbackView updateFeedback(Long id, UpdateFeedbackRequest request) {
        AuthPrincipal admin = currentUserService.requirePrincipal();
        if (request.status() == null) {
            throw ApiException.badRequest("STATUS_REQUIRED", "A target status is required.");
        }
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("FEEDBACK_NOT_FOUND",
                        "No feedback ticket with id " + id + "."));

        FeedbackStatus previous = feedback.getStatus();
        feedback.handle(request.status(), request.adminReply(), admin.getId());
        feedbackRepository.save(feedback);

        auditService.record(admin.getId(), admin.getUsername(), AuditAction.ADMIN_UPDATE_FEEDBACK,
                "FEEDBACK", id, "SUCCESS",
                "status " + previous + " -> " + request.status());

        User author = userRepository.findById(feedback.getUserId()).orElse(null);
        return new AdminFeedbackView(feedback.getId(), feedback.getUserId(),
                author == null ? null : author.getEmail(), feedback.getTitle(),
                feedback.getContent(), feedback.getCategory(), feedback.getStatus(),
                feedback.getAdminReply(), feedback.getCreatedAt(), feedback.getUpdatedAt());
    }

    // -------------------------------------------------------------- dashboard

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long lockedUsers = userRepository.countByStatus(UserStatus.LOCKED);
        long deactivatedUsers = userRepository.countByStatus(UserStatus.DEACTIVATED);

        long newUsersLast7Days = userRepository
                .count(UserSpecifications.createdAfter(Instant.now().minus(7, ChronoUnit.DAYS)));

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalUsers", totalUsers);
        metrics.put("activeUsers", activeUsers);
        metrics.put("lockedUsers", lockedUsers);
        metrics.put("deactivatedUsers", deactivatedUsers);
        metrics.put("newUsersLast7Days", newUsersLast7Days);
        metrics.put("totalTransactions", transactionRepository.count());
        metrics.put("systemCategories", categoryRepository.countBySystemCategoryTrue());
        metrics.put("personalCategories", categoryRepository.countBySystemCategoryFalse());

        Map<String, Object> feedback = new LinkedHashMap<>();
        for (FeedbackStatus value : FeedbackStatus.values()) {
            feedback.put(value.name(), feedbackRepository.countByStatus(value));
        }
        feedback.put("total", feedbackRepository.count());
        metrics.put("feedbackByStatus", feedback);

        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("engine", aiChatService.engineLabel());
        // Counted from the persisted transcript, so it reflects real completed turns.
        ai.put("totalQuestions", aiChatService.countAllQuestions());
        metrics.put("ai", ai);

        metrics.put("runtime", systemMetricsService.snapshot());
        return metrics;
    }

    // ---------------------------------------------------------------- helpers

    private AdminUserView toAdminUserView(User user) {
        String role = roleRepository.findById(user.getRoleId()).map(Role::getName).orElse(Role.USER);
        return new AdminUserView(user.getId(), user.getEmail(), user.getUsername(),
                user.getFullName(), role, user.getStatus(),
                transactionRepository.countByUserId(user.getId()),
                user.getLastLoginAt(), user.getCreatedAt());
    }

    private long transactionCountFor(Long categoryId) {
        return transactionRepository.countByCategoryId(categoryId);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("FIELD_REQUIRED", "Field '" + field + "' is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 64) {
            throw ApiException.badRequest("FIELD_TOO_LONG",
                    "Field '" + field + "' must not exceed 64 characters.");
        }
        return trimmed;
    }
}

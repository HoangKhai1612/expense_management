package com.finai.notification;

import com.finai.common.ApiException;
import com.finai.common.PageResponse;
import com.finai.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String BUDGET_REFERENCE = "BUDGET";

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(NotificationRepository notificationRepository,
                               CurrentUserService currentUserService) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    public record NotificationView(
            Long id,
            String type,
            NotificationLevel level,
            String title,
            String message,
            String referenceType,
            Long referenceId,
            boolean read,
            java.time.Instant createdAt
    ) {
        static NotificationView of(Notification notification) {
            return new NotificationView(notification.getId(), notification.getType(),
                    notification.getLevel(), notification.getTitle(), notification.getMessage(),
                    notification.getReferenceType(), notification.getReferenceId(),
                    notification.isRead(), notification.getCreatedAt());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationView> list(int page, int size) {
        Long userId = currentUserService.requireUserId();
        Page<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(Math.max(page, 0), size <= 0 ? 20 : Math.min(size, 100)));
        return PageResponse.of(result, NotificationView::of);
    }

    @Transactional(readOnly = true)
    public List<NotificationView> unread() {
        Long userId = currentUserService.requireUserId();
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .limit(50)
                .map(NotificationView::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUserIdAndReadFalse(currentUserService.requireUserId());
    }

    @Transactional
    public NotificationView markRead(Long id) {
        Long userId = currentUserService.requireUserId();
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("NOTIFICATION_NOT_FOUND",
                        "No notification with id " + id + " belongs to you."));
        notification.markRead();
        return NotificationView.of(notificationRepository.save(notification));
    }

    @Transactional
    public long markAllRead() {
        Long userId = currentUserService.requireUserId();
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(Notification::markRead);
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    /**
     * Raises the 80%-style warning once per budget. Repeat crossings are suppressed
     * by the partial unique index on (user, type, reference) for unread rows, and
     * this existence check avoids turning a legitimate race into a failed request.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void raiseBudgetWarningIfAbsent(Long userId, Long budgetId, String categoryName,
                                          BigDecimal used, BigDecimal amount, BigDecimal usagePercentage) {
        raiseIfAbsent(userId, NotificationType.BUDGET_WARNING, NotificationLevel.WARNING, budgetId,
                "Budget warning: " + categoryName,
                describe(used, amount, usagePercentage) + " of your " + categoryName
                        + " budget is used. This is a warning, not a limit.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void raiseBudgetExceededIfAbsent(Long userId, Long budgetId, String categoryName,
                                            BigDecimal used, BigDecimal amount, BigDecimal usagePercentage) {
        raiseIfAbsent(userId, NotificationType.BUDGET_EXCEEDED, NotificationLevel.CRITICAL, budgetId,
                "Budget exceeded: " + categoryName,
                "You have used " + format(used) + " of your " + format(amount) + " budget for "
                        + categoryName + " (" + format(usagePercentage) + "%).");
    }

    private void raiseIfAbsent(Long userId, String type, NotificationLevel level, Long referenceId,
                               String title, String message) {
        boolean alreadyRaised = notificationRepository
                .existsByUserIdAndTypeAndReferenceTypeAndReferenceIdAndReadFalse(
                        userId, type, BUDGET_REFERENCE, referenceId);
        if (alreadyRaised) {
            return;
        }
        try {
            notificationRepository.save(
                    new Notification(userId, type, level, title, message, BUDGET_REFERENCE, referenceId));
            log.info("Raised {} for user {} on budget {}", type, userId, referenceId);
        } catch (DataIntegrityViolationException ex) {
            // Another request inserted the same alert concurrently; that is fine.
            log.debug("Duplicate notification suppressed for budget {}", referenceId);
        }
    }

    private String describe(BigDecimal used, BigDecimal amount, BigDecimal usagePercentage) {
        return format(used) + " of " + format(amount) + " (" + format(usagePercentage) + "%)";
    }

    private String format(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}

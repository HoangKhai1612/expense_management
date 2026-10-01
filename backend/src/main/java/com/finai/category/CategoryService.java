package com.finai.category;

import com.finai.common.ApiException;
import com.finai.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Category visibility rules.
 *
 * A normal user sees the system catalogue plus their own personal categories and
 * can only ever mutate the latter. System categories are reserved for the admin
 * endpoints, which is what stops an administrator edit from silently rewriting
 * every user's historical transactions.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public CategoryService(CategoryRepository categoryRepository, CurrentUserService currentUserService) {
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    public record CategoryView(
            Long id,
            String name,
            String code,
            CategoryType type,
            String icon,
            String color,
            boolean systemCategory,
            boolean active
    ) {
        static CategoryView of(Category category) {
            return new CategoryView(category.getId(), category.getName(), category.getCode(),
                    category.getType(), category.getIcon(), category.getColor(),
                    category.isSystemCategory(), category.isActive());
        }
    }

    public record UpsertPersonalCategoryRequest(
            String name,
            CategoryType type,
            String icon,
            String color
    ) {
    }

    /** System catalogue plus the caller's own personal categories. */
    @Transactional(readOnly = true)
    public List<CategoryView> listForCurrentUser(CategoryType filter) {
        Long userId = currentUserService.requireUserId();
        List<Category> all = new ArrayList<>(categoryRepository.findByUserIdIsNullAndActiveTrueOrderByTypeAscNameAsc());
        all.addAll(categoryRepository.findByUserIdAndActiveTrueOrderByTypeAscNameAsc(userId));
        return all.stream()
                .filter(category -> filter == null || category.getType() == filter)
                .map(CategoryView::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryView> listAllSystemCategories() {
        return categoryRepository.findAll().stream()
                .filter(Category::isSystemCategory)
                .map(CategoryView::of)
                .toList();
    }

    @Transactional
    public CategoryView createPersonalCategory(UpsertPersonalCategoryRequest request) {
        Long userId = currentUserService.requireUserId();
        String name = requireName(request.name());
        CategoryType type = request.type() == null
                ? CategoryType.EXPENSE
                : request.type();

        String code = generateCode(userId, name);
        Category category = Category.personal(name, code, type, request.icon(), request.color(), userId);
        return CategoryView.of(categoryRepository.save(category));
    }

    @Transactional
    public CategoryView updatePersonalCategory(Long categoryId, UpsertPersonalCategoryRequest request) {
        Long userId = currentUserService.requireUserId();
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> ApiException.notFound("CATEGORY_NOT_FOUND",
                        "No personal category with id " + categoryId + " belongs to you."));

        category.changeDetails(requireName(request.name()),
                request.type() == null ? category.getType() : request.type(),
                request.icon(), request.color());
        return CategoryView.of(categoryRepository.save(category));
    }

    /**
     * Deactivates a personal category. The row is kept so that historical
     * transactions still resolve their category name; a hard delete would either
     * fail on the foreign key or orphan the user's history.
     */
    @Transactional
    public void deactivatePersonalCategory(Long categoryId) {
        Long userId = currentUserService.requireUserId();
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> ApiException.notFound("CATEGORY_NOT_FOUND",
                        "No personal category with id " + categoryId + " belongs to you."));
        category.changeActive(false);
        categoryRepository.save(category);
    }

    /**
     * Resolves a category the caller is allowed to use, and asserts its type
     * matches the movement being recorded.
     */
    @Transactional(readOnly = true)
    public Category requireUsable(Long categoryId, CategoryType expectedType, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.badRequest("CATEGORY_NOT_FOUND",
                        "Category " + categoryId + " does not exist."));

        boolean owned = category.getUserId() != null && category.getUserId().equals(userId);
        boolean global = category.isSystemCategory();
        if (!owned && !global) {
            // Do not confirm the existence of another user's private category.
            throw ApiException.forbidden("That category belongs to another user.");
        }
        if (!category.isActive()) {
            throw ApiException.badRequest("CATEGORY_INACTIVE",
                    "Category " + categoryId + " is no longer active.");
        }
        if (expectedType != null && category.getType() != expectedType) {
            throw ApiException.badRequest("CATEGORY_TYPE_MISMATCH",
                    "Category " + categoryId + " is an " + category.getType()
                            + " category and cannot be used for an " + expectedType + " entry.");
        }
        return category;
    }

    private String generateCode(Long userId, String name) {
        String base = name.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        if (base.length() > 24) {
            base = base.substring(0, 24);
        }
        if (base.isEmpty()) {
            base = "CATEGORY";
        }
        String candidate = base;
        int suffix = 1;
        while (categoryRepository.findByUserIdAndCodeIgnoreCase(userId, candidate).isPresent()) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw ApiException.badRequest("CATEGORY_NAME_REQUIRED", "Category name is required.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 64) {
            throw ApiException.badRequest("CATEGORY_NAME_TOO_LONG",
                    "Category name must not exceed 64 characters.");
        }
        return trimmed;
    }
}

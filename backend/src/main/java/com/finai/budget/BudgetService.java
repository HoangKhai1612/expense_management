package com.finai.budget;

import com.finai.category.Category;
import com.finai.category.CategoryRepository;
import com.finai.category.CategoryType;
import com.finai.common.ApiException;
import com.finai.config.BudgetAlertProperties;
import com.finai.notification.NotificationService;
import com.finai.security.CurrentUserService;
import com.finai.transaction.TransactionRepository;
import com.finai.transaction.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;
    private final BudgetAlertProperties alertProperties;
    private final CurrentUserService currentUserService;

    public BudgetService(BudgetRepository budgetRepository,
                         TransactionRepository transactionRepository,
                         CategoryRepository categoryRepository,
                         NotificationService notificationService,
                         BudgetAlertProperties alertProperties,
                         CurrentUserService currentUserService) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.notificationService = notificationService;
        this.alertProperties = alertProperties;
        this.currentUserService = currentUserService;
    }

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    public record UpsertBudgetRequest(
            Long categoryId,
            BigDecimal amount,
            BudgetPeriodType periodType,
            LocalDate periodStart
    ) {
    }

    public record BudgetView(
            Long id,
            Long categoryId,
            String categoryName,
            String categoryIcon,
            String categoryColor,
            BigDecimal amount,
            BudgetPeriodType periodType,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal usedAmount,
            BigDecimal remainingAmount,
            BigDecimal usagePercentage,
            String status
    ) {
    }

    /**
     * Status of a budget against its configured thresholds. The thresholds live in
     * configuration; the strings are what the mobile UI switches on.
     */
    public enum BudgetStatus {
        /** Below the warning threshold. */
        SAFE,
        /** At or above the warning threshold but below the exceeded threshold. */
        WARNING,
        /** At or above the exceeded threshold. */
        EXCEEDED
    }

    @Transactional(readOnly = true)
    public List<BudgetView> listActive() {
        Long userId = currentUserService.requireUserId();
        return buildViews(budgetRepository.findByUserIdAndActiveTrueOrderByPeriodStartDesc(userId), userId);
    }

    @Transactional(readOnly = true)
    public List<BudgetView> listAll() {
        Long userId = currentUserService.requireUserId();
        return buildViews(budgetRepository.findByUserIdOrderByPeriodStartDesc(userId), userId);
    }

    @Transactional(readOnly = true)
    public BudgetView getOne(Long id) {
        Long userId = currentUserService.requireUserId();
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("BUDGET_NOT_FOUND",
                        "No budget with id " + id + " belongs to you."));
        return buildViews(List.of(budget), userId).get(0);
    }

    @Transactional
    public BudgetView create(UpsertBudgetRequest request) {
        Long userId = currentUserService.requireUserId();
        Long categoryId = requireCategory(request.categoryId(), userId);
        BigDecimal amount = requireAmount(request.amount());
        BudgetPeriodType periodType = request.periodType() == null
                ? BudgetPeriodType.MONTHLY
                : request.periodType();
        LocalDate start = request.periodStart() == null ? currentMonthStart() : request.periodStart();

        if (budgetRepository.findByUserIdAndCategoryIdAndPeriodStart(userId, categoryId, start).isPresent()) {
            throw ApiException.conflict("BUDGET_ALREADY_EXISTS",
                    "A budget already exists for this category and period.");
        }

        Budget budget = new Budget(userId, categoryId, amount, periodType,
                start, periodEndFor(periodType, start));
        return toView(budgetRepository.save(budget), userId);
    }

    @Transactional
    public BudgetView update(Long id, UpsertBudgetRequest request) {
        Long userId = currentUserService.requireUserId();
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("BUDGET_NOT_FOUND",
                        "No budget with id " + id + " belongs to you."));

        Long categoryId = request.categoryId() == null ? budget.getCategoryId()
                : requireCategory(request.categoryId(), userId);
        BigDecimal amount = request.amount() == null ? budget.getAmount() : requireAmount(request.amount());
        BudgetPeriodType periodType = request.periodType() == null
                ? budget.getPeriodType() : request.periodType();
        LocalDate start = request.periodStart() == null ? budget.getPeriodStart() : request.periodStart();

        if (!categoryId.equals(budget.getCategoryId()) || !start.equals(budget.getPeriodStart())) {
            Optional<Budget> clash = budgetRepository.findByUserIdAndCategoryIdAndPeriodStart(
                    userId, categoryId, start);
            if (clash.isPresent() && !clash.get().getId().equals(budget.getId())) {
                throw ApiException.conflict("BUDGET_ALREADY_EXISTS",
                        "Another budget already covers this category and period.");
            }
        }

        budget.change(amount, periodType, start, periodEndFor(periodType, start));
        return toView(budgetRepository.save(budget), userId);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = currentUserService.requireUserId();
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("BUDGET_NOT_FOUND",
                        "No budget with id " + id + " belongs to you."));
        budgetRepository.delete(budget);
    }

    /**
     * Re-evaluates the caller's live budgets and raises an alert the first time a
     * threshold is crossed. Called after a transaction is written and whenever the
     * budget screen is opened, so the alert does not depend on any background job.
     */
    @Transactional
    public List<BudgetView> evaluateCurrentPeriodAndNotify() {
        Long userId = currentUserService.requireUserId();
        LocalDate today = LocalDate.now();
        List<Budget> live = budgetRepository.findActiveCoveringDate(userId, today);

        List<BudgetView> views = buildViews(live, userId);
        for (BudgetView view : views) {
            BudgetStatus status = statusOf(view.usagePercentage());
            if (status == BudgetStatus.WARNING) {
                notificationService.raiseBudgetWarningIfAbsent(userId, view.id(),
                        view.categoryName(), view.usedAmount(), view.amount(), view.usagePercentage());
            } else if (status == BudgetStatus.EXCEEDED) {
                notificationService.raiseBudgetExceededIfAbsent(userId, view.id(),
                        view.categoryName(), view.usedAmount(), view.amount(), view.usagePercentage());
            }
        }
        return views;
    }

    private List<BudgetView> buildViews(List<Budget> budgets, Long userId) {
        Map<Long, Category> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

        return budgets.stream().map(budget -> {
            Category category = categories.get(budget.getCategoryId());
            BigDecimal used = transactionRepository.sumByUserTypeAndDateAndCategory(
                    userId, TransactionType.EXPENSE, budget.getPeriodStart(), budget.getPeriodEnd(),
                    budget.getCategoryId());
            BigDecimal usedAmount = used == null ? BigDecimal.ZERO : used;
            BigDecimal amount = budget.getAmount();
            BigDecimal remaining = amount.subtract(usedAmount);
            BigDecimal usage = usagePercentage(usedAmount, amount);

            return new BudgetView(
                    budget.getId(),
                    budget.getCategoryId(),
                    category == null ? "Unknown category" : category.getName(),
                    category == null ? null : category.getIcon(),
                    category == null ? null : category.getColor(),
                    amount,
                    budget.getPeriodType(),
                    budget.getPeriodStart(),
                    budget.getPeriodEnd(),
                    usedAmount,
                    remaining,
                    usage,
                    statusOf(usage).name());
        }).toList();
    }

    private BudgetView toView(Budget budget, Long userId) {
        return buildViews(List.of(budget), userId).get(0);
    }

    /**
     * used / amount as a percentage. Rounded to two decimals so the value the UI
     * shows is exactly the value the alert threshold is compared against.
     */
    static BigDecimal usagePercentage(BigDecimal used, BigDecimal amount) {
        if (amount == null || amount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return used.multiply(HUNDRED).divide(amount, 2, RoundingMode.HALF_UP);
    }

    BudgetStatus statusOf(BigDecimal usagePercentage) {
        if (usagePercentage.compareTo(alertProperties.getExceededThreshold()) >= 0) {
            return BudgetStatus.EXCEEDED;
        }
        if (usagePercentage.compareTo(alertProperties.getWarningThreshold()) >= 0) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.SAFE;
    }

    private Long requireCategory(Long categoryId, Long userId) {
        if (categoryId == null) {
            throw ApiException.badRequest("CATEGORY_REQUIRED", "A category is required for a budget.");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.badRequest("CATEGORY_NOT_FOUND",
                        "Category " + categoryId + " does not exist."));
        boolean usable = category.isSystemCategory()
                || (category.getUserId() != null && category.getUserId().equals(userId));
        if (!usable) {
            throw ApiException.forbidden("That category belongs to another user.");
        }
        if (category.getType() != CategoryType.EXPENSE) {
            throw ApiException.badRequest("CATEGORY_TYPE_MISMATCH",
                    "A budget can only be set on an EXPENSE category.");
        }
        return categoryId;
    }

    private BigDecimal requireAmount(BigDecimal amount) {
        if (amount == null) {
            throw ApiException.badRequest("AMOUNT_REQUIRED", "Budget amount is required.");
        }
        if (amount.signum() <= 0) {
            throw ApiException.badRequest("AMOUNT_MUST_BE_POSITIVE",
                    "Budget amount must be greater than zero.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw ApiException.badRequest("AMOUNT_TOO_LARGE",
                    "Budget amount must not exceed " + MAX_AMOUNT + ".");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    static LocalDate currentMonthStart() {
        return YearMonth.now().atDay(1);
    }

    static LocalDate periodEndFor(BudgetPeriodType periodType, LocalDate start) {
        return switch (periodType) {
            case WEEKLY -> start.plusDays(6);
            case MONTHLY -> start.withDayOfMonth(start.lengthOfMonth());
            case YEARLY -> start.withDayOfYear(start.lengthOfYear());
        };
    }
}

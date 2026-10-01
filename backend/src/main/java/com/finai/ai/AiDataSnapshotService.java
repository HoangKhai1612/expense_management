package com.finai.ai;

import com.finai.budget.Budget;
import com.finai.budget.BudgetRepository;
import com.finai.category.Category;
import com.finai.category.CategoryRepository;
import com.finai.statistics.StatisticsService;
import com.finai.transaction.TransactionRepository;
import com.finai.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the verified fact base the assistant is allowed to reason over.
 *
 * Two properties are load-bearing:
 *  1. the userId is a required argument and is never taken from the question, so
 *     the assistant cannot be steered towards another account's numbers;
 *  2. every value is a parameterised aggregate over that user's own rows. There is
 *     no natural-language-to-SQL step at all, which removes the entire class of
 *     injection and accidental-write risks that a generated query would introduce.
 */
@Service
public class AiDataSnapshotService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;

    public AiDataSnapshotService(TransactionRepository transactionRepository,
                                 BudgetRepository budgetRepository,
                                 CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
    }

    public record CategoryTotal(Long categoryId, String name, BigDecimal amount, long count) {
    }

    public record BudgetFact(Long id, String categoryName, BigDecimal limit, BigDecimal used,
                             BigDecimal usagePercentage) {
    }

    /**
     * Facts for one user. {@code hasAnyData} is the switch the analyst uses to decide
     * between answering and explicitly declining to answer.
     */
    public record Snapshot(
            Long userId,
            YearMonth currentMonth,
            YearMonth previousMonth,
            boolean hasAnyData,
            long transactionCount,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal balance,
            BigDecimal previousIncome,
            BigDecimal previousExpense,
            List<CategoryTotal> topSpending,
            List<CategoryTotal> topIncome,
            Map<String, BigDecimal> spendingTrend,
            List<BudgetFact> budgets
    ) {
        public BigDecimal allTimeExpense() {
            return expense;
        }
    }

    @Transactional(readOnly = true)
    public Snapshot load(Long userId) {
        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);

        Map<Long, Category> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Function.identity(), (a, b) -> a));

        BigDecimal income = sum(userId, TransactionType.INCOME, current);
        BigDecimal expense = sum(userId, TransactionType.EXPENSE, current);
        BigDecimal previousIncome = sum(userId, TransactionType.INCOME, previous);
        BigDecimal previousExpense = sum(userId, TransactionType.EXPENSE, previous);
        long transactionCount = transactionRepository.countByUserId(userId);

        List<CategoryTotal> topSpending = topCategories(userId, TransactionType.EXPENSE, current, categories);
        List<CategoryTotal> topIncome = topCategories(userId, TransactionType.INCOME, current, categories);

        Map<String, BigDecimal> trend = new LinkedHashMap<>();
        Map<String, BigDecimal> previousByCategory = totalsByCategoryName(
                userId, TransactionType.EXPENSE, previous, categories);
        for (CategoryTotal entry : topSpending) {
            BigDecimal before = previousByCategory.getOrDefault(entry.name(), BigDecimal.ZERO);
            trend.put(entry.name(), entry.amount().subtract(before));
        }

        List<BudgetFact> budgets = budgetFacts(userId, current, categories);

        return new Snapshot(
                userId, current, previous,
                transactionCount > 0,
                transactionCount,
                income, expense, income.subtract(expense),
                previousIncome, previousExpense,
                topSpending, topIncome, trend, budgets);
    }

    private List<BudgetFact> budgetFacts(Long userId, YearMonth month, Map<Long, Category> categories) {
        List<Budget> live = budgetRepository.findActiveCoveringDate(userId, month.atDay(1));
        return live.stream().map(budget -> {
            BigDecimal used = StatisticsService.zeroIfNull(
                    transactionRepository.sumByUserTypeAndDateAndCategory(
                            userId, TransactionType.EXPENSE,
                            budget.getPeriodStart(), budget.getPeriodEnd(), budget.getCategoryId()));
            Category category = categories.get(budget.getCategoryId());
            return new BudgetFact(
                    budget.getId(),
                    category == null ? "Unknown category" : category.getName(),
                    budget.getAmount(),
                    used,
                    usagePercentage(used, budget.getAmount()));
        }).toList();
    }

    private List<CategoryTotal> topCategories(Long userId, TransactionType type, YearMonth month,
                                              Map<Long, Category> categories) {
        return transactionRepository
                .sumByCategory(userId, type, month.atDay(1), month.atEndOfMonth())
                .stream()
                .limit(5)
                .map(row -> {
                    Long categoryId = (Long) row[0];
                    Category category = categories.get(categoryId);
                    return new CategoryTotal(
                            categoryId,
                            category == null ? "Unknown category" : category.getName(),
                            (BigDecimal) row[1],
                            ((Number) row[2]).longValue());
                })
                .toList();
    }

    private Map<String, BigDecimal> totalsByCategoryName(Long userId, TransactionType type,
                                                        YearMonth month, Map<Long, Category> categories) {
        return transactionRepository
                .sumByCategory(userId, type, month.atDay(1), month.atEndOfMonth())
                .stream()
                .collect(Collectors.toMap(
                        row -> {
                            Category category = categories.get((Long) row[0]);
                            return category == null ? "Unknown category" : category.getName();
                        },
                        row -> (BigDecimal) row[1],
                        (a, b) -> a));
    }

    private BigDecimal sum(Long userId, TransactionType type, YearMonth month) {
        return StatisticsService.zeroIfNull(transactionRepository.sumByUserAndTypeAndDateBetween(
                userId, type, month.atDay(1), month.atEndOfMonth()));
    }

    private static BigDecimal usagePercentage(BigDecimal used, BigDecimal limit) {
        if (limit == null || limit.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return used.multiply(new BigDecimal("100"))
                .divide(limit, 2, java.math.RoundingMode.HALF_UP);
    }

    /** Exposed so the analyst can phrase "since" statements without recomputing. */
    static LocalDate firstDayOf(Snapshot snapshot) {
        return snapshot.currentMonth().atDay(1);
    }
}

package com.finai.statistics;

import com.finai.category.Category;
import com.finai.category.CategoryRepository;
import com.finai.common.ApiException;
import com.finai.security.CurrentUserService;
import com.finai.transaction.Transaction;
import com.finai.transaction.TransactionRepository;
import com.finai.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Every figure produced here is a query over the caller's own transactions.
 * Nothing is sampled, estimated or seeded: an account with no data gets zeros.
 */
@Service
public class StatisticsService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public StatisticsService(TransactionRepository transactionRepository,
                             CategoryRepository categoryRepository,
                             CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    public record Overview(
            LocalDate from,
            LocalDate to,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal balance,
            long incomeCount,
            long expenseCount
    ) {
    }

    public record CategoryBreakdown(
            Long categoryId,
            String categoryName,
            String categoryCode,
            String categoryIcon,
            String categoryColor,
            BigDecimal total,
            long count,
            BigDecimal percentage
    ) {
    }

    public record MonthlyPoint(
            int year,
            int month,
            String label,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal balance
    ) {
    }

    public record DailyPoint(
            LocalDate date,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal balance
    ) {
    }

    @Transactional(readOnly = true)
    public Overview overview(LocalDate from, LocalDate to) {
        Long userId = currentUserService.requireUserId();
        LocalDate[] window = resolveWindow(from, to);

        BigDecimal income = zeroIfNull(transactionRepository.sumByUserAndTypeAndDateBetween(
                userId, TransactionType.INCOME, window[0], window[1]));
        BigDecimal expense = zeroIfNull(transactionRepository.sumByUserAndTypeAndDateBetween(
                userId, TransactionType.EXPENSE, window[0], window[1]));

        List<Transaction> inWindow = transactionRepository.findByUserAndDateRange(
                userId, window[0], window[1]);
        long incomeCount = inWindow.stream().filter(t -> t.getType() == TransactionType.INCOME).count();
        long expenseCount = inWindow.stream().filter(t -> t.getType() == TransactionType.EXPENSE).count();

        return new Overview(window[0], window[1], income, expense,
                income.subtract(expense), incomeCount, expenseCount);
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdown> byCategory(TransactionType type, LocalDate from, LocalDate to) {
        Long userId = currentUserService.requireUserId();
        if (type == null) {
            type = TransactionType.EXPENSE;
        }
        LocalDate[] window = resolveWindow(from, to);

        List<Object[]> rows = transactionRepository.sumByCategory(userId, type, window[0], window[1]);
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, Category> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));

        BigDecimal total = rows.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryBreakdown> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Long categoryId = (Long) row[0];
            BigDecimal sum = (BigDecimal) row[1];
            long count = ((Number) row[2]).longValue();
            Category category = categories.get(categoryId);

            BigDecimal percentage = total.signum() == 0
                    ? BigDecimal.ZERO
                    : sum.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);

            result.add(new CategoryBreakdown(
                    categoryId,
                    category == null ? "Unknown category" : category.getName(),
                    category == null ? "UNKNOWN" : category.getCode(),
                    category == null ? null : category.getIcon(),
                    category == null ? null : category.getColor(),
                    sum, count, percentage));
        }
        return result;
    }

    /** Twelve points for the requested year, January to December, zero-filled. */
    @Transactional(readOnly = true)
    public List<MonthlyPoint> monthly(int year) {
        Long userId = currentUserService.requireUserId();
        int targetYear = (year >= 2000 && year <= 2100) ? year : LocalDate.now().getYear();

        List<Transaction> yearTransactions = transactionRepository.findByUserAndDateRange(
                userId, LocalDate.of(targetYear, 1, 1), LocalDate.of(targetYear, 12, 31));

        Map<YearMonth, List<Transaction>> grouped = yearTransactions.stream()
                .collect(Collectors.groupingBy(
                        transaction -> YearMonth.from(transaction.getTransactionDate()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<MonthlyPoint> points = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            YearMonth key = YearMonth.of(targetYear, month);
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            for (Transaction transaction : grouped.getOrDefault(key, List.of())) {
                if (transaction.getType() == TransactionType.INCOME) {
                    income = income.add(transaction.getAmount());
                } else {
                    expense = expense.add(transaction.getAmount());
                }
            }
            points.add(new MonthlyPoint(targetYear, month,
                    String.format("%02d", month), income, expense, income.subtract(expense)));
        }
        return points;
    }

    /** One point per day in the window, zero-filled so a chart has no gaps. */
    @Transactional(readOnly = true)
    public List<DailyPoint> daily(LocalDate from, LocalDate to) {
        Long userId = currentUserService.requireUserId();
        LocalDate[] window = resolveWindow(from, to);
        if (window[0].plusDays(366).isBefore(window[1])) {
            throw ApiException.badRequest("DATE_RANGE_TOO_WIDE",
                    "The daily series is limited to 366 days.");
        }

        Map<LocalDate, List<Transaction>> grouped = transactionRepository
                .findByUserAndDateRange(userId, window[0], window[1]).stream()
                .collect(Collectors.groupingBy(Transaction::getTransactionDate));

        List<DailyPoint> points = new ArrayList<>();
        for (LocalDate date = window[0]; !date.isAfter(window[1]); date = date.plusDays(1)) {
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            for (Transaction transaction : grouped.getOrDefault(date, List.of())) {
                if (transaction.getType() == TransactionType.INCOME) {
                    income = income.add(transaction.getAmount());
                } else {
                    expense = expense.add(transaction.getAmount());
                }
            }
            points.add(new DailyPoint(date, income, expense, income.subtract(expense)));
        }
        return points;
    }

    /**
     * Resolves the reporting window. Default is the current calendar month, which is
     * what the home screen shows.
     */
    LocalDate[] resolveWindow(LocalDate from, LocalDate to) {
        LocalDate start = from;
        LocalDate end = to;
        if (start == null && end == null) {
            YearMonth month = YearMonth.now();
            return new LocalDate[]{month.atDay(1), month.atEndOfMonth()};
        }
        if (start == null) {
            start = end.minusDays(29);
        }
        if (end == null) {
            end = LocalDate.now();
        }
        if (start.isAfter(end)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE", "'from' must be on or before 'to'.");
        }
        return new LocalDate[]{start, end};
    }

    public static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

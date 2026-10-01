package com.finai.dashboard;

import com.finai.budget.BudgetService;
import com.finai.notification.NotificationService;
import com.finai.security.CurrentUserService;
import com.finai.statistics.StatisticsService;
import com.finai.transaction.TransactionRepository;
import com.finai.transaction.TransactionService;
import com.finai.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * The home screen is served in one round trip. Every field is derived from the
 * caller's own transactions; with no data the response carries zeros and an empty
 * list, which the client renders as an empty state rather than as fake numbers.
 */
@Service
public class DashboardService {

    private final StatisticsService statisticsService;
    private final BudgetService budgetService;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public DashboardService(StatisticsService statisticsService,
                            BudgetService budgetService,
                            TransactionService transactionService,
                            TransactionRepository transactionRepository,
                            NotificationService notificationService,
                            CurrentUserService currentUserService) {
        this.statisticsService = statisticsService;
        this.budgetService = budgetService;
        this.transactionService = transactionService;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    public record BudgetSummary(
            int totalBudgets,
            int warningBudgets,
            int exceededBudgets,
            BigDecimal totalBudgetAmount,
            BigDecimal totalUsedAmount
    ) {
    }

    public record DashboardView(
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal monthIncome,
            BigDecimal monthExpense,
            BigDecimal monthBalance,
            BigDecimal allTimeIncome,
            BigDecimal allTimeExpense,
            BigDecimal allTimeBalance,
            long totalTransactionCount,
            BudgetSummary budgetSummary,
            List<StatisticsService.CategoryBreakdown> topSpendingCategories,
            List<TransactionService.TransactionView> recentTransactions,
            long unreadNotificationCount
    ) {
    }

    @Transactional(readOnly = true)
    public DashboardView load() {
        Long userId = currentUserService.requireUserId();
        YearMonth month = YearMonth.now();
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        StatisticsService.Overview current = statisticsService.overview(start, end);

        BigDecimal allTimeIncome = StatisticsService.zeroIfNull(
                transactionRepository.totalAmountByUserAndType(userId, TransactionType.INCOME));
        BigDecimal allTimeExpense = StatisticsService.zeroIfNull(
                transactionRepository.totalAmountByUserAndType(userId, TransactionType.EXPENSE));

        List<BudgetService.BudgetView> budgets = budgetService.listActive();

        List<StatisticsService.CategoryBreakdown> topCategories =
                statisticsService.byCategory(TransactionType.EXPENSE, start, end)
                        .stream()
                        .limit(5)
                        .toList();

        return new DashboardView(
                start, end,
                current.totalIncome(), current.totalExpense(), current.balance(),
                allTimeIncome, allTimeExpense, allTimeIncome.subtract(allTimeExpense),
                transactionRepository.countByUserId(userId),
                summarise(budgets),
                topCategories,
                transactionService.recent(),
                notificationService.unreadCount());
    }

    private BudgetSummary summarise(List<BudgetService.BudgetView> budgets) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalUsed = BigDecimal.ZERO;
        int warnings = 0;
        int exceeded = 0;
        for (BudgetService.BudgetView budget : budgets) {
            totalAmount = totalAmount.add(budget.amount());
            totalUsed = totalUsed.add(budget.usedAmount());
            if (BudgetService.BudgetStatus.EXCEEDED.name().equals(budget.status())) {
                exceeded++;
            } else if (BudgetService.BudgetStatus.WARNING.name().equals(budget.status())) {
                warnings++;
            }
        }
        return new BudgetSummary(budgets.size(), warnings, exceeded, totalAmount, totalUsed);
    }
}

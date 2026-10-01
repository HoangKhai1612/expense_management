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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long CATEGORY_ID = 1L;

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private CurrentUserService currentUserService;

    private BudgetService service;

    @BeforeEach
    void setUp() {
        BudgetAlertProperties properties = new BudgetAlertProperties();
        service = new BudgetService(budgetRepository, transactionRepository, categoryRepository,
                notificationService, properties, currentUserService);
        // Lenient: the pure calculation tests never resolve the current user.
        lenient().when(currentUserService.requireUserId()).thenReturn(USER_ID);
    }

    private Category expenseCategory() {
        Category category = Category.system("Food & Drink", "FOOD", CategoryType.EXPENSE, "food", "#FF0000");
        // JPA assigns identities; tests do it explicitly so lookups by id resolve.
        org.springframework.test.util.ReflectionTestUtils.setField(category, "id", CATEGORY_ID);
        return category;
    }

    @Test
    @DisplayName("usage percentage is used divided by limit, to two decimals")
    void computesUsagePercentage() {
        assertThat(BudgetService.usagePercentage(new BigDecimal("3000000"), new BigDecimal("4000000")))
                .isEqualByComparingTo("75.00");
        assertThat(BudgetService.usagePercentage(new BigDecimal("3600000"), new BigDecimal("3000000")))
                .isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("a zero limit does not divide by zero")
    void zeroLimitYieldsZeroUsage() {
        assertThat(BudgetService.usagePercentage(new BigDecimal("100"), BigDecimal.ZERO))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(BudgetService.usagePercentage(new BigDecimal("100"), null))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("status flips at the configured thresholds, inclusively")
    void derivesStatusFromThresholds() {
        assertThat(service.statusOf(new BigDecimal("79.99"))).isEqualTo(BudgetService.BudgetStatus.SAFE);
        assertThat(service.statusOf(new BigDecimal("80"))).isEqualTo(BudgetService.BudgetStatus.WARNING);
        assertThat(service.statusOf(new BigDecimal("99.99"))).isEqualTo(BudgetService.BudgetStatus.WARNING);
        assertThat(service.statusOf(new BigDecimal("100"))).isEqualTo(BudgetService.BudgetStatus.EXCEEDED);
        assertThat(service.statusOf(new BigDecimal("250"))).isEqualTo(BudgetService.BudgetStatus.EXCEEDED);
    }

    @Test
    @DisplayName("monthly period end is the last day of that month")
    void computesMonthlyPeriodEnd() {
        assertThat(BudgetService.periodEndFor(BudgetPeriodType.MONTHLY, LocalDate.of(2026, 2, 5)))
                .isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(BudgetService.periodEndFor(BudgetPeriodType.MONTHLY, LocalDate.of(2024, 2, 5)))
                .isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    @DisplayName("weekly and yearly periods end on the correct boundary")
    void computesOtherPeriodEnds() {
        assertThat(BudgetService.periodEndFor(BudgetPeriodType.WEEKLY, LocalDate.of(2026, 9, 30)))
                .isEqualTo(LocalDate.of(2026, 10, 6));
        assertThat(BudgetService.periodEndFor(BudgetPeriodType.YEARLY, LocalDate.of(2026, 3, 1)))
                .isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("a second budget for the same category and period is a conflict")
    void rejectsDuplicatePeriod() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(expenseCategory()));
        when(budgetRepository.findByUserIdAndCategoryIdAndPeriodStart(eq(USER_ID), eq(CATEGORY_ID), any()))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(Budget.class)));

        assertThatThrownBy(() -> service.create(new BudgetService.UpsertBudgetRequest(
                CATEGORY_ID, new BigDecimal("1000000"), BudgetPeriodType.MONTHLY,
                LocalDate.of(2026, 9, 1))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("BUDGET_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("a budget on an INCOME category is rejected")
    void rejectsIncomeCategory() {
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(Category.system("Salary", "SALARY", CategoryType.INCOME, "s", "#0F0")));

        assertThatThrownBy(() -> service.create(new BudgetService.UpsertBudgetRequest(
                CATEGORY_ID, new BigDecimal("1000000"), BudgetPeriodType.MONTHLY, LocalDate.of(2026, 9, 1))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("CATEGORY_TYPE_MISMATCH"));
    }

    @Test
    @DisplayName("a non-positive budget amount is rejected")
    void rejectsNonPositiveAmount() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(expenseCategory()));

        assertThatThrownBy(() -> service.create(new BudgetService.UpsertBudgetRequest(
                CATEGORY_ID, BigDecimal.ZERO, BudgetPeriodType.MONTHLY, LocalDate.of(2026, 9, 1))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("AMOUNT_MUST_BE_POSITIVE"));
    }

    @Test
    @DisplayName("a user cannot set a budget on another user's own category")
    void rejectsForeignUserCategory() {
        Category foreign = Category.personal("Hobby", "HOBBY", CategoryType.EXPENSE, "h", "#00F", USER_ID + 99);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.create(new BudgetService.UpsertBudgetRequest(
                CATEGORY_ID, new BigDecimal("1000000"), BudgetPeriodType.MONTHLY, LocalDate.of(2026, 9, 1))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("a budget with no spending reports zero used and a full remainder")
    void unusedBudgetIsFullyRemaining() {
        Budget budget = budgetFixture(new BigDecimal("1000000"));
        when(categoryRepository.findAll()).thenReturn(List.of(expenseCategory()));
        when(budgetRepository.findByUserIdAndActiveTrueOrderByPeriodStartDesc(USER_ID))
                .thenReturn(List.of(budget));
        when(transactionRepository.sumByUserTypeAndDateAndCategory(
                eq(USER_ID), eq(TransactionType.EXPENSE), any(), any(), eq(CATEGORY_ID)))
                .thenReturn(null);

        List<BudgetService.BudgetView> views = service.listActive();

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.usedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(view.remainingAmount()).isEqualByComparingTo("1000000");
            assertThat(view.usagePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(view.status()).isEqualTo("SAFE");
            assertThat(view.categoryName()).isEqualTo("Food & Drink");
        });
    }

    @Test
    @DisplayName("crossing the warning threshold raises exactly one warning")
    void raisesWarningOnceCrossed() {
        Budget budget = budgetFixture(new BigDecimal("1000000"));
        when(budgetRepository.findActiveCoveringDate(eq(USER_ID), any())).thenReturn(List.of(budget));
        when(categoryRepository.findAll()).thenReturn(List.of(expenseCategory()));
        when(transactionRepository.sumByUserTypeAndDateAndCategory(
                eq(USER_ID), eq(TransactionType.EXPENSE), any(), any(), eq(CATEGORY_ID)))
                .thenReturn(new BigDecimal("800000"));

        List<BudgetService.BudgetView> views = service.evaluateCurrentPeriodAndNotify();

        assertThat(views).singleElement()
                .extracting(BudgetService.BudgetView::status).isEqualTo("WARNING");
        verify(notificationService).raiseBudgetWarningIfAbsent(eq(USER_ID), eq(10L),
                eq("Food & Drink"), eq(new BigDecimal("800000")), eq(new BigDecimal("1000000")),
                eq(new BigDecimal("80.00")));
        verify(notificationService, never()).raiseBudgetExceededIfAbsent(
                anyLong(), anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("crossing the exceeded threshold raises the critical alert instead")
    void raisesCriticalWhenExceeded() {
        Budget budget = budgetFixture(new BigDecimal("1000000"));
        when(budgetRepository.findActiveCoveringDate(eq(USER_ID), any())).thenReturn(List.of(budget));
        when(categoryRepository.findAll()).thenReturn(List.of(expenseCategory()));
        when(transactionRepository.sumByUserTypeAndDateAndCategory(
                eq(USER_ID), eq(TransactionType.EXPENSE), any(), any(), eq(CATEGORY_ID)))
                .thenReturn(new BigDecimal("1200000"));

        List<BudgetService.BudgetView> views = service.evaluateCurrentPeriodAndNotify();

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.usagePercentage()).isEqualByComparingTo("120.00");
            assertThat(view.remainingAmount()).isEqualByComparingTo("-200000");
            assertThat(view.status()).isEqualTo("EXCEEDED");
        });
        verify(notificationService).raiseBudgetExceededIfAbsent(eq(USER_ID), eq(10L),
                eq("Food & Drink"), eq(new BigDecimal("1200000")), eq(new BigDecimal("1000000")),
                eq(new BigDecimal("120.00")));
    }

    @Test
    @DisplayName("a budget under the warning threshold raises no alert")
    void silentWhenSafe() {
        Budget budget = budgetFixture(new BigDecimal("1000000"));
        when(budgetRepository.findActiveCoveringDate(eq(USER_ID), any())).thenReturn(List.of(budget));
        when(categoryRepository.findAll()).thenReturn(List.of(expenseCategory()));
        when(transactionRepository.sumByUserTypeAndDateAndCategory(
                eq(USER_ID), eq(TransactionType.EXPENSE), any(), any(), eq(CATEGORY_ID)))
                .thenReturn(new BigDecimal("100000"));

        List<BudgetService.BudgetView> views = service.evaluateCurrentPeriodAndNotify();

        assertThat(views).singleElement()
                .extracting(BudgetService.BudgetView::status).isEqualTo("SAFE");
        verify(notificationService, never()).raiseBudgetWarningIfAbsent(
                anyLong(), anyLong(), any(), any(), any(), any());
        verify(notificationService, never()).raiseBudgetExceededIfAbsent(
                anyLong(), anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("reading a budget owned by another user is a 404, not a 403")
    void hidesForeignBudgets() {
        when(budgetRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOne(99L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("BUDGET_NOT_FOUND"));
    }

    private Budget budgetFixture(BigDecimal amount) {
        LocalDate start = LocalDate.of(2026, 9, 1);
        Budget budget = new Budget(USER_ID, CATEGORY_ID, amount, BudgetPeriodType.MONTHLY,
                start, LocalDate.of(2026, 9, 30));
        // The repositories normally assign the identity; tests set it explicitly.
        org.springframework.test.util.ReflectionTestUtils.setField(budget, "id", 10L);
        return budget;
    }

    @Test
    @DisplayName("the period end stored on a new budget matches its period type")
    void storesCorrectPeriodEndOnCreate() {
        ArgumentCaptor<Budget> saved = ArgumentCaptor.forClass(Budget.class);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(expenseCategory()));
        when(budgetRepository.findByUserIdAndCategoryIdAndPeriodStart(eq(USER_ID), eq(CATEGORY_ID), any()))
                .thenReturn(Optional.empty());
        when(categoryRepository.findAll()).thenReturn(List.of(expenseCategory()));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget budget = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(budget, "id", 11L);
            return budget;
        });
        when(transactionRepository.sumByUserTypeAndDateAndCategory(
                eq(USER_ID), eq(TransactionType.EXPENSE), any(), any(), eq(CATEGORY_ID)))
                .thenReturn(BigDecimal.ZERO);

        BudgetService.BudgetView view = service.create(new BudgetService.UpsertBudgetRequest(
                CATEGORY_ID, new BigDecimal("2500000"), BudgetPeriodType.MONTHLY, LocalDate.of(2026, 2, 1)));

        verify(budgetRepository).save(saved.capture());
        assertThat(saved.getValue().getPeriodEnd()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(view.remainingAmount()).isEqualByComparingTo("2500000");
        assertThat(view.status()).isEqualTo("SAFE");
    }
}

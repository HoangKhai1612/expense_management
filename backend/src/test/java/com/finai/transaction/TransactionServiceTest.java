package com.finai.transaction;

import com.finai.category.Category;
import com.finai.category.CategoryRepository;
import com.finai.category.CategoryService;
import com.finai.common.ApiException;
import com.finai.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long CATEGORY_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private CurrentUserService currentUserService;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(transactionRepository, categoryRepository, categoryService,
                currentUserService);
        when(currentUserService.requireUserId()).thenReturn(USER_ID);

        Category category = Category.system("Food & Drink", "FOOD",
                com.finai.category.CategoryType.EXPENSE, "food", "#FF0000");
        ReflectionTestUtils.setField(category, "id", CATEGORY_ID);
        when(categoryRepository.findAll()).thenReturn(List.of(category));
    }

    private Transaction transactionFixture(Long id) {
        Transaction transaction = new Transaction(USER_ID, CATEGORY_ID, TransactionType.EXPENSE,
                new BigDecimal("2400000.00"), "Groceries", LocalDate.of(2026, 9, 15));
        ReflectionTestUtils.setField(transaction, "id", id);
        return transaction;
    }

    private TransactionService.UpsertRequest validRequest() {
        return new TransactionService.UpsertRequest(CATEGORY_ID, TransactionType.EXPENSE,
                new BigDecimal("2400000.00"), "Groceries", LocalDate.of(2026, 9, 15));
    }

    @Test
    @DisplayName("the expected category type follows the transaction type")
    void mapsTransactionTypeToCategoryType() {
        assertThat(TransactionService.expectedCategoryType(TransactionType.INCOME))
                .isEqualTo(com.finai.category.CategoryType.INCOME);
        assertThat(TransactionService.expectedCategoryType(TransactionType.EXPENSE))
                .isEqualTo(com.finai.category.CategoryType.EXPENSE);
    }

    @Test
    @DisplayName("a created transaction is stored with the caller's user id")
    void createStampsTheCurrentUser() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        service.create(validRequest());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("a zero or negative amount is rejected before anything is written")
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.create(new TransactionService.UpsertRequest(
                CATEGORY_ID, TransactionType.EXPENSE, BigDecimal.ZERO, null, LocalDate.of(2026, 9, 15))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("AMOUNT_MUST_BE_POSITIVE"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("a missing amount is rejected with a specific code")
    void rejectsMissingAmount() {
        assertThatThrownBy(() -> service.create(new TransactionService.UpsertRequest(
                CATEGORY_ID, TransactionType.EXPENSE, null, null, LocalDate.of(2026, 9, 15))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("AMOUNT_REQUIRED"));
    }

    @Test
    @DisplayName("an amount beyond the NUMERIC(15,2) ceiling is rejected")
    void rejectsOversizedAmount() {
        assertThatThrownBy(() -> service.create(new TransactionService.UpsertRequest(
                CATEGORY_ID, TransactionType.EXPENSE, new BigDecimal("10000000000.00"), null,
                LocalDate.of(2026, 9, 15))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("AMOUNT_TOO_LARGE"));
    }

    @Test
    @DisplayName("an amount with more than two decimals is rejected rather than silently rounded")
    void rejectsOverPreciseAmount() {
        assertThatThrownBy(() -> service.create(new TransactionService.UpsertRequest(
                CATEGORY_ID, TransactionType.EXPENSE, new BigDecimal("100.123"), null,
                LocalDate.of(2026, 9, 15))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("AMOUNT_TOO_PRECISE"));
    }

    @Test
    @DisplayName("a transaction dated in the future is rejected")
    void rejectsFutureDate() {
        assertThatThrownBy(() -> service.create(new TransactionService.UpsertRequest(
                CATEGORY_ID, TransactionType.EXPENSE, new BigDecimal("1000.00"), null,
                LocalDate.now().plusDays(30))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("DATE_IN_FUTURE"));
    }

    @Test
    @DisplayName("a transaction older than ten years is rejected")
    void rejectsAncientDate() {
        assertThatThrownBy(() -> service.create(new TransactionService.UpsertRequest(
                CATEGORY_ID, TransactionType.EXPENSE, new BigDecimal("1000.00"), null,
                LocalDate.now().minusYears(11))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("DATE_TOO_OLD"));
    }

    @Test
    @DisplayName("an over-long note is rejected instead of being truncated")
    void rejectsOverlongNote() {
        assertThatThrownBy(() -> service.create(new TransactionService.UpsertRequest(
                CATEGORY_ID, TransactionType.EXPENSE, new BigDecimal("1000.00"),
                "x".repeat(501), LocalDate.of(2026, 9, 15))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("NOTE_TOO_LONG"));
    }

    @Test
    @DisplayName("a blank note is stored as null")
    void blankNoteBecomesNull() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        service.create(new TransactionService.UpsertRequest(CATEGORY_ID, TransactionType.EXPENSE,
                new BigDecimal("1000.00"), "   ", LocalDate.of(2026, 9, 15)));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getNote()).isNull();
    }

    /**
     * The service's own contract is that it asks the category guard for the type
     * matching the transaction type. The guard's rejection behaviour is covered by
     * CategoryServiceTest; here we pin what is delegated.
     */
    @Test
    @DisplayName("the write path asks the category guard for the type matching the transaction")
    void delegatesCategoryTypeCheckToCategoryGuard() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        service.create(new TransactionService.UpsertRequest(CATEGORY_ID, TransactionType.INCOME,
                new BigDecimal("1000.00"), null, LocalDate.of(2026, 9, 15)));

        verify(categoryService).requireUsable(CATEGORY_ID,
                com.finai.category.CategoryType.INCOME, USER_ID);
    }

    @Test
    @DisplayName("a category the guard rejects stops the write")
    void categoryGuardCanBlockTheWrite() {
        org.mockito.Mockito.doThrow(ApiException.badRequest("CATEGORY_TYPE_MISMATCH", "nope"))
                .when(categoryService).requireUsable(any(), any(), any());

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("CATEGORY_TYPE_MISMATCH"));
        verify(transactionRepository, never()).save(any());
    }

    // -------------------------------------------------------- cross-user isolation

    @Test
    @DisplayName("reading another user's transaction reports not found, not forbidden")
    void readingForeignTransactionIsNotFound() {
        when(transactionRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOne(99L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("TRANSACTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("updating another user's transaction is refused and nothing is saved")
    void cannotUpdateForeignTransaction() {
        when(transactionRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, validRequest()))
                .isInstanceOf(ApiException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleting another user's transaction is refused and nothing is deleted")
    void cannotDeleteForeignTransaction() {
        when(transactionRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ApiException.class);
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("the recent list is scoped to the caller's own rows")
    void recentIsScopedToCaller() {
        when(transactionRepository.findTop5ByUserIdOrderByTransactionDateDescCreatedAtDesc(USER_ID))
                .thenReturn(List.of(transactionFixture(1L)));

        assertThat(service.recent()).singleElement()
                .extracting(TransactionService.TransactionView::id).isEqualTo(1L);
        verify(transactionRepository)
                .findTop5ByUserIdOrderByTransactionDateDescCreatedAtDesc(USER_ID);
    }

    @Test
    @DisplayName("a transaction whose category vanished still renders for the user")
    void toleratesMissingCategory() {
        Transaction orphaned = new Transaction(USER_ID, 999L, TransactionType.EXPENSE,
                new BigDecimal("1000.00"), null, LocalDate.of(2026, 9, 15));
        ReflectionTestUtils.setField(orphaned, "id", 3L);
        when(transactionRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(orphaned));

        TransactionService.TransactionView view = service.getOne(3L);

        assertThat(view.category().name()).isEqualTo("Unknown category");
        assertThat(view.amount()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("an inverted search range is rejected")
    void rejectsInvertedDateRange() {
        assertThatThrownBy(() -> service.search(null, null,
                LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1), 0, 20))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("INVALID_DATE_RANGE"));
    }

    @Test
    @DisplayName("an update that matches a real row changes its fields")
    void updateAppliesChanges() {
        Transaction existing = transactionFixture(1L);
        when(transactionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        TransactionService.TransactionView view = service.update(1L,
                new TransactionService.UpsertRequest(CATEGORY_ID, TransactionType.EXPENSE,
                        new BigDecimal("2500000.00"), "Groceries corrected", LocalDate.of(2026, 9, 16)));

        assertThat(view.amount()).isEqualByComparingTo("2500000.00");
        assertThat(view.note()).isEqualTo("Groceries corrected");
        assertThat(view.transactionDate()).isEqualTo(LocalDate.of(2026, 9, 16));
    }
}

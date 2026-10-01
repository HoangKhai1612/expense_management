package com.finai.transaction;

import com.finai.category.Category;
import com.finai.category.CategoryRepository;
import com.finai.category.CategoryService;
import com.finai.common.ApiException;
import com.finai.common.PageResponse;
import com.finai.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    /** Hard ceiling so a mistyped amount cannot overflow the NUMERIC(15,2) column. */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              CategoryService categoryService,
                              CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
    }

    public record UpsertRequest(
            Long categoryId,
            TransactionType type,
            BigDecimal amount,
            String note,
            LocalDate transactionDate
    ) {
    }

    public record TransactionView(
            Long id,
            TransactionType type,
            BigDecimal amount,
            String note,
            LocalDate transactionDate,
            CategorySummary category,
            java.time.Instant createdAt
    ) {
    }

    public record CategorySummary(Long id, String name, String code, String icon, String color) {
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionView> search(TransactionType type, Long categoryId,
                                                LocalDate from, LocalDate to,
                                                int page, int size) {
        Long userId = currentUserService.requireUserId();
        validateDateRange(from, to);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                clampSize(size),
                Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("createdAt")));
        Page<Transaction> result = transactionRepository.search(userId, type, categoryId, from, to, pageable);
        Map<Long, CategorySummary> categories = loadCategories();
        return PageResponse.of(result, transaction -> toView(transaction, categories));
    }

    @Transactional(readOnly = true)
    public TransactionView getOne(Long id) {
        Long userId = currentUserService.requireUserId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("TRANSACTION_NOT_FOUND",
                        "No transaction with id " + id + " belongs to you."));
        return toView(transaction, loadCategories());
    }

    @Transactional(readOnly = true)
    public List<TransactionView> recent() {
        Long userId = currentUserService.requireUserId();
        Map<Long, CategorySummary> categories = loadCategories();
        return transactionRepository
                .findTop5ByUserIdOrderByTransactionDateDescCreatedAtDesc(userId)
                .stream()
                .map(transaction -> toView(transaction, categories))
                .toList();
    }

    @Transactional
    public TransactionView create(UpsertRequest request) {
        Long userId = currentUserService.requireUserId();
        TransactionType type = requireType(request.type());
        BigDecimal amount = requireAmount(request.amount());
        LocalDate date = requireDate(request.transactionDate());

        categoryService.requireUsable(request.categoryId(), expectedCategoryType(type), userId);

        Transaction transaction = new Transaction(userId, request.categoryId(), type, amount,
                normaliseNote(request.note()), date);
        transaction = transactionRepository.save(transaction);
        return toView(transaction, loadCategories());
    }

    @Transactional
    public TransactionView update(Long id, UpsertRequest request) {
        Long userId = currentUserService.requireUserId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("TRANSACTION_NOT_FOUND",
                        "No transaction with id " + id + " belongs to you."));

        TransactionType type = requireType(request.type());
        BigDecimal amount = requireAmount(request.amount());
        LocalDate date = requireDate(request.transactionDate());
        categoryService.requireUsable(request.categoryId(), expectedCategoryType(type), userId);

        transaction.change(request.categoryId(), type, amount, normaliseNote(request.note()), date);
        return toView(transactionRepository.save(transaction), loadCategories());
    }

    @Transactional
    public void delete(Long id) {
        Long userId = currentUserService.requireUserId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("TRANSACTION_NOT_FOUND",
                        "No transaction with id " + id + " belongs to you."));
        transactionRepository.delete(transaction);
    }

    private TransactionView toView(Transaction transaction, Map<Long, CategorySummary> categories) {
        CategorySummary category = categories.get(transaction.getCategoryId());
        if (category == null) {
            // The category was hard-deleted by an administrator; surface it rather
            // than crash, so the user's history stays readable.
            category = new CategorySummary(transaction.getCategoryId(), "Unknown category",
                    "UNKNOWN", null, "#9E9E9E");
        }
        return new TransactionView(transaction.getId(), transaction.getType(),
                transaction.getAmount(), transaction.getNote(),
                transaction.getTransactionDate(), category, transaction.getCreatedAt());
    }

    private Map<Long, CategorySummary> loadCategories() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, category -> new CategorySummary(
                        category.getId(), category.getName(), category.getCode(),
                        category.getIcon(), category.getColor()),
                        (first, second) -> first));
    }

    static com.finai.category.CategoryType expectedCategoryType(TransactionType type) {
        return type == TransactionType.INCOME
                ? com.finai.category.CategoryType.INCOME
                : com.finai.category.CategoryType.EXPENSE;
    }

    private TransactionType requireType(TransactionType type) {
        if (type == null) {
            throw ApiException.badRequest("TRANSACTION_TYPE_REQUIRED",
                    "Type must be either INCOME or EXPENSE.");
        }
        return type;
    }

    private BigDecimal requireAmount(BigDecimal amount) {
        if (amount == null) {
            throw ApiException.badRequest("AMOUNT_REQUIRED", "Amount is required.");
        }
        if (amount.signum() <= 0) {
            throw ApiException.badRequest("AMOUNT_MUST_BE_POSITIVE",
                    "Amount must be greater than zero. Use the INCOME/EXPENSE type to express direction.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw ApiException.badRequest("AMOUNT_TOO_LARGE",
                    "Amount must not exceed " + MAX_AMOUNT + ".");
        }
        if (amount.scale() > 2) {
            throw ApiException.badRequest("AMOUNT_TOO_PRECISE",
                    "Amount supports at most 2 decimal places.");
        }
        return amount.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }

    private LocalDate requireDate(LocalDate date) {
        if (date == null) {
            throw ApiException.badRequest("DATE_REQUIRED", "Transaction date is required.");
        }
        if (date.isAfter(LocalDate.now().plusDays(1))) {
            throw ApiException.badRequest("DATE_IN_FUTURE",
                    "Transaction date cannot be in the future.");
        }
        if (date.isBefore(LocalDate.now().minusYears(10))) {
            throw ApiException.badRequest("DATE_TOO_OLD",
                    "Transaction date cannot be more than 10 years in the past.");
        }
        return date;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE",
                    "'from' must be on or before 'to'.");
        }
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 200);
    }

    private String normaliseNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String trimmed = note.trim();
        if (trimmed.length() > 500) {
            throw ApiException.badRequest("NOTE_TOO_LONG", "Note must not exceed 500 characters.");
        }
        return trimmed;
    }
}

package com.finai.transaction;

import com.finai.category.CategoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A single money movement owned by exactly one user.
 *
 * The amount is always stored positive; direction lives in {@code type} so that
 * "balance = income - expense" never depends on a sign convention that could be
 * broken by a bad client.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Transaction() {
        // for JPA
    }

    public Transaction(Long userId, Long categoryId, TransactionType type, BigDecimal amount,
                       String note, LocalDate transactionDate) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.transactionDate = transactionDate;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public void change(Long categoryId, TransactionType type, BigDecimal amount, String note,
                       LocalDate transactionDate) {
        this.categoryId = categoryId;
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.transactionDate = transactionDate;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public TransactionType getType() {
        return type;
    }

    /** Value the category is expected to have, used to keep entity and catalogue consistent. */
    public CategoryType expectedCategoryType() {
        return type == TransactionType.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

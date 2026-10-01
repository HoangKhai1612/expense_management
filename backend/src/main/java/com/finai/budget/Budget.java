package com.finai.budget;

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

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 16)
    private BudgetPeriodType periodType = BudgetPeriodType.MONTHLY;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Budget() {
        // for JPA
    }

    public Budget(Long userId, Long categoryId, BigDecimal amount, BudgetPeriodType periodType,
                  LocalDate periodStart, LocalDate periodEnd) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public void change(BigDecimal amount, BudgetPeriodType periodType,
                       LocalDate periodStart, LocalDate periodEnd) {
        this.amount = amount;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(periodStart) && !date.isAfter(periodEnd);
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

    public BigDecimal getAmount() {
        return amount;
    }

    public BudgetPeriodType getPeriodType() {
        return periodType;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

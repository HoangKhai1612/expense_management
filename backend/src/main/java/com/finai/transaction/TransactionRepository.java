package com.finai.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Every read is scoped by user_id. A transaction owned by somebody else is
     * simply not in the result set, so ownership cannot be bypassed by guessing ids.
     */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.userId = :userId
              AND (:type IS NULL OR t.type = :type)
              AND (:categoryId IS NULL OR t.categoryId = :categoryId)
              AND (:from IS NULL OR t.transactionDate >= :from)
              AND (:to IS NULL OR t.transactionDate <= :to)
            """)
    Page<Transaction> search(@Param("userId") Long userId,
                             @Param("type") TransactionType type,
                             @Param("categoryId") Long categoryId,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to,
                             Pageable pageable);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    List<Transaction> findTop5ByUserIdOrderByTransactionDateDescCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    long countByCategoryId(Long categoryId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.userId = :userId AND t.type = :type
              AND t.transactionDate BETWEEN :from AND :to
            """)
    BigDecimal sumByUserAndTypeAndDateBetween(@Param("userId") Long userId,
                                             @Param("type") TransactionType type,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);

    @Query("""
            SELECT t.categoryId, COALESCE(SUM(t.amount), 0), COUNT(t) FROM Transaction t
            WHERE t.userId = :userId AND t.type = :type
              AND t.transactionDate BETWEEN :from AND :to
            GROUP BY t.categoryId
            ORDER BY SUM(t.amount) DESC
            """)
    List<Object[]> sumByCategory(@Param("userId") Long userId,
                                 @Param("type") TransactionType type,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.userId = :userId AND t.type = :type
              AND t.transactionDate BETWEEN :from AND :to AND t.categoryId = :categoryId
            """)
    BigDecimal sumByUserTypeAndDateAndCategory(@Param("userId") Long userId,
                                               @Param("type") TransactionType type,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to,
                                               @Param("categoryId") Long categoryId);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.userId = :userId AND t.transactionDate BETWEEN :from AND :to
            ORDER BY t.transactionDate ASC
            """)
    List<Transaction> findByUserAndDateRange(@Param("userId") Long userId,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.userId = :userId
            """)
    BigDecimal totalAmountByUser(@Param("userId") Long userId);

    /** All-time total for one direction, with no date bound. */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.userId = :userId AND t.type = :type
            """)
    BigDecimal totalAmountByUserAndType(@Param("userId") Long userId,
                                        @Param("type") TransactionType type);
}

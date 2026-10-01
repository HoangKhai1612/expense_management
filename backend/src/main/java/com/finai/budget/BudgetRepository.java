package com.finai.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdAndActiveTrueOrderByPeriodStartDesc(Long userId);

    List<Budget> findByUserIdOrderByPeriodStartDesc(Long userId);

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    Optional<Budget> findByUserIdAndCategoryIdAndPeriodStart(Long userId, Long categoryId,
                                                            LocalDate periodStart);

    @Query("""
            SELECT b FROM Budget b
            WHERE b.userId = :userId AND b.active = TRUE AND b.periodEnd >= :date
            """)
    List<Budget> findActiveCoveringDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}

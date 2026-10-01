package com.finai.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdIsNullAndActiveTrueOrderByTypeAscNameAsc();

    List<Category> findByUserIdAndActiveTrueOrderByTypeAscNameAsc(Long userId);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    Optional<Category> findByUserIdAndCodeIgnoreCase(Long userId, String code);

    boolean existsByCodeIgnoreCaseAndSystemCategoryTrue(String code);

    long countBySystemCategoryTrue();

    long countBySystemCategoryFalse();
}

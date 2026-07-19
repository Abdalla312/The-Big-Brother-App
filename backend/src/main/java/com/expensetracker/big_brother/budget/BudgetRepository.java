package com.expensetracker.big_brother.budget;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    Page<Budget> findAllByUserIdAndMonth(UUID userId, String month, Pageable pageable);

    boolean existsByUserIdAndCategoryIdAndMonth(UUID userId, UUID categoryId, String month);
}

package com.expensetracker.big_brother.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findAllByUserIdAndMonth(UUID userId, String month);

    boolean existsByUserIdAndCategoryIdAndMonth(UUID userId, UUID categoryId, String month);
}

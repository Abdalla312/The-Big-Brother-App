package com.expensetracker.big_brother.budget;

import com.expensetracker.big_brother.common.BypassSoftDelete;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    @Override
    @Query("SELECT b FROM Budget b WHERE b.id = :id ")
    @NotNull
    Optional<Budget> findById(@NotNull UUID id);

    Page<Budget> findAllByUserIdAndMonth(UUID userId, String month, Pageable pageable);

    List<Budget> findAllByUserIdAndMonth(UUID userId, String month);

    boolean existsByUserIdAndCategoryIdAndMonth(UUID userId, UUID categoryId, String month);

    @BypassSoftDelete
    @Query("SELECT b FROM Budget b WHERE b.deletedAt IS NOT NULL AND b.user.id = :userId ")
    Page<Budget> findDeletedBudgets(UUID userId, Pageable pageable);

    @BypassSoftDelete
    @Query("SELECT b FROM Budget b WHERE b.id = :id AND b.deletedAt IS NOT NULL ")
    Optional<Budget> findDeletedById(@Param("id") UUID id);
}

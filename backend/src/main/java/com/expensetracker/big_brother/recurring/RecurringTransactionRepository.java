package com.expensetracker.big_brother.recurring;

import com.expensetracker.big_brother.common.BypassSoftDelete;
import com.expensetracker.big_brother.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, UUID> {

    @Override
    @Query("SELECT r FROM RecurringTransaction r WHERE r.id = :id ")
    @NotNull
    Optional<RecurringTransaction> findById(@NotNull UUID id);

    Page<RecurringTransaction> findAllByUserId(UUID userId, Pageable pageable);

    @Query("SELECT r " +
            "FROM RecurringTransaction r " +
            "WHere r.isActive = true AND r.nextExecutionDate <= :date")
    Page<RecurringTransaction> findDueBatch(@Param("date") LocalDate date, Pageable batch);

    boolean existsByCategoryId(UUID categoryId);

    @BypassSoftDelete
    @Query("SELECT r FROM RecurringTransaction r WHERE r.deletedAt IS NOT NULL AND r.user.id = :userId ")
    Page<RecurringTransaction> findDeletedRecurringTransactions(UUID userId, Pageable pageable);

    @BypassSoftDelete
    @Query("SELECT r FROM RecurringTransaction r WHERE r.deletedAt IS NOT NULL AND r.id = :id ")
    Optional<RecurringTransaction> findDeletedById(UUID id);
}
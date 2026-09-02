package com.expensetracker.big_brother.recurring;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, UUID> {
    Page<RecurringTransaction> findAllByUserId(UUID userId, Pageable pageable);

    @Query("SELECT r " +
            "FROM RecurringTransaction r " +
            "WHere r.isActive = true AND r.nextExecutionDate <= :date")
    Page<RecurringTransaction> findDueBatch(@Param("date") LocalDate date, Pageable batch);

    boolean existsByCategoryId(UUID categoryId);
}
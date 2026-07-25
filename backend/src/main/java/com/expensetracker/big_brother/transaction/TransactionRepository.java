package com.expensetracker.big_brother.transaction;

import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.report.dto.projection.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    boolean existsByCategoryId(UUID categoryId);

    @Query("SELECT coalesce(sum(t.amount),0 ) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.category.id = :categoryId  " +
            "AND t.type = 'EXPENSE' " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumExpensesByUserAndCategoryAndDateRange(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t.type, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY t.type")
    List<TypeSum> sumByType(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t.category.id, t.category.name, t.category.color, " +
            "t.category.icon, t.type, COALESCE(SUM(t.amount), 0) " +
            "FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND t.type = :type " +
            "GROUP BY t.category.id, t.category.name, t.category.color, " +
            "t.category.icon, t.type " +
            "ORDER BY SUM(t.amount) DESC ")
    List<CategorySum> sumByCategory(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("type") TransactionType type);

    @Query("SELECT CAST(FUNCTION('TO_CHAR', t.transactionDate, 'YYYY-MM') AS string), " +
            "t.type, COALESCE(SUM(t.amount), 0 ) " +
            "FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('TO_CHAR', t.transactionDate, 'YYYY-MM'), t.type " +
            "ORDER BY FUNCTION('TO_CHAR', t.transactionDate, 'YYYY-MM') ")
    List<MonthSum> sumByMonth(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT t.category.id, t.category.name, t.category.color," +
            "COALESCE(sum(t.amount), 0) " +
            "From Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.type = 'EXPENSE' " +
            "AND FUNCTION('TO_CHAR', t.transactionDate, 'YYYY-MM') = :month " +
            "GROUP BY t.category.id, t.category.name,t.category.color")
    List<CategoryExpenses> expensesByCategory(
            @Param("userId") UUID userId,
            @Param("month") String month);

    @Query("SELECT t.paymentMethod, t.category.id, t.category.name, t.category.color, " +
            "COALESCE(sum(t.amount), 0) " +
            "FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.type = :type " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY t.paymentMethod, t.category.id, t.category.name, t.category.color ")
    List<PaymentSum> sumByPaymentMethod(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("type") TransactionType type
    );

    long countByUserIdAndTransactionDateBetween(UUID userId, LocalDate from, LocalDate to);
}

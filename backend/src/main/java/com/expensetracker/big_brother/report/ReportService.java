package com.expensetracker.big_brother.report;

import com.expensetracker.big_brother.budget.Budget;
import com.expensetracker.big_brother.budget.BudgetRepository;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.report.dto.*;
import com.expensetracker.big_brother.report.dto.projection.*;
import com.expensetracker.big_brother.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public MonthlySummaryResponse getMonthlySummary(UUID userId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new IllegalArgumentException("Start date must be before end date");

        List<TypeSum> rows = transactionRepository.sumByType(userId, from, to);

        BigDecimal[] totals = {BigDecimal.ZERO, BigDecimal.ZERO};
        rows.forEach(row -> {
            switch (row.type()) {
                case INCOME -> totals[0] = totals[0].add(row.total());
                case EXPENSE -> totals[1] = totals[1].add(row.total());
            }
        });
        long count = transactionRepository.countByUserIdAndTransactionDateBetween(userId, from, to);
        return new MonthlySummaryResponse(totals[0], totals[1], totals[0].subtract(totals[1]), count);
    }

    public List<CategoryBreakdownResponse> getCategoryBreakdown(
            UUID userId, LocalDate from, LocalDate to, TransactionType type) {
        if (from.isAfter(to)) throw new IllegalArgumentException("Start date must be before end date");

        List<CategorySum> rows = transactionRepository.sumByCategory(userId, from, to, type);

        BigDecimal total = rows.stream().map(CategorySum::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream()
                .map(row -> new CategoryBreakdownResponse(
                        row.categoryId(),
                        row.name(),
                        row.color(),
                        row.icon(),
                        row.type(),
                        row.amount(),
                        computePercentage(row.amount(), total)
                )).toList();

    }


    public List<TrendResponse> getTrend(UUID userId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new IllegalArgumentException("Start date must be before end date");

        List<MonthSum> rows = transactionRepository.sumByMonth(userId, from, to);

        return rows.stream()
                .collect(Collectors.groupingBy(
                        MonthSum::month,
                        Collectors.toMap(
                                MonthSum::type,
                                MonthSum::total
                        )
                )).entrySet().stream()
                .map(entry -> new TrendResponse(
                        entry.getKey(),
                        entry.getValue().getOrDefault(TransactionType.INCOME, BigDecimal.ZERO),
                        entry.getValue().getOrDefault(TransactionType.EXPENSE, BigDecimal.ZERO)
                )).sorted(Comparator.comparing(TrendResponse::month)).toList();
    }

    @Transactional(readOnly = true)
    public List<BudgetComparisonResponse> getBudgetComparison(UUID userId, String month) {
        List<Budget> budgets = budgetRepository.findAllByUserIdAndMonth(userId, month);
        List<CategoryExpenses> spending = transactionRepository.expensesByCategory(userId, month);

        Map<UUID, BigDecimal> spendingMap = spending.stream()
                .collect(Collectors.toMap(CategoryExpenses::categoryId, CategoryExpenses::amount));

        List<BudgetComparisonResponse> responseList = new ArrayList<>(budgets.stream()
                .map(budget -> {
                    BigDecimal spent = spendingMap.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
                    BigDecimal remaining = budget.getLimitAmount().subtract(spent);
                    double percentUsed = computePercentage(spent, budget.getLimitAmount());
                    return new BudgetComparisonResponse(
                            budget.getCategory().getId(), budget.getCategory().getName(), budget.getCategory().getColor(),
                            budget.getLimitAmount(), spent, remaining, percentUsed);
                }).toList());

        Set<UUID> budgetedCategoryIds = budgets.stream().map(b ->
                b.getCategory().getId()).collect(Collectors.toSet());
        spending.stream()
                .filter(s -> !budgetedCategoryIds.contains(s.categoryId()))
                .forEach(s -> responseList.add(new BudgetComparisonResponse(
                        s.categoryId(), s.name(), s.color(),
                        BigDecimal.ZERO, s.amount(), s.amount().negate(), 0.0)));
        return responseList;
    }

    public List<PaymentBreakdown> paymentMethodBreakdowns(UUID userId, LocalDate from, LocalDate to, TransactionType type) {
        List<PaymentSum> rows = transactionRepository.sumByPaymentMethod(userId, from, to, type);
        BigDecimal total = rows.stream()
                .map(PaymentSum::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return rows.stream()
                .map(row -> new PaymentBreakdown(
                        row.paymentMethod(),
                        row.categoryId(),
                        row.categoryName(),
                        row.categoryColor(),
                        row.amount(),
                        computePercentage(row.amount(), total)
                )).toList();

    }

    private double computePercentage(BigDecimal part, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return part
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

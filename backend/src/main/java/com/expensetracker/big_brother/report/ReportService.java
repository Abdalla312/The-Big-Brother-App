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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    public List<BudgetComparisonResponse> getBudgetComparison(UUID userId, String month) {

        List<CategoryExpenses> spending = transactionRepository.expensesByCategory(userId, month);

        List<Budget> budgets = budgetRepository.findAllByUserIdAndMonth(userId, month);
        Map<UUID, BigDecimal> budgetMap = budgets.stream()
                .collect(Collectors.toMap(
                        b -> b.getCategory().getId(),
                        Budget::getLimitAmount
                ));
        return spending.stream()
                .map(s -> {
                    BigDecimal budgeted = budgetMap.getOrDefault(s.categoryId(), BigDecimal.ZERO);
                    BigDecimal remaining = budgeted.subtract(s.amount());
                    double percentUsed = computePercentage(s.amount(), budgeted);
                    return new BudgetComparisonResponse(
                            s.categoryId(), s.name(), s.color(),
                            budgeted, s.amount(), remaining, percentUsed);
                }).toList();
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

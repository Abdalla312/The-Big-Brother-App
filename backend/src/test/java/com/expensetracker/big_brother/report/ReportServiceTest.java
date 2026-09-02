package com.expensetracker.big_brother.report;

import com.expensetracker.big_brother.budget.Budget;
import com.expensetracker.big_brother.budget.BudgetRepository;
import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.report.dto.*;
import com.expensetracker.big_brother.report.dto.projection.*;
import com.expensetracker.big_brother.transaction.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final LocalDate from = YearMonth.now().atDay(1);
    private final LocalDate to = YearMonth.now().atEndOfMonth();
    private final String month = LocalDate.now().toString();

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @InjectMocks
    private ReportService reportService;


    private Category aCategory() {
        Category c = new Category();
        c.setId(categoryId);
        c.setName("Food");
        c.setType(TransactionType.EXPENSE);
        c.setIcon("folder");
        c.setColor("#FF0000");
        return c;
    }

    private Budget aBudget(BigDecimal limit) {
        Budget b = new Budget();
        b.setId(UUID.randomUUID());
        b.setMonth(month);
        b.setLimitAmount(limit);
        b.setCategory(aCategory());
        return b;
    }

    private TypeSum aTypeSum(TransactionType type, BigDecimal total) {
        return new TypeSum(type, total);
    }

    private CategorySum aCategorySum(UUID id, String name, TransactionType type, BigDecimal amount) {
        return new CategorySum(id, name, "#FF0000", "icon", type, amount);
    }

    private MonthSum aMonthSum(String month, TransactionType type, BigDecimal total) {
        return new MonthSum(month, type, total);
    }

    private CategoryExpenses aCategoryExpenses(UUID id, String name, BigDecimal amount) {
        return new CategoryExpenses(id, name, "##FF0000", amount);
    }

    private PaymentSum aPaymentSum(String paymentMethod, UUID categoryId, String catName, BigDecimal amount) {
        return new PaymentSum(paymentMethod, categoryId, catName, "#FF0000", amount);
    }

    @Test
    void getMonthlySummary_Success() {
        TypeSum incomeSum = aTypeSum(TransactionType.INCOME, new BigDecimal("1000.0"));
        TypeSum expenseSum = aTypeSum(TransactionType.EXPENSE, new BigDecimal("400.0"));

        when(transactionRepository.sumByType(userId, from, to)).thenReturn(List.of(incomeSum, expenseSum));
        when(transactionRepository.countByUserIdAndTransactionDateBetween(userId, from, to)).thenReturn(5L);

        MonthlySummaryResponse response = reportService.getMonthlySummary(userId, from, to);

        assertThat(response).isNotNull();
        assertThat(response.totalIncome()).isEqualByComparingTo("1000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("400.00");
        assertThat(response.netBalance()).isEqualByComparingTo("600.00");
        assertThat(response.transactionCount()).isEqualTo(5L);

        verify(transactionRepository).sumByType(userId, from, to);
        verify(transactionRepository).countByUserIdAndTransactionDateBetween(userId, from, to);
    }

    @Test
    void getMonthlySummary_NoTransactions_ReturnsZeroTotals() {
        when(transactionRepository.sumByType(userId, from, to)).thenReturn(List.of());
        when(transactionRepository.countByUserIdAndTransactionDateBetween(userId, from, to)).thenReturn(0L);

        MonthlySummaryResponse response = reportService.getMonthlySummary(userId, from, to);

        assertThat(response.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("0.00");
        assertThat(response.netBalance()).isEqualByComparingTo("0.00");
        assertThat(response.transactionCount()).isEqualByComparingTo(0L);

        verify(transactionRepository).sumByType(userId, from, to);
        verify(transactionRepository).countByUserIdAndTransactionDateBetween(userId, from, to);
    }

    @Test
    void getMonthlySummary_OnlyIncome_ExpenseIsZero() {
        TypeSum incomeSum = aTypeSum(TransactionType.INCOME, new BigDecimal("1000.0"));

        when(transactionRepository.sumByType(userId, from, to)).thenReturn(List.of(incomeSum));
        when(transactionRepository.countByUserIdAndTransactionDateBetween(userId, from, to)).thenReturn(1L);

        MonthlySummaryResponse response = reportService.getMonthlySummary(userId, from, to);

        assertThat(response.totalIncome()).isEqualByComparingTo("1000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("0.00");
        assertThat(response.netBalance()).isEqualByComparingTo("1000.00");
        assertThat(response.transactionCount()).isEqualByComparingTo(1L);

        verify(transactionRepository).sumByType(userId, from, to);
        verify(transactionRepository).countByUserIdAndTransactionDateBetween(userId, from, to);
    }

    @Test
    void getMonthlySummary_OnlyExpense_IncomeIsZero() {
        TypeSum expenseSum = aTypeSum(TransactionType.EXPENSE, new BigDecimal("1000.0"));

        when(transactionRepository.sumByType(userId, from, to)).thenReturn(List.of(expenseSum));
        when(transactionRepository.countByUserIdAndTransactionDateBetween(userId, from, to)).thenReturn(1L);

        MonthlySummaryResponse response = reportService.getMonthlySummary(userId, from, to);

        assertThat(response.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("1000.00");
        assertThat(response.netBalance()).isEqualByComparingTo("-1000.00");
        assertThat(response.transactionCount()).isEqualByComparingTo(1L);

        verify(transactionRepository).sumByType(userId, from, to);
        verify(transactionRepository).countByUserIdAndTransactionDateBetween(userId, from, to);
    }

    @Test
    void getMonthlySummary_SameStartAndEndDate_Success() {
        TypeSum incomeSum = aTypeSum(TransactionType.INCOME, new BigDecimal("1000.0"));
        TypeSum expenseSum = aTypeSum(TransactionType.EXPENSE, new BigDecimal("400.0"));

        when(transactionRepository.sumByType(userId, from, from)).thenReturn(List.of(incomeSum, expenseSum));
        when(transactionRepository.countByUserIdAndTransactionDateBetween(userId, from, from)).thenReturn(5L);

        MonthlySummaryResponse response = reportService.getMonthlySummary(userId, from, from);

        assertThat(response).isNotNull();
        assertThat(response.totalIncome()).isEqualByComparingTo("1000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("400.00");
        assertThat(response.netBalance()).isEqualByComparingTo("600.00");
        assertThat(response.transactionCount()).isEqualTo(5L);

        verify(transactionRepository).sumByType(userId, from, from);
        verify(transactionRepository).countByUserIdAndTransactionDateBetween(userId, from, from);
    }

    @Test
    void getMonthlySummary_InvalidDateRange_ThrowsException() {
        assertThatThrownBy(() -> reportService.getMonthlySummary(userId, to, from))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start date must be before end date");
    }

    @Test
    void getCategoryBreakdown_Success() {
        CategorySum food = aCategorySum(categoryId, "Food", TransactionType.EXPENSE, new BigDecimal("300.00"));
        CategorySum rent = aCategorySum(UUID.randomUUID(), "Rent", TransactionType.EXPENSE, new BigDecimal("700.00"));

        when(transactionRepository.sumByCategory(userId, from, to, TransactionType.EXPENSE))
                .thenReturn(List.of(food, rent));
        List<CategoryBreakdownResponse> response = reportService.getCategoryBreakdown(userId, from, to, TransactionType.EXPENSE);

        assertThat(response).hasSize(2);
        assertThat(response.getFirst().percentage()).isEqualTo(30.0);
        assertThat(response.get(1).percentage()).isEqualTo(70.0);
    }

    @Test
    void getCategoryBreakdown_NoTransactions_ReturnsEmptyList() {
        when(transactionRepository.sumByCategory(userId, from, to, TransactionType.EXPENSE))
                .thenReturn(List.of());

        List<CategoryBreakdownResponse> response = reportService.getCategoryBreakdown(userId, from, to, TransactionType.EXPENSE);

        assertThat(response).hasSize(0);

    }

    @Test
    void getCategoryBreakdown_SingleCategory_Returns100Percent() {
        CategorySum food = aCategorySum(categoryId, "Food", TransactionType.EXPENSE, new BigDecimal("300.00"));

        when(transactionRepository.sumByCategory(userId, from, to, TransactionType.EXPENSE))
                .thenReturn(List.of(food));
        List<CategoryBreakdownResponse> response = reportService.getCategoryBreakdown(userId, from, to, TransactionType.EXPENSE);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().percentage()).isEqualTo(100.0);
    }

    @Test
    void getCategoryBreakdown_InvalidDateRange_ThrowsException() {
        assertThatThrownBy(() -> reportService.getCategoryBreakdown(userId, to, from, TransactionType.EXPENSE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getTrend_Success() {
        MonthSum monthIncome = aMonthSum(LocalDate.now().toString(), TransactionType.INCOME, new BigDecimal("1000.00"));
        MonthSum monthExpense = aMonthSum(LocalDate.now().toString(), TransactionType.EXPENSE, new BigDecimal("400.00"));

        when(transactionRepository.sumByMonth(userId, from, to)).thenReturn(List.of(monthIncome, monthExpense));
        List<TrendResponse> response = reportService.getTrend(userId, from, to);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().month()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getFirst().income()).isEqualByComparingTo("1000.00");
        assertThat(response.getFirst().expenses()).isEqualByComparingTo("400.00");
    }

    @Test
    void getTrend_MonthWithOnlyIncome_DefaultsExpenseToZero() {
        MonthSum monthIncome = aMonthSum(LocalDate.now().toString(), TransactionType.INCOME, new BigDecimal("1000.00"));

        when(transactionRepository.sumByMonth(userId, from, to)).thenReturn(List.of(monthIncome));
        List<TrendResponse> response = reportService.getTrend(userId, from, to);

        assertThat(response.getFirst().expenses()).isEqualByComparingTo("0.0");
        assertThat(response.getFirst().income()).isEqualByComparingTo("1000.00");
    }

    @Test
    void getTrend_MonthWithOnlyExpense_DefaultsIncomeToZero() {
        MonthSum monthIncome = aMonthSum(LocalDate.now().toString(), TransactionType.EXPENSE, new BigDecimal("1000.00"));

        when(transactionRepository.sumByMonth(userId, from, to)).thenReturn(List.of(monthIncome));
        List<TrendResponse> response = reportService.getTrend(userId, from, to);

        assertThat(response.getFirst().income()).isEqualByComparingTo("0.0");
        assertThat(response.getFirst().expenses()).isEqualByComparingTo("1000.00");
    }

    @Test
    void getTrend_NoTransactions_ReturnsEmptyList() {
        when(transactionRepository.sumByMonth(userId, from, to)).thenReturn(List.of());
        List<TrendResponse> response = reportService.getTrend(userId, from, to);

        assertThat(response).hasSize(0);
    }

    @Test
    void getTrend_InvalidDateRange_ThrowsException() {
        assertThatThrownBy(() -> reportService.getTrend(userId, to, from))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getBudgetComparison_Success() {
        CategoryExpenses foodExpense = aCategoryExpenses(categoryId, "Food", new BigDecimal("300.0"));
        Budget foodBudget = aBudget(new BigDecimal("1000.00"));

        when(transactionRepository.expensesByCategory(userId, month)).thenReturn(List.of(foodExpense));
        when(budgetRepository.findAllByUserIdAndMonth(userId, month)).thenReturn(List.of(foodBudget));

        List<BudgetComparisonResponse> response = reportService.getBudgetComparison(userId, month);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().budgeted()).isEqualByComparingTo("1000.00");
        assertThat(response.getFirst().spent()).isEqualByComparingTo("300.00");
        assertThat(response.getFirst().remaining()).isEqualByComparingTo("700.00");
        assertThat(response.getFirst().percentUsed()).isEqualTo(30.0);
    }

    @Test
    void getBudgetComparison_OverBudget_ReturnsNegativeRemaining() {
        CategoryExpenses foodExpense = aCategoryExpenses(categoryId, "Food", new BigDecimal("1300.0"));
        Budget foodBudget = aBudget(new BigDecimal("1000.00"));

        when(transactionRepository.expensesByCategory(userId, month)).thenReturn(List.of(foodExpense));
        when(budgetRepository.findAllByUserIdAndMonth(userId, month)).thenReturn(List.of(foodBudget));

        List<BudgetComparisonResponse> response = reportService.getBudgetComparison(userId, month);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().budgeted()).isEqualByComparingTo("1000.00");
        assertThat(response.getFirst().spent()).isEqualByComparingTo("1300.00");
        assertThat(response.getFirst().remaining()).isEqualByComparingTo("-300.00");
        assertThat(response.getFirst().percentUsed()).isEqualTo(130.0);
    }

    @Test
    void getBudgetComparison_UnbudgetedCategory_DefaultsBudgetToZero() {
        CategoryExpenses foodExpense = aCategoryExpenses(categoryId, "Food", new BigDecimal("300.00"));

        when(transactionRepository.expensesByCategory(userId, month)).thenReturn(List.of(foodExpense));
        when(budgetRepository.findAllByUserIdAndMonth(userId, month)).thenReturn(List.of());

        List<BudgetComparisonResponse> response = reportService.getBudgetComparison(userId, month);

        assertThat(response.getFirst().budgeted()).isEqualByComparingTo("0.0");
        assertThat(response.getFirst().percentUsed()).isEqualTo(0.0);
        assertThat(response.getFirst().spent()).isEqualByComparingTo("300.0");
        assertThat(response.getFirst().remaining()).isEqualByComparingTo("-300.0");
    }

    @Test
    void getBudgetComparison_ZeroLimitBudget_ReturnsZeroPercent() {
        CategoryExpenses foodExpense = aCategoryExpenses(categoryId, "Food", new BigDecimal("300.00"));
        Budget foodBudget = aBudget(new BigDecimal("0.00"));

        when(transactionRepository.expensesByCategory(userId, month)).thenReturn(List.of(foodExpense));
        when(budgetRepository.findAllByUserIdAndMonth(userId, month)).thenReturn(List.of(foodBudget));

        List<BudgetComparisonResponse> response = reportService.getBudgetComparison(userId, month);

        assertThat(response.getFirst().budgeted()).isEqualByComparingTo("0.0");
        assertThat(response.getFirst().percentUsed()).isEqualTo(0.0);
        assertThat(response.getFirst().budgeted()).isEqualByComparingTo("0.0");
        assertThat(response.getFirst().spent()).isEqualByComparingTo("300.0");
        assertThat(response.getFirst().remaining()).isEqualByComparingTo("-300.0");
    }

    @Test
    void getBudgetComparison_NoSpendingInMonth_ReturnsAll() {
        Budget foodBudget = aBudget(new BigDecimal("1000.00"));

        when(transactionRepository.expensesByCategory(userId, month)).thenReturn(List.of());
        when(budgetRepository.findAllByUserIdAndMonth(userId, month)).thenReturn(List.of(foodBudget));

        List<BudgetComparisonResponse> response = reportService.getBudgetComparison(userId, month);

        assertThat(response).hasSize(1);
    }

    @Test
    void paymentMethodBreakdowns_Success() {
        PaymentSum cash = aPaymentSum("Cash", categoryId, "Food", new BigDecimal("200.00"));
        PaymentSum card = aPaymentSum("Card", categoryId, "Food", new BigDecimal("800.00"));

        when(transactionRepository.sumByPaymentMethod(userId, from, to, TransactionType.EXPENSE)).thenReturn(List.of(cash, card));

        List<PaymentBreakdown> response = reportService.paymentMethodBreakdowns(userId, from, to, TransactionType.EXPENSE);

        assertThat(response).hasSize(2);
        assertThat(response.getFirst().percentage()).isEqualTo(20.0);
        assertThat(response.get(1).percentage()).isEqualTo(80.0);
    }

    @Test
    void paymentMethodBreakdowns_NoTransactions_ReturnsEmptyList() {
        when(transactionRepository.sumByPaymentMethod(userId, from, to, TransactionType.EXPENSE)).thenReturn(List.of());

        List<PaymentBreakdown> response = reportService.paymentMethodBreakdowns(userId, from, to, TransactionType.EXPENSE);

        assertThat(response).hasSize(0);
    }
}

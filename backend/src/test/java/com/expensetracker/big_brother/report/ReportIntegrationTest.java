package com.expensetracker.big_brother.report;

import com.expensetracker.big_brother.BaseIntegrationTest;
import com.expensetracker.big_brother.budget.Budget;
import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.security.CustomUserDetails;
import com.expensetracker.big_brother.transaction.Transaction;
import com.expensetracker.big_brother.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReportIntegrationTest extends BaseIntegrationTest {
    private CustomUserDetails userAPrincipal;
    private User userA;
    private Category expenseCategory;
    private Category incomeCategory;


    private final LocalDate from = YearMonth.now().atDay(1);
    private final LocalDate to = YearMonth.now().atEndOfMonth();
    private final String month = LocalDate.now().toString();

    @BeforeEach
    void setUp() {
        clearDatabase();
        userA = seedUser("userA@example.com", "User A");
        userAPrincipal = new CustomUserDetails(userA);
        expenseCategory = seedCategory("Food", TransactionType.EXPENSE, userA);
        incomeCategory = seedCategory("Salary", TransactionType.INCOME, userA);
    }

    // GET /api/v1/reports/summary
    @Test
    void getMonthlySummary_ValidRequest_Returns200() throws Exception {
        seedTransaction(new BigDecimal("1000.0"), LocalDate.now(), userA, incomeCategory);
        seedTransaction(new BigDecimal("500.0"), LocalDate.now(), userA, expenseCategory);

        performGet("/api/v1/reports/summary" + "?startDate=" + from + "&endDate=" + to, userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalIncome").value("1000.0"))
                .andExpect(jsonPath("$.data.totalExpenses").value("500.0"))
                .andExpect(jsonPath("$.data.netBalance").value("500.0"))
                .andExpect(jsonPath("$.data.transactionCount").value(2));
    }

    @Test
    void getMonthlySummary_MissingStartDate_Returns400() throws Exception {
        performGet("/api/v1/reports/summary" + "?endDate=" + to, userAPrincipal)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Required parameter 'startDate' is missing"));
    }

    @Test
    void getMonthlySummary_MissingEndDate_Returns400() throws Exception {
        performGet("/api/v1/reports/summary" + "?startDate=" + from, userAPrincipal)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Required parameter 'endDate' is missing"));
    }

    @Test
    void getMonthlySummary_InvalidDateFormat_Returns400() throws Exception {
        performGet("/api/v1/reports/summary" + "?startDate=bad-date" + "&endDate=" + to, userAPrincipal)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'startDate': bad-date"));
    }

    @Test
    void getMonthlySummary_StartDateAfterEndDate_Returns400() throws Exception {
        performGet("/api/v1/reports/summary" + "?startDate=" + to + "&endDate=" + from, userAPrincipal)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Start date must be before end date"));

    }

    // GET /api/v1/reports/category-breakdown
    @Test
    void getCategoryBreakdown_ValidRequest_DefaultsToExpense_Returns200() throws Exception{
        Category utilitiesCat =  seedCategory("Utilities", TransactionType.EXPENSE, userA);
        seedTransaction(new BigDecimal("100.0"), LocalDate.now(), userA, utilitiesCat);
        seedTransaction(new BigDecimal("500.0"), LocalDate.now(), userA, expenseCategory);
        performGet("/api/v1/reports/category-breakdown" + "?startDate=" + from + "&endDate=" + to , userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category breakdown retrieved"))
                .andExpect(jsonPath("$.data[0].amount").value("500.0"))
                .andExpect(jsonPath("$.data[1].amount").value("100.0"));
    }

    @Test
    void getCategoryBreakdown_WithIncomeType_Returns200() throws Exception {
        seedTransaction(new BigDecimal("1000.00"), LocalDate.now(), userA, incomeCategory);
        seedTransaction(new BigDecimal("300.00"), LocalDate.now(), userA, expenseCategory);
        performGet("/api/v1/reports/category-breakdown" + "?startDate=" + from + "&endDate=" + to + "&type=INCOME", userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("INCOME"))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void getCategoryBreakdown_InvalidTypeEnum_Returns400() throws Exception {
        performGet("/api/v1/reports/category-breakdown" + "?startDate=" + from + "&endDate=" + to + "&type=INVALID", userAPrincipal)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'type': INVALID"));
    }

    @Test
    void getCategoryBreakdown_MissingDates_Returns400() throws Exception {
        performGet("/api/v1/reports/category-breakdown", userAPrincipal)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'startDate' is missing"));
    }

    // GET /api/v1/reports/trend
    @Test
    void getTrend_ValidDateRange_Returns200() throws Exception {
        seedTransaction(new BigDecimal("500.00"), LocalDate.now(), userA, expenseCategory);
        seedTransaction(new BigDecimal("1000.00"), LocalDate.now(), userA, incomeCategory);

        performGet("/api/v1/reports/trend" + "?startDate=" + from + "&endDate=" + to, userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Trend retrieved"))
                .andExpect(jsonPath("$.data[0].month").value(YearMonth.now().toString()))
                .andExpect(jsonPath("$.data[0].income").value("1000.0"))
                .andExpect(jsonPath("$.data[0].expenses").value("500.0"));
    }

    @Test
    void getTrend_MissingDates_Returns400() throws Exception {
        performGet("/api/v1/reports/trend", userAPrincipal)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'startDate' is missing"));

    }

    // GET /api/v1/reports/budget-comparison
    @Test
    void getBudgetComparison_ValidMonth_Returns200() throws Exception {
        Category utilitiesCat = seedCategory("Utilities", TransactionType.EXPENSE, userA);
        Budget utilitiesBudget = seedBudget(userA, utilitiesCat, YearMonth.now().toString(), new BigDecimal("2000"));
        Budget foodBudget = seedBudget(userA, expenseCategory, YearMonth.now().toString(), new BigDecimal("3000"));
        seedTransaction(new BigDecimal("500.00"),LocalDate.now(), userA, utilitiesCat);
        seedTransaction(new BigDecimal("1500.00"),LocalDate.now(), userA, expenseCategory);

        performGet("/api/v1/reports/budget-comparison" + "?month=" + YearMonth.now(), userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Budget comparison retrieved"))
                .andExpect(jsonPath("$.data[*].name").value(containsInAnyOrder("Utilities", "Food")))
                .andExpect(jsonPath("$.data[?(@.name=='Utilities')].percentUsed").value(25.0))
                .andExpect(jsonPath("$.data[?(@.name=='Utilities')].remaining").value(1500.0))
                .andExpect(jsonPath("$.data[?(@.name=='Food')].percentUsed").value(50.0))
                .andExpect(jsonPath("$.data[?(@.name=='Food')].remaining").value(1500.0));
    }

    @Test
    void getBudgetComparison_MissingMonth_Returns400() throws Exception {
        performGet("/api/v1/reports/budget-comparison", userAPrincipal)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'month' is missing"));
    }

    @Test
    void getBudgetComparison_InvalidMonthFormat_Returns400() throws Exception {
        performGet("/api/v1/reports/budget-comparison" + "?month=abc", userAPrincipal)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].message").value("Month must be in yyyy-MM format"));
    }

    @Test
    void getPaymentMethodBreakdown_ValidRequest_DefaultsToExpense_Returns200() throws Exception {
        Transaction cashTransaction = seedTransaction(new BigDecimal("500.0"), LocalDate.now(), userA, expenseCategory);
        Transaction creditTransaction = seedTransaction(new BigDecimal("1500.0"), LocalDate.now(), userA, expenseCategory);

        cashTransaction.setPaymentMethod("Cash");
        creditTransaction.setPaymentMethod("Credit Card");

        transactionRepository.save(cashTransaction);
        transactionRepository.save(creditTransaction);

        performGet("/api/v1/reports/payment-method-breakdown" + "?startDate=" + from + "&endDate=" + to, userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment method breakdown retrieved"))
                .andExpect(jsonPath("$.data[*].paymentMethod").value(containsInAnyOrder("Cash", "Credit Card")))
                .andExpect(jsonPath("$.data[?(@.paymentMethod=='Cash')].amount").value(500.0))
                .andExpect(jsonPath("$.data[?(@.paymentMethod=='Cash')].percentage").value(25.0))
                .andExpect(jsonPath("$.data[?(@.paymentMethod=='Credit Card')].amount").value(1500.0))
                .andExpect(jsonPath("$.data[?(@.paymentMethod=='Credit Card')].percentage").value(75.0));
    }

    @Test
    void getPaymentMethodBreakdown_WithIncomeType_Returns200() throws Exception {
        Transaction bankTransferTransaction = seedTransaction(new BigDecimal("500.0"), LocalDate.now(), userA, incomeCategory);
        Transaction creditTransaction = seedTransaction(new BigDecimal("1500.0"), LocalDate.now(), userA, incomeCategory);

        bankTransferTransaction.setPaymentMethod("Bank Transfer");
        creditTransaction.setPaymentMethod("Credit Card");

        transactionRepository.save(bankTransferTransaction);
        transactionRepository.save(creditTransaction);

        performGet("/api/v1/reports/payment-method-breakdown" + "?startDate=" + from + "&endDate=" + to + "&type=INCOME", userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].paymentMethod").value(containsInAnyOrder("Bank Transfer", "Credit Card")));
    }

    @Test
    void getPaymentMethodBreakdown_missingDates_returns400() throws Exception {
        performGet("/api/v1/reports/payment-method-breakdown", userAPrincipal)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'startDate' is missing"));
    }
}

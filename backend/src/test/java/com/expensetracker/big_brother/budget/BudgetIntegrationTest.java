package com.expensetracker.big_brother.budget;

import com.expensetracker.big_brother.BaseIntegrationTest;
import com.expensetracker.big_brother.budget.dto.BudgetRequest;
import com.expensetracker.big_brother.budget.dto.UpdateBudgetRequest;
import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.security.CustomUserDetails;
import com.expensetracker.big_brother.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BudgetIntegrationTest extends BaseIntegrationTest {
    private CustomUserDetails userAPrincipal;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        clearDatabase();
        userA = seedUser("userA@example.com", "User A");
        userB = seedUser("userB@example.com", "User B");
        userAPrincipal = new CustomUserDetails(userA);
    }

    @Test
    void getBudgets_WithSpentCalculations_Returns200() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        seedBudget(userA, category, "2026-07", new BigDecimal("1000.00"));
        seedTransaction(new BigDecimal("150.00"), LocalDate.of(2026, 7, 5), userA, category);
        seedTransaction(new BigDecimal("50.00"), LocalDate.of(2026, 7, 15), userA, category);
        performGet("/api/v1/budget?month=2026-07", userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].limitAmount").value("1000.0"))
                .andExpect(jsonPath("$.data[0].spentAmount").value("200.0"))
                .andExpect(jsonPath("$.data[0].remainingAmount").value("800.0"))
                .andExpect(jsonPath("$.data[0].percentUsed").value("20.0"));
    }

    @Test
    void getBudgets_NoBudgetsFound_Returns200() throws Exception {
        performGet("/api/v1/budget?month=2026-06", userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void getBudgets_InvalidMonthFormat_Returns400() throws Exception {
        performGet("/api/v1/budget?month=07-2026", userAPrincipal)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void getBudgets_UnAuthorized_Returns401() throws Exception {
        performGet("/api/v1/budget?month=07-2026", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // POST /api/v1/budget
    @Test
    void createBudget_ValidRequest_Returns201() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        BudgetRequest request = new BudgetRequest(
                category.getId(), YearMonth.now().toString(), new BigDecimal("1000.0"));
        performPost("/api/v1/budget", userAPrincipal, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Budget created"))
                .andExpect(jsonPath("$.data.spentAmount").value("0"));
    }

    @Test
    void createBudget_NullCategory_Returns400() throws Exception {
        BudgetRequest request = new BudgetRequest(
                null, YearMonth.now().toString(), new BigDecimal("1000.0"));
        performPost("/api/v1/budget", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].message").value("Category is required"));
    }

    @Test
    void createBudget_NullMonth_Returns400() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        BudgetRequest request = new BudgetRequest(
                category.getId(), null, new BigDecimal("1000.0"));
        performPost("/api/v1/budget", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].message").value("Month is required (format: yyyy-MM)"));
    }

    @Test
    void createBudget_InvalidMonthPattern_Returns400() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        BudgetRequest request = new BudgetRequest(
                category.getId(), "2026-7", new BigDecimal("1000.0"));
        performPost("/api/v1/budget", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].message").value("Month must be in yyyy-MM format"));
    }

    @Test
    void createBudget_NullLimitAmount_Returns400() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        BudgetRequest request = new BudgetRequest(
                category.getId(), "2026-07", null);
        performPost("/api/v1/budget", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].message").value("Budget limit is required"));
    }

    @Test
    void createBudget_InvalidLimitAmount_Returns400() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        BudgetRequest request = new BudgetRequest(
                category.getId(), "2026-07", BigDecimal.ZERO);
        performPost("/api/v1/budget", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].message").value("Budget limit must be greater than zero"));
    }

    @Test
    void createBudget_NonExistentCategory_Returns404() throws Exception {
        BudgetRequest request = new BudgetRequest(
                UUID.randomUUID(), "2026-07", new BigDecimal("1000.0"));
        performPost("/api/v1/budget", userAPrincipal, request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("Category not found with id: ")));

    }

    @Test
    void createBudget_OtherUsersCategory_Returns403() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userB);
        BudgetRequest request = new BudgetRequest(
                category.getId(), YearMonth.now().toString(), new BigDecimal("1000.0"));
        performPost("/api/v1/budget", userAPrincipal, request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void createBudget_DuplicateBudget_Returns409() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        Budget budget = seedBudget(userA, category, YearMonth.now().toString(), new BigDecimal("1000.0"));
        BudgetRequest request = new BudgetRequest(
                category.getId(), YearMonth.now().toString(), new BigDecimal("1000.0"));
        performPost("/api/v1/budget", userAPrincipal, request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("A budget for this month already exists for ")));
    }

    @Test
    void createBudget_UnAuthorized_Returns401() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        BudgetRequest request = new BudgetRequest(
                category.getId(), YearMonth.now().toString(), new BigDecimal("1000.0"));
        performPost("/api/v1/budget", null, request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"));
    }

    // PATCH /api/v1/budget
    @Test
    void updateBudget_UpdateLimit_Returns200() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        Budget budget = seedBudget(userA, category, YearMonth.now().toString(), new BigDecimal("1000.0"));
        UpdateBudgetRequest request = new UpdateBudgetRequest(null, null, new BigDecimal("1100.0"));
        performPatch("/api/v1/budget/" + budget.getId(), userAPrincipal, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("message").value("Budget updated"))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.limitAmount").value("1100.0"));
    }

    @Test
    void updateBudget_UpdateCategoryAndMonth_Returns200() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        Category newCategory = seedCategory("Utilities", TransactionType.EXPENSE, userA);
        Budget budget = seedBudget(userA, category, YearMonth.now().toString(), new BigDecimal("1000.0"));
        UpdateBudgetRequest request = new UpdateBudgetRequest(newCategory.getId(), YearMonth.of(2026, 6).toString(), null);
        performPatch("/api/v1/budget/" + budget.getId(), userAPrincipal, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("message").value("Budget updated"))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.month").value("2026-06"))
                .andExpect(jsonPath("$.data.category.name").value("Utilities"));
    }

    @Test
    void updateBudget_NonExistentBudget_Returns404() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        UpdateBudgetRequest request = new UpdateBudgetRequest(category.getId(), YearMonth.of(2026, 6).toString(), null);
        performPatch("/api/v1/budget/" + UUID.randomUUID(), userAPrincipal, request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("Budget not found with id: ")));
    }

    @Test
    void updateBudget_OtherUsersBudget_Returns403() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userB);
        Budget budget = seedBudget(userB, category, YearMonth.now().toString(), new BigDecimal("1000.0"));
        UpdateBudgetRequest request = new UpdateBudgetRequest(null, YearMonth.of(2026, 6).toString(), null);
        performPatch("/api/v1/budget/" + budget.getId(), userAPrincipal, request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void updateBudget_NonExistentNewCategory_Returns404() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        Budget budget = seedBudget(userA, category, YearMonth.now().toString(), new BigDecimal("1000.0"));
        UpdateBudgetRequest request = new UpdateBudgetRequest(UUID.randomUUID(), null, null);
        performPatch("/api/v1/budget/" + budget.getId(), userAPrincipal, request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("Category not found with id: ")));
    }

    @Test
    void updateBudget_OtherUsersCategory_Returns403() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        Category userBCategory = seedCategory("Utilities", TransactionType.EXPENSE, userB);
        Budget budget = seedBudget(userA, category, YearMonth.now().toString(), new BigDecimal("1000.0"));
        UpdateBudgetRequest request = new UpdateBudgetRequest(userBCategory.getId(), null, null);
        performPatch("/api/v1/budget/" + budget.getId(), userAPrincipal, request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void updateBudget_InvalidMonthAndLimit_Returns400() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        Budget budget = seedBudget(userA, category, YearMonth.now().toString(), new BigDecimal("1000.0"));
        UpdateBudgetRequest request = new UpdateBudgetRequest(null, "06-2026", BigDecimal.ZERO);
        performPatch("/api/v1/budget/" + budget.getId(), userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("errors[*].message", containsInAnyOrder("Month must be in yyyy-MM format", "Budget limit must be greater than zero")));

    }

    @Test
    void updateBudget_UnAuthorized_Returns401() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        Budget budget = seedBudget(userA, category, YearMonth.now().toString(), new BigDecimal("1000.0"));
        UpdateBudgetRequest request = new UpdateBudgetRequest(null, null, BigDecimal.ONE);
        performPatch("/api/v1/budget/" + budget.getId(), null, request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"));
    }

    // DELETE /api/v1/budget
//    │1│Success│Delete own budget│200 OK; record absent from DB│
    @Test
    void deleteBudget_Success_Returns200() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        Budget budget = seedBudget(userA, category, YearMonth.now().toString(), new BigDecimal("1000.0"));
        performDelete("/api/v1/budget/" + budget.getId(), userAPrincipal, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Budget deleted"));
        assertThat(budgetRepository.findById(budget.getId())).isEmpty();
    }

    //    │2│Not Found — Budget Does Not Exist│Random {id} UUID│404 Not Found│
    @Test
    void deleteBudget_NonExistentBudget_Returns404() throws Exception {
        performDelete("/api/v1/budget/" + UUID.randomUUID(), userAPrincipal, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("Budget not found with id: ")));
    }

    @Test
    void deleteBudget_OtherUsersBudget_Returns403() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userB);
        Budget budget = seedBudget(userB, category, YearMonth.now().toString(), new BigDecimal("1000.0"));
        performDelete("/api/v1/budget/" + budget.getId(), userAPrincipal, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void deleteBudget_UnAuthorized_Returns401() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        Budget budget = seedBudget(userA, category, YearMonth.now().toString(), new BigDecimal("1000.0"));
        performDelete("/api/v1/budget/" + budget.getId(), null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"));
    }
}

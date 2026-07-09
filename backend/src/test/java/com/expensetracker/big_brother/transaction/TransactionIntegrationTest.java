package com.expensetracker.big_brother.transaction;

import com.expensetracker.big_brother.BaseIntegrationTest;
import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.security.CustomUserDetails;
import com.expensetracker.big_brother.transaction.dto.TransactionRequest;
import com.expensetracker.big_brother.transaction.dto.UpdateTransactionRequest;
import com.expensetracker.big_brother.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TransactionIntegrationTest extends BaseIntegrationTest {

    private CustomUserDetails userAPrincipal;
    private User userA;
    private User userB;
    private Category defaultCategory;
    private Category userACategory;

    @BeforeEach
    void setUp() {
        clearDatabase();
        userA = seedUser("userA@example.com", "User A");
        userB = seedUser("userB@example.com", "User B");
        userAPrincipal = new CustomUserDetails(userA);

        defaultCategory = seedCategory("Food", TransactionType.EXPENSE, null);
        userACategory = seedCategory("Utilities", TransactionType.EXPENSE, userA);
    }

    // - GET /api/v1/transactions
    @Test
    void getTransactions_WithoutFilters_ReturnsPaginatedList() throws Exception {
        seedTransaction(new BigDecimal("50.00"), LocalDate.now(), userA, defaultCategory);
        seedTransaction(new BigDecimal("150.00"), LocalDate.now(), userB, defaultCategory);

        performGet("/api/v1/transactions", userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].amount").value("50.0"));
    }

    @Test
    void getTransactions_WithCategoryFilter_ReturnsFilteredList() throws Exception {
        seedTransaction(new BigDecimal("50.00"), LocalDate.now(), userA, defaultCategory);
        seedTransaction(new BigDecimal("150.00"), LocalDate.now(), userA, userACategory);
        performGet("/api/v1/transactions?categoryId=" + userACategory.getId(), userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].amount").value("150.0"));
    }

    @Test
    void getTransactions_WithMonthFilter_ReturnsFilteredList() throws Exception {
        seedTransaction(new BigDecimal("50.00"), LocalDate.now(), userA, defaultCategory);
        seedTransaction(new BigDecimal("150.00"), LocalDate.now(), userA, userACategory);
        performGet("/api/v1/transactions?month=" + YearMonth.now(), userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].amount").value("50.0"));
    }

    @Test
    void getTransactions_UnAuthorized_Returns401() throws Exception {
        performGet("/api/v1/transactions", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // - POST /api/v1/transactions
    @Test
    void createTransaction_WithValidRequest_Returns201() throws Exception {
        TransactionRequest request = new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("45.50"), LocalDate.now(), "Dinner", "Credit Card", defaultCategory.getId());
        performPost("/api/v1/transactions", userAPrincipal, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value("45.5"))
                .andExpect(jsonPath("$.data.note").value("Dinner"));
    }

    @Test
    void createTransaction_WithInvalidAmount_Returns400() throws Exception {
        TransactionRequest request = new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("-45.50"), LocalDate.now(), "Dinner", "Credit Card", defaultCategory.getId());
        performPost("/api/v1/transactions", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createTransaction_OtherUsersCategory_Returns403() throws Exception {
        Category userBCategory = seedCategory("Private", TransactionType.EXPENSE, userB);
        TransactionRequest request = new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("45.50"), LocalDate.now(), "Dinner", "Credit Card", userBCategory.getId());
        performPost("/api/v1/transactions", userAPrincipal, request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void createTransaction_CategoryNotFound_Returns404() throws Exception {
        TransactionRequest request = new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("45.50"), LocalDate.now(), "Dinner", "Credit Card", UUID.randomUUID());
        performPost("/api/v1/transactions", userAPrincipal, request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // - PATCH /api/v1/transactions/{id} (Update Transaction)
    @Test
    void updateTransaction_WithValidRequest_Returns200() throws Exception {
        Transaction transaction = seedTransaction(new BigDecimal("40.0"), LocalDate.now(), userA, userACategory);
        UpdateTransactionRequest request = new UpdateTransactionRequest(
                null, new BigDecimal("50.0"), null, "updated", null, null);
        performPatch("/api/v1/transactions/" + transaction.getId(), userAPrincipal, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value("50.0"))
                .andExpect(jsonPath("$.message").value("Transaction updated"))
                .andExpect(jsonPath("$.data.note").value("updated"));
    }

    @Test
    void updateTransaction_WithNewCategory_Returns200() throws Exception {
        Transaction transaction = seedTransaction(new BigDecimal("50.00"), LocalDate.now(), userA, defaultCategory);
        Category userACategory = seedCategory("User A Category", TransactionType.EXPENSE, userA);
        UpdateTransactionRequest request = new UpdateTransactionRequest(null, null, null, null, null, userACategory.getId());
        performPatch("/api/v1/transactions/" + transaction.getId(), userAPrincipal, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction updated"))
                .andExpect(jsonPath("$.data.category.name").value("User A Category"));
    }

    @Test
    void updateTransaction_NonExistentTransaction_Returns404() throws Exception {
        UpdateTransactionRequest request = new UpdateTransactionRequest(
                TransactionType.EXPENSE, null, null, null, null, null);
        performPatch("/api/v1/transactions/" + UUID.randomUUID(), userAPrincipal, request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void updateTransaction_OtherUsersTransaction_Returns403() throws Exception {
        Transaction userBTransaction = seedTransaction(
                new BigDecimal("10.0"), LocalDate.now(), userB, defaultCategory);
        UpdateTransactionRequest request = new UpdateTransactionRequest(
                TransactionType.INCOME, null, null, null, null, null);
        performPatch("/api/v1/transactions/" + userBTransaction.getId(), userAPrincipal, request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void updateTransaction_NonExistentNewCategory_Returns404() throws Exception {
        Transaction transaction = seedTransaction(
                new BigDecimal("40.0"), LocalDate.now(), userA, defaultCategory);
        UpdateTransactionRequest request = new UpdateTransactionRequest(
                null, null, null, null, null, UUID.randomUUID());
        performPatch("/api/v1/transactions/" + transaction.getId(), userAPrincipal, request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void updateTransaction_OtherUsersCategory_Return403() throws Exception {
        Transaction transaction = seedTransaction(new BigDecimal("50.00"), LocalDate.now(), userA, defaultCategory);
        Category userBCategory = seedCategory("User B Category", TransactionType.EXPENSE, userB);
        UpdateTransactionRequest request = new UpdateTransactionRequest(null, null, null, null, null, userBCategory.getId());
        performPatch("/api/v1/transactions/" + transaction.getId(), userAPrincipal, request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    //  DELETE /api/v1/transactions/{id} (Delete Transaction)

    @Test
    void deleteTransaction_OwnTransaction_Returns200() throws Exception {
        Transaction transaction = seedTransaction(new BigDecimal("400.0"), LocalDate.now(), userA, defaultCategory);
        performDelete("/api/v1/transactions/" + transaction.getId(), userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction deleted successfully"));
        assertThat(transactionRepository.findById(transaction.getId())).isEmpty();
    }

    @Test
    void deleteTransaction_NonExistentTransaction_Returns404() throws Exception {
        performDelete("/api/v1/transactions/" + UUID.randomUUID(), userAPrincipal)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

    }

    @Test
    void deleteTransaction_OtherUsersTransaction_Returns403() throws Exception {
        Transaction userBTransaction = seedTransaction(new BigDecimal("1.0"), LocalDate.now(), userB, defaultCategory);
        performDelete("/api/v1/transactions/" + userBTransaction.getId(), userAPrincipal)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

    }
}

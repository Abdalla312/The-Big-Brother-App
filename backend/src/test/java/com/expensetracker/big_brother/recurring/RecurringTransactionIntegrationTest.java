package com.expensetracker.big_brother.recurring;


import com.expensetracker.big_brother.BaseIntegrationTest;
import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.recurring.dto.CreateRecurringTransactionRequest;
import com.expensetracker.big_brother.recurring.dto.UpdateRecurringTransactionRequest;
import com.expensetracker.big_brother.security.CustomUserDetails;
import com.expensetracker.big_brother.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class RecurringTransactionIntegrationTest extends BaseIntegrationTest {
    private CustomUserDetails userAPrincipal;
    private User userA, userB;
    private Category defaultCategory, userACategory, userBCategory;
    private RecurringTransaction userARule, userBRule;
    @Autowired private RecurringTransactionRepository recurringTransactionRepository;
    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void setUp() {
        clearDatabase();
        userA = seedUser("userA@example.com", "User A");
        userB = seedUser("userB@example.com", "User B");
        userAPrincipal = new CustomUserDetails(userA);

        defaultCategory = seedCategory("Food", TransactionType.EXPENSE, null);
        userACategory = seedCategory("Utilities", TransactionType.EXPENSE, userA);
        userBCategory = seedCategory("Private", TransactionType.EXPENSE, userB);
        userARule = seedRecurringTransaction(new BigDecimal("100.00"), LocalDate.now(), RecurrenceFrequency.MONTHLY, TransactionType.EXPENSE, userA, userACategory, true);
        userBRule = seedRecurringTransaction(new BigDecimal("150.00"), LocalDate.now(), RecurrenceFrequency.MONTHLY, TransactionType.EXPENSE, userB, userBCategory, true);
    }

    //    GET all
    @Test
    void getAll_ReturnsOnlyCurrentUserRules() throws Exception {
        performGet("/api/v1/recurring-transactions", userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Retrieved"))
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    @Test
    void getAll_UnAuthorized_Returns401() throws Exception {
        performGet("/api/v1/recurring-transactions", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    //    GET one
    @Test
    void getItem_Returns200() throws Exception {
        performGet("/api/v1/recurring-transactions/" + userARule.getId(), userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Retrieved"));
    }

    @Test
    void getItem_NonExistentRule_Returns404() throws Exception {
        performGet("/api/v1/recurring-transactions/" + UUID.randomUUID(), userAPrincipal)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void getItem_OtherUsersRule_Returns403() throws Exception {
        performGet("/api/v1/recurring-transactions/" + userBRule.getId(), userAPrincipal)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    //    POST
    @Test
    void createItem_Success_Returns201() throws Exception {
        CreateRecurringTransactionRequest request = new CreateRecurringTransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("200.00"), userACategory.getId(),
                RecurrenceFrequency.MONTHLY, LocalDate.now(), "test-payment", "test-note");
        performPost("/api/v1/recurring-transactions", userAPrincipal, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Created successfully"));
    }

    @Test
    void createItem_TypeMismatch_Returns400() throws Exception {
        CreateRecurringTransactionRequest request = new CreateRecurringTransactionRequest(
                TransactionType.INCOME, new BigDecimal("200.00"), userACategory.getId(),
                RecurrenceFrequency.MONTHLY, LocalDate.now(), "test-payment", "test-note");
        performPost("/api/v1/recurring-transactions", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transaction type doesn't match category type"));

    }

    @Test
    void createItem_OtherUsersCategory_Returns403() throws Exception {
        CreateRecurringTransactionRequest request = new CreateRecurringTransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("200.00"), userBCategory.getId(),
                RecurrenceFrequency.MONTHLY, LocalDate.now(), "test-payment", "test-note");
        performPost("/api/v1/recurring-transactions", userAPrincipal, request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("You do not have permission to access this resource"));

    }

    @Test
    void createItem_NonExistentCategory_Returns404() throws Exception {
        CreateRecurringTransactionRequest request = new CreateRecurringTransactionRequest(
                TransactionType.INCOME, new BigDecimal("200.00"), UUID.randomUUID(),
                RecurrenceFrequency.MONTHLY, LocalDate.now(), "test-payment", "test-note");
        performPost("/api/v1/recurring-transactions", userAPrincipal, request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Category not found with id:")));
    }

    @Test
    void createItem_InvalidPayload_Returns400() throws Exception {
        CreateRecurringTransactionRequest request = new CreateRecurringTransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("-200.00"), null,
                RecurrenceFrequency.MONTHLY, LocalDate.now(), "test-payment", "test-note");
        performPost("/api/v1/recurring-transactions", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("amount", "categoryId")))
                .andExpect(jsonPath("$.errors[?(@.field=='amount')].message").value("must be greater than 0"))
                .andExpect(jsonPath("$.errors[?(@.field=='categoryId')].message").value("must not be null"));
    }

    //    PATCH
    @Test
    void updateItem_Success_Returns200() throws Exception {
        UpdateRecurringTransactionRequest request = new UpdateRecurringTransactionRequest(
                new BigDecimal("200.0"), null, null, null, null, null);
        performPatch("/api/v1/recurring-transactions/" + userARule.getId(), userAPrincipal, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Updated"))
                .andExpect(jsonPath("$.data.amount").value(200.0));
    }

    @Test
    void updateItem_ChangeCategory_Returns200() throws Exception {
        UpdateRecurringTransactionRequest request = new UpdateRecurringTransactionRequest(
                null, defaultCategory.getId(), null, null, null, null);
        performPatch("/api/v1/recurring-transactions/" + userARule.getId(), userAPrincipal, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Updated"))
                .andExpect(jsonPath("$.data.category.id").value(defaultCategory.getId().toString()));
    }

    @Test
    void updateItem_NonExistentRule_Returns404() throws Exception {
        UpdateRecurringTransactionRequest request = new UpdateRecurringTransactionRequest(
                new BigDecimal("200.0"), null, null, null, null, null);
        performPatch("/api/v1/recurring-transactions/" + UUID.randomUUID(), userAPrincipal, request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("Recurring transaction not found with id:")));
    }

    @Test
    void updateItem_OtherUsersRule_Returns403() throws Exception {
        UpdateRecurringTransactionRequest request = new UpdateRecurringTransactionRequest(
                new BigDecimal("200.0"), null, null, null, null, null);
        performPatch("/api/v1/recurring-transactions/" + userBRule.getId(), userAPrincipal, request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void updateItem_OtherUsersNewCategory_Returns403() throws Exception {
        UpdateRecurringTransactionRequest request = new UpdateRecurringTransactionRequest(
                null, userBCategory.getId(), null, null, null, null);
        performPatch("/api/v1/recurring-transactions/" + userBRule.getId(), userAPrincipal, request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    //    Toggle
    @Test
    void toggleActivity_Success_Returns200() throws Exception {
        performPatch("/api/v1/recurring-transactions/" + userARule.getId() + "/toggle", userAPrincipal, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Status toggled"))
                .andExpect(jsonPath("$.data.isActive").value("false"));
    }

    @Test
    void toggleActivity_NonExistentRule_Returns404() throws Exception {
        performPatch("/api/v1/recurring-transactions/" + UUID.randomUUID() + "/toggle", userAPrincipal, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("Recurring transaction not found with id: ")));
    }

    @Test
    void toggleActivity_OtherUsersRule_Returns403() throws Exception {
        performPatch("/api/v1/recurring-transactions/" + userBRule.getId() + "/toggle", userAPrincipal, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    //    DELETE
    @Test
    void deleteItem_Success_Returns200() throws Exception {
        performDelete("/api/v1/recurring-transactions/" + userARule.getId(), userAPrincipal, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted successfully"));

        entityManager.flush();
        entityManager.clear();

        performGet("/api/v1/recurring-transactions/" + userARule.getId(), userAPrincipal)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void deleteItem_NonExistentRule_Returns404() throws Exception {
        performDelete("/api/v1/recurring-transactions/" + UUID.randomUUID(), userAPrincipal, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void deleteItem_OtherUsersRule_Returns403() throws Exception {
        performDelete("/api/v1/recurring-transactions/" + userBRule.getId(), userAPrincipal, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }
}

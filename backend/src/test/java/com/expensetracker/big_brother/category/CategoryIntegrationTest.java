package com.expensetracker.big_brother.category;

import com.expensetracker.big_brother.BaseIntegrationTest;
import com.expensetracker.big_brother.category.dto.CreateCategoryRequest;
import com.expensetracker.big_brother.category.dto.UpdateCategoryRequest;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.security.CustomUserDetails;
import com.expensetracker.big_brother.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class CategoryIntegrationTest extends BaseIntegrationTest {

    private CustomUserDetails userAPrincipal;
    private User userA;
    private User userB;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void setUp() {
        clearDatabase();
        userA = seedUser("userA@example.com", "User A");
        userB = seedUser("userB@example.com", "User B");
        userAPrincipal = new CustomUserDetails(userA);
    }

    // GET
    @Test
    void getCategories_ReturnsOwnCategories() throws Exception {
        seedCategory("Food (Default)", TransactionType.EXPENSE, null);
        seedCategory("Salary (User A)", TransactionType.INCOME, userA);
        seedCategory("Business (User B)", TransactionType.INCOME, userB);

        performGet("/api/v1/categories", userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("Salary (User A)"));
    }
    @Test
    void getCategories_typeIncome_ReturnsOnlyUsersIncomeCategories() throws Exception{
        seedCategory("Food (Default)", TransactionType.EXPENSE, null);
        seedCategory("Salary (User A)", TransactionType.INCOME, userA);
        seedCategory("Business (User B)", TransactionType.INCOME, userB);
        performGet("/api/v1/categories?type=INCOME", userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("Salary (User A)"));
    }

    @Test
    void getCategories_DefaultCategoriesTrue_ReturnsOnlyDefaultCategories() throws Exception{
        seedCategory("Food (Default)", TransactionType.EXPENSE, null);
        seedCategory("Salary (User A)", TransactionType.INCOME, userA);
        seedCategory("Business (User B)", TransactionType.INCOME, userB);
        performGet("/api/v1/categories?defaultCategories=true", userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("Food (Default)"));
    }

    @Test
    void getCategories_TypeExpense_Returns200() throws Exception{
        seedCategory("Food (Default)", TransactionType.EXPENSE, null);
        seedCategory("Salary (User A)", TransactionType.INCOME, userA);
        seedCategory("Business (User A)", TransactionType.EXPENSE, userA);
        performGet("/api/v1/categories?type=EXPENSE", userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("Business (User A)"));
    }

    @Test
    void getCategories_UnAuthorized_Returns401() throws Exception {
        performGet("/api/v1/categories", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // - POST
    @Test
    void createCategory_WithValidRequest_Returns201() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Utilities", TransactionType.EXPENSE, "#FF0000", "electric");
        performPost("/api/v1/categories", userAPrincipal, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Category created"))
                .andExpect(jsonPath("$.data.name").value("Utilities"));
    }

    @Test
    void createCategory_WithBlankName_Returns400() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(" ", TransactionType.EXPENSE, "#FF0000", "electric");
        performPost("/api/v1/categories", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createCategory_WithNameTooLong_Returns400() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("A".repeat(101), TransactionType.EXPENSE, "#FF0000", "electric");
        performPost("/api/v1/categories", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createCategory_WithNullType_Returns400() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Utilities", null, "#FF0000", "electric");
        performPost("/api/v1/categories", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createCategory_WithColorTooLong_Returns400() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Utilities", TransactionType.EXPENSE, "A".repeat(21), "electric");
        performPost("/api/v1/categories", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createCategory_WithIconTooLong_Returns400() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Utilities", TransactionType.EXPENSE, "#FF0000", "A".repeat(51));
        performPost("/api/v1/categories", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createCategory_UnAuthorized_Returns401() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Utilities", TransactionType.EXPENSE, "#FF0000", "A".repeat(51));
        performPost("/api/v1/categories", null, request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    // - PATCH /api/v1/categories/{id}

    @Test
    void updateCategory_OwnCategory_Returns200() throws Exception {
        Category category = seedCategory("Food", TransactionType.EXPENSE, userA);
        UpdateCategoryRequest request = new UpdateCategoryRequest("New Name", null, null);
        performPatch("/api/v1/categories/" + category.getId(), userAPrincipal, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"));
    }

    @Test
    void updateCategory_NonExistentCategory_Returns404() throws Exception {
        UpdateCategoryRequest request = new UpdateCategoryRequest("New Name", null, null);
        performPatch("/api/v1/categories/" + UUID.randomUUID(), userAPrincipal, request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void updateCategory_DefaultCategory_Returns403() throws Exception {
        Category defaultCategory = seedCategory("Food", TransactionType.EXPENSE, null);
        UpdateCategoryRequest request = new UpdateCategoryRequest("New Name", null, null);
        performPatch("/api/v1/categories/" + defaultCategory.getId(), userAPrincipal, request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void updateCategory_OtherUsersCategory_Returns403() throws Exception {
        Category category = seedCategory("User B Custom", TransactionType.EXPENSE, userB);
        UpdateCategoryRequest request = new UpdateCategoryRequest("New Name", null, null);
        performPatch("/api/v1/categories/" + category.getId(), userAPrincipal, request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void updateCategory_UnAuthorized_Returns401() throws Exception {
        Category defaultCategory = seedCategory("Food", TransactionType.EXPENSE, null);
        UpdateCategoryRequest request = new UpdateCategoryRequest("New Name", null, null);
        performPatch("/api/v1/categories/" + defaultCategory.getId(), null, request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }


    // - DELETE

    @Test
    void deleteCategory_OwnCategory_Returns200() throws Exception {
        Category category = seedCategory("Own Category", TransactionType.EXPENSE, userA);

        performDelete("/api/v1/categories/" + category.getId(), userAPrincipal, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted"));

        entityManager.flush();
        entityManager.clear();

        assertFalse(categoryRepository.existsById(category.getId()));
    }

    @Test
    void deleteCategory_NonExistentCategory_Returns404() throws Exception {
        performDelete("/api/v1/categories/" + UUID.randomUUID(), userAPrincipal, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void deleteCategory_DefaultCategory_Returns403() throws Exception {
        Category defaultCategory = seedCategory("Default Category name", TransactionType.INCOME, null);
        performDelete("/api/v1/categories/" + defaultCategory.getId(), userAPrincipal, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void deleteCategory_OtherUsersCategory_Returns403() throws Exception {
        Category userBCategory = seedCategory("User B Category", TransactionType.EXPENSE, userB);

        performDelete("/api/v1/categories/" + userBCategory.getId(), userAPrincipal, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void deleteCategory_InUse_Returns409() throws Exception {
        Category category = seedCategory("Rent", TransactionType.EXPENSE, userA);
        seedTransaction(new BigDecimal("1200.0"), LocalDate.now(), userA, category);

        performDelete("/api/v1/categories/" + category.getId(), userAPrincipal, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void deleteCategory_UnAuthorized_Returns401() throws Exception {
        Category category = seedCategory("Rent", TransactionType.INCOME, userA);

        performDelete("/api/v1/categories/" + category.getId(), null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

}

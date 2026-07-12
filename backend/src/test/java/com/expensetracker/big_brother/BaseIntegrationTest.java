package com.expensetracker.big_brother;

import com.expensetracker.big_brother.budget.Budget;
import com.expensetracker.big_brother.budget.BudgetRepository;
import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.category.CategoryRepository;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.security.CustomUserDetails;
import com.expensetracker.big_brother.transaction.Transaction;
import com.expensetracker.big_brother.transaction.TransactionRepository;
import com.expensetracker.big_brother.user.Role;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected CategoryRepository categoryRepository;
    @Autowired
    protected TransactionRepository transactionRepository;
    @Autowired
    protected BudgetRepository budgetRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    // HTTP Request helper methods
    protected ResultActions performGet(String url, CustomUserDetails principal) throws Exception {
        var requestBuilder = get(url).contentType(MediaType.APPLICATION_JSON);
        if (principal != null) {
            requestBuilder.with(user(principal));
        }
        return mockMvc.perform(requestBuilder);
    }

    protected ResultActions performPost(String url, CustomUserDetails principal, Object body) throws Exception {
        var requestBuilder = post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (principal != null) {
            requestBuilder.with(user(principal));
        }
        return mockMvc.perform(requestBuilder);
    }

    protected ResultActions performPatch(String url, CustomUserDetails principal, Object body) throws Exception {
        var requestBuilder = patch(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (principal != null) {
            requestBuilder.with(user(principal));
        }
        return mockMvc.perform(requestBuilder);
    }

    protected ResultActions performDelete(String url, CustomUserDetails principal, Object request) throws Exception {
        var requestBuilder = delete(url).contentType(MediaType.APPLICATION_JSON);
        if (principal != null) {
            requestBuilder.with(user(principal));
        }
        if (request != null ) requestBuilder.content(objectMapper.writeValueAsString(request));
        return mockMvc.perform(requestBuilder);
    }

    // Entity seeding / Builder helpers

    protected User seedUser(String email, String name) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("hashed"));
        u.setRole(Role.USER);
        u.setUserVerified(true);
        return userRepository.save(u);
    }

    protected Category seedCategory(String name, TransactionType type, User user) {
        Category c = new Category();
        c.setName(name);
        c.setType(type);
        c.setColor("#FF0000");
        c.setIcon("folder");
        c.setUser(user);
        return categoryRepository.save(c);
    }

    protected Transaction seedTransaction(BigDecimal amount, LocalDate date, User user, Category category) {
        Transaction t = new Transaction();
        t.setType(category.getType());
        t.setAmount(amount);
        t.setTransactionDate(date);
        t.setUser(user);
        t.setCategory(category);
        return transactionRepository.save(t);
    }

    protected Budget seedBudget(User user, Category category, String month, BigDecimal limitAmount) {
        Budget b = new Budget();
        b.setMonth(month);
        b.setLimitAmount(limitAmount);
        b.setUser(user);
        b.setCategory(category);
        return budgetRepository.save(b);
    }

    protected void clearDatabase() {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }
}

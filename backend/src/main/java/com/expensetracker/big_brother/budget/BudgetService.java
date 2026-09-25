package com.expensetracker.big_brother.budget;

import com.expensetracker.big_brother.budget.dto.BudgetRequest;
import com.expensetracker.big_brother.budget.dto.BudgetResponse;
import com.expensetracker.big_brother.budget.dto.UpdateBudgetRequest;
import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.category.CategoryRepository;
import com.expensetracker.big_brother.common.PageResponse;
import com.expensetracker.big_brother.common.validation.OwnershipValidator;
import com.expensetracker.big_brother.exception.DuplicateResourceException;
import com.expensetracker.big_brother.exception.ResourceNotFoundException;
import com.expensetracker.big_brother.exception.ResourceOwnershipException;
import com.expensetracker.big_brother.report.ReportService;
import com.expensetracker.big_brother.report.dto.projection.CategoryExpenses;
import com.expensetracker.big_brother.transaction.TransactionRepository;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetMapper budgetMapper;
    private final OwnershipValidator ownershipValidator;
    private final UserRepository userRepository;
    private final ReportService reportService;

    // list all budgets in a month
    @Transactional(readOnly = true)
    public PageResponse<BudgetResponse> getBudgets(UUID userId, String month, Pageable pageable) {
        try{
            YearMonth.parse(month);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid date format. Use yyyy-MM");
        }
        Page<Budget> budgets = budgetRepository.findAllByUserIdAndMonth(userId, month, pageable);

        List<CategoryExpenses> expenses = transactionRepository.expensesByCategory(userId, month);
        Map<UUID, BigDecimal> spendingMap = expenses.stream()
                .collect(Collectors.toMap(CategoryExpenses::categoryId, CategoryExpenses::amount));

        Page<BudgetResponse> responsePage = budgets.map(budget -> {
            BigDecimal spent = spendingMap.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
            return budgetMapper.toResponseWithCalculations(budget, spent);
        });
        return PageResponse.from(responsePage);
    }

    // create new budget
    @Transactional
    public BudgetResponse createBudget(BudgetRequest request, UUID userId) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        if (category.getUser() != null && !category.getUser().getId().equals(userId)) {
            throw new ResourceOwnershipException();
        }
        boolean exists = budgetRepository.existsByUserIdAndCategoryIdAndMonth(
                userId, category.getId(), request.month());
        if (exists) {
            throw new DuplicateResourceException("A budget for this month already exists for " + request.month());
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Budget budget = new Budget();
        budget.setMonth(request.month());
        budget.setLimitAmount(request.limitAmount());
        budget.setCategory(category);
        budget.setUser(user);
        Budget saved;
        try {
            saved = budgetRepository.save(budget);
        } catch (DataIntegrityViolationException e){
            log.warn("Concurrent budget creation detected for user={}, category={}, month={}", userId, category.getId(), request.month());
            throw new DuplicateResourceException("A budget for this category and month already exists");
        }
        YearMonth yearMonth = YearMonth.parse(request.month());
        BigDecimal spent = transactionRepository.sumExpensesByUserAndCategoryAndDateRange(
                userId, category.getId(), yearMonth.atDay(1), yearMonth.atEndOfMonth());
        return budgetMapper.toResponseWithCalculations(saved, spent);
    }

    @Transactional
    public BudgetResponse updateBudget(UUID budgetId, UpdateBudgetRequest request, UUID userId) {

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", budgetId));
        ownershipValidator.validateOwnership(budget.getUser().getId(), userId);

        if (request.categoryId() != null && !request.categoryId().equals(budget.getCategory().getId())) {
            Category newCategory = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
            if (newCategory.getUser() != null && !newCategory.getUser().getId().equals(userId)) throw new ResourceOwnershipException();
            budget.setCategory(newCategory);
        }
        if (request.limitAmount() != null) budget.setLimitAmount(request.limitAmount());
        if (request.month() != null) budget.setMonth(request.month());
        Budget saved = budgetRepository.save(budget);
        YearMonth yearMonth = YearMonth.parse(saved.getMonth());
        BigDecimal spent = transactionRepository
                .sumExpensesByUserAndCategoryAndDateRange(userId, saved.getCategory().getId(), yearMonth.atDay(1), yearMonth.atEndOfMonth());
        return budgetMapper.toResponseWithCalculations(saved, spent);
    }

    @Transactional
    public void deleteBudget(UUID userId, UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", budgetId));
        ownershipValidator.validateOwnership(budget.getUser().getId(), userId);
        budgetRepository.delete(budget);
    }

    @Transactional(readOnly = true)
    public Page<BudgetResponse> getDeletedBudgets(UUID userId, Pageable pageable) {
        Page<Budget> deletedBudgets = budgetRepository.findDeletedBudgets(userId, pageable);
        return deletedBudgets.map(budgetMapper::toResponse);
    }

    @Transactional
    public BudgetResponse restoreDeletedBudget(UUID userId, UUID id) {
        Budget deletedBudget = budgetRepository.findDeletedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deleted Budget", id));
        ownershipValidator.validateOwnership(deletedBudget.getUser().getId(), userId);
        deletedBudget.setDeletedAt(null);
        budgetRepository.save(deletedBudget);
        return budgetMapper.toResponse(deletedBudget);
    }
}

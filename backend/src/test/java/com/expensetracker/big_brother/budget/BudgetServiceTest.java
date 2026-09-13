package com.expensetracker.big_brother.budget;

import com.expensetracker.big_brother.budget.dto.BudgetRequest;
import com.expensetracker.big_brother.budget.dto.BudgetResponse;
import com.expensetracker.big_brother.budget.dto.UpdateBudgetRequest;
import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.category.CategoryRepository;
import com.expensetracker.big_brother.category.dto.CategoryResponse;
import com.expensetracker.big_brother.common.PageResponse;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.common.validation.OwnershipValidator;
import com.expensetracker.big_brother.exception.DuplicateResourceException;
import com.expensetracker.big_brother.exception.ResourceNotFoundException;
import com.expensetracker.big_brother.exception.ResourceOwnershipException;
import com.expensetracker.big_brother.transaction.TransactionRepository;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BudgetServiceTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID budgetId = UUID.randomUUID();
    private final String month = YearMonth.now().toString();
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BudgetMapper budgetMapper;
    @Mock
    private OwnershipValidator ownershipValidator;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private BudgetService budgetService;

    private User aUser() {
        User u = new User();
        u.setId(userId);
        return u;
    }

    private Category aCategory() {
        Category c = new Category();
        c.setId(categoryId);
        c.setName("Food");
        c.setType(TransactionType.EXPENSE);
        c.setUser(aUser());
        return c;
    }

    private CategoryResponse aCategoryResponse(Category c) {
        return new CategoryResponse(
                c.getId(), c.getName(), c.getType(), "#FFFFFF", "icon", c.getUser() == null, null);
    }

    private Budget aBudget() {
        Budget b = new Budget();
        b.setMonth(month);
        b.setLimitAmount(new BigDecimal("150.0"));
        b.setCategory(aCategory());
        b.setUser(aUser());
        return b;
    }

    private BudgetResponse aBudgetResponse(Budget b, BigDecimal spent) {
        BigDecimal remaining = b.getLimitAmount().subtract(spent);
        double percent = spent.doubleValue() / b.getLimitAmount().doubleValue() * 100.0;
        return new BudgetResponse(
                b.getId(), aCategoryResponse(b.getCategory()), b.getMonth(), b.getLimitAmount(), spent, remaining, Math.round(percent * 100.0) / 100.0, null);
    }

    private BudgetRequest aBudgetRequest() {
        return new BudgetRequest(categoryId, month, new BigDecimal("500.0"));
    }

    private UpdateBudgetRequest anUpdateBudgetRequest() {
        return new UpdateBudgetRequest(null, null, new BigDecimal("600.00"));
    }

    @Test
    void getBudgets_Success() {
        Budget budget = aBudget();
        BigDecimal spent = new BigDecimal("50.00");
        BudgetResponse response = aBudgetResponse(budget, spent);
        Page<Budget> page = new PageImpl<>(List.of(budget));

        when(budgetRepository.findAllByUserIdAndMonth(eq(userId), eq(month), any(Pageable.class))).thenReturn(page);

        when(transactionRepository.sumExpensesByUserAndCategoryAndDateRange(
                userId, categoryId, YearMonth.now().atDay(1), YearMonth.now().atEndOfMonth()))
                .thenReturn(spent);
        when(budgetMapper.toResponseWithCalculations(budget, spent)).thenReturn(response);

        PageResponse<BudgetResponse> result = budgetService.getBudgets(userId, month, Pageable.ofSize(20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().spentAmount()).isEqualByComparingTo(spent);

        verify(budgetRepository).findAllByUserIdAndMonth(eq(userId), eq(month), any(Pageable.class));
        verify(transactionRepository).sumExpensesByUserAndCategoryAndDateRange(userId, categoryId, YearMonth.now().atDay(1), YearMonth.now().atEndOfMonth());
    }

    @Test
    void getBudgets_NoBudgets() {
        when(budgetRepository.findAllByUserIdAndMonth(eq(userId), eq(month), any(Pageable.class))).thenReturn(Page.empty());

        PageResponse<BudgetResponse> result = budgetService.getBudgets(userId, month, Pageable.ofSize(20));

        assertThat(result.content()).isEmpty();
        verify(transactionRepository, never()).sumExpensesByUserAndCategoryAndDateRange(any(), any(), any(), any());
    }

    @Test
    void getBudgets_invalidMonth_throwsException() {
        assertThatThrownBy(() -> budgetService.getBudgets(userId, "abc", Pageable.ofSize(20)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createBudget_Success() {
        Budget budget = aBudget();
        BudgetRequest request = aBudgetRequest();
        Category category = aCategory();
        BigDecimal spent = new BigDecimal("0");
        BudgetResponse response = aBudgetResponse(budget, spent);

        when(categoryRepository.findById(request.categoryId())).thenReturn(Optional.of(category));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonth(userId, categoryId, month)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(aUser()));
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);
        when(transactionRepository.sumExpensesByUserAndCategoryAndDateRange(userId, categoryId, YearMonth.now().atDay(1), YearMonth.now().atEndOfMonth())).thenReturn(spent);
        when(budgetMapper.toResponseWithCalculations(budget, spent)).thenReturn(response);

        BudgetResponse result = budgetService.createBudget(request, userId);

        assertThat(result).isNotNull();
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void createBudget_CategoryNotFound_ThrowsException() {
        BudgetRequest request = aBudgetRequest();
        when(categoryRepository.findById(request.categoryId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> budgetService.createBudget(request, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void createBudget_CategoryOwnershipViolation_ThrowsException() {
        Category category = aCategory();
        UUID userBId = UUID.randomUUID();
        User userB = aUser();
        userB.setId(userBId);
        category.setUser(userB);
        BudgetRequest request = aBudgetRequest();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        assertThatThrownBy(() -> budgetService.createBudget(request, userId))
                .isInstanceOf(ResourceOwnershipException.class);
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void createBudget_DuplicateMonth_ThrowsException() {
        Category category = aCategory();
        Budget budget = aBudget();
        BudgetRequest request = aBudgetRequest();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonth(userId, categoryId, month)).thenReturn(true);
        assertThatThrownBy(() -> budgetService.createBudget(request, userId))
                .isInstanceOf(DuplicateResourceException.class);
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void createBudget_UserNotFound_ThrowsException() {
        Category category = aCategory();
        UUID userId = UUID.randomUUID();
        category.getUser().setId(userId);
        Budget budget = aBudget();
        BudgetRequest request = aBudgetRequest();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonth(userId, categoryId, month)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.createBudget(request, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void createBudget_ConcurrentInsert_ThrowsDuplicateException() {
        Category category = aCategory();
        BudgetRequest request = aBudgetRequest();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(budgetRepository.existsByUserIdAndCategoryIdAndMonth(userId, categoryId, month)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(aUser()));
        when(budgetRepository.save(any(Budget.class))).thenThrow(new DataIntegrityViolationException("dup key"));

        assertThatThrownBy(() -> budgetService.createBudget(request, userId))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateBudget_LimitOnly_Success() {
        Budget budget = aBudget();
        budget.setLimitAmount(new BigDecimal("600.0"));
        UpdateBudgetRequest request = anUpdateBudgetRequest();
        BudgetResponse response = aBudgetResponse(budget, new BigDecimal("0.0"));
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(budget)).thenReturn(budget);
        when(transactionRepository.sumExpensesByUserAndCategoryAndDateRange(userId, categoryId, YearMonth.now().atDay(1), YearMonth.now().atEndOfMonth()))
                .thenReturn(new BigDecimal("0.0"));
        when(budgetMapper.toResponseWithCalculations(budget, new BigDecimal("0.0"))).thenReturn(response);
        BudgetResponse result = budgetService.updateBudget(budgetId, request, userId);
        assertThat(result).isNotNull();
        assertThat(result.limitAmount()).isEqualByComparingTo(new BigDecimal("600.0"));
    }

    @Test
    void updateBudget_ChangeCategory_Success() {
        Budget budget = aBudget();

        UUID newCategoryId = UUID.randomUUID();
        Category newCategory = aCategory();
        newCategory.setId(newCategoryId);
        newCategory.setName("New Category");

        UpdateBudgetRequest request = new UpdateBudgetRequest(newCategoryId, null, null);

        CategoryResponse catResponse = aCategoryResponse(newCategory);
        BigDecimal spent = BigDecimal.ZERO;
        BudgetResponse response = new BudgetResponse(
                budgetId, catResponse, month, budget.getLimitAmount(), spent, budget.getLimitAmount(), 0.0, null);
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(newCategory));
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);
        when(transactionRepository.sumExpensesByUserAndCategoryAndDateRange(userId, newCategoryId, YearMonth.now().atDay(1), YearMonth.now().atEndOfMonth())).thenReturn(spent);
        when(budgetMapper.toResponseWithCalculations(budget, spent)).thenReturn(response);

        BudgetResponse result = budgetService.updateBudget(budgetId, request, userId);

        assertThat(result).isNotNull();

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(captor.capture());
        Budget saved = captor.getValue();
        assertThat(saved.getCategory().getId()).isEqualTo(newCategoryId);
    }

    @Test
    void updateBudget_NotFound_ThrowsException() {
        UpdateBudgetRequest request = anUpdateBudgetRequest();
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> budgetService.updateBudget(budgetId, request, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void updateBudget_OwnershipViolation_ThrowsException() {
        Budget budget = aBudget();
        UUID userBId = UUID.randomUUID();
        UpdateBudgetRequest request = anUpdateBudgetRequest();
        Category category = aCategory();
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        doThrow(new ResourceOwnershipException()).when(ownershipValidator).validateOwnership(budget.getUser().getId(), userBId);
        assertThatThrownBy(() -> budgetService.updateBudget(budgetId, request, userBId))
                .isInstanceOf(ResourceOwnershipException.class);
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void updateBudget_NewCategoryNotFound_ThrowsException() {
        Budget budget = aBudget();
        UpdateBudgetRequest request = new UpdateBudgetRequest(UUID.randomUUID(), null, null);
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findById(request.categoryId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> budgetService.updateBudget(budgetId, request, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(budgetRepository, never()).save(any(Budget.class));

    }

    @Test
    void updateBudget_NewCategoryOwnershipViolation_ThrowsException() {
        Budget budget = aBudget();
        Category newCategory = aCategory();
        UUID userBId = UUID.randomUUID();
        User userB = aUser();
        userB.setId(userBId);
        UUID newCategoryId = UUID.randomUUID();
        newCategory.setId(newCategoryId);
        newCategory.setUser(userB);
        UpdateBudgetRequest request = new UpdateBudgetRequest(newCategoryId, null, null);
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(newCategory));
        doNothing().when(ownershipValidator).validateOwnership(userId, userId);
        assertThatThrownBy(() -> budgetService.updateBudget(budgetId, request, userId))
                .isInstanceOf(ResourceOwnershipException.class);
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void deleteBudget_Success() {
        Budget budget = aBudget();
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        budgetService.deleteBudget(userId, budgetId);
        verify(budgetRepository).delete(budget);
    }

    @Test
    void deleteBudget_NotFound_ThrowsException() {
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> budgetService.deleteBudget(userId, budgetId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(budgetRepository, never()).delete(any(Budget.class));
    }

    @Test
    void deleteBudget_OwnershipViolation_ThrowsException() {
        Budget budget = aBudget();
        UUID userBId = UUID.randomUUID();
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        doThrow(new ResourceOwnershipException()).when(ownershipValidator).validateOwnership(budget.getUser().getId(), userBId);
        assertThatThrownBy(() -> budgetService.deleteBudget(userBId, budgetId))
                .isInstanceOf(ResourceOwnershipException.class);
        verify(budgetRepository, never()).delete(budget);
    }
}

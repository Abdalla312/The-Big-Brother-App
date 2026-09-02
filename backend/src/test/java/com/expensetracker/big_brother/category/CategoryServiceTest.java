package com.expensetracker.big_brother.category;

import com.expensetracker.big_brother.category.dto.CategoryResponse;
import com.expensetracker.big_brother.category.dto.CreateCategoryRequest;
import com.expensetracker.big_brother.category.dto.UpdateCategoryRequest;
import com.expensetracker.big_brother.common.PageResponse;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.common.validation.OwnershipValidator;
import com.expensetracker.big_brother.exception.CategoryInUseException;
import com.expensetracker.big_brother.exception.ResourceNotFoundException;
import com.expensetracker.big_brother.exception.ResourceOwnershipException;
import com.expensetracker.big_brother.recurring.RecurringTransactionRepository;
import com.expensetracker.big_brother.transaction.TransactionRepository;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OwnershipValidator ownershipValidator;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;
    @InjectMocks
    private CategoryService categoryService;

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
        c.setColor("#FF0000");
        c.setIcon("utensils");
        c.setUser(aUser());
        return c;
    }

    private Category aSystemCategory() {
        Category c = new Category();
        c.setId(UUID.randomUUID());
        c.setName("Default");
        c.setType(TransactionType.EXPENSE);
        c.setUser(null);
        return c;
    }

    private CategoryResponse aCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getColor(),
                category.getIcon(),
                category.getUser() == null);
    }

    private CreateCategoryRequest aCreateRequest() {
        return new CreateCategoryRequest("Food", TransactionType.EXPENSE, "#FF0000", "utensils");
    }

    private UpdateCategoryRequest anUpdateRequest() {
        return new UpdateCategoryRequest("Groceries", "#00FF00", "shopping-cart");
    }

    // ----getAllCategories------
    @Test
    void getAllCategories_success() {
        Category cat1 = aCategory();
        Category cat2 = aSystemCategory();
        Page<Category> page = new PageImpl<>(List.of(cat1, cat2));
        when(categoryRepository.findAllByUserIdOrUserIsNull(eq(userId), any(Pageable.class))).thenReturn(page);
        when(categoryMapper.toResponse(cat1)).thenReturn(aCategoryResponse(cat1));
        when(categoryMapper.toResponse(cat2)).thenReturn(aCategoryResponse(cat2));

        PageResponse<CategoryResponse> result = categoryService.getAllCategories(userId, "all", Pageable.ofSize(20));

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        verify(categoryRepository).findAllByUserIdOrUserIsNull(eq(userId) , any(Pageable.class));
    }

    @Test
    void getAllUserCategories_success() {
        Category cat1 = aCategory();
        Category cat2 = aSystemCategory();
        Page<Category> page = new PageImpl<>(List.of(cat1));
        when(categoryRepository.findAllByUserId(eq(userId), any(Pageable.class))).thenReturn(page);
        when(categoryMapper.toResponse(cat1)).thenReturn(aCategoryResponse(cat1));

        PageResponse<CategoryResponse> result = categoryService.getAllCategories(userId, "user", Pageable.ofSize(20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        verify(categoryRepository).findAllByUserId(eq(userId) , any(Pageable.class));
    }

    @Test
    void getAllDefaultCategories_success() {
        Category cat1 = aCategory();
        Category cat2 = aSystemCategory();
        Page<Category> page = new PageImpl<>(List.of(cat2));
        when(categoryRepository.findAllByUserIdIsNull(any(Pageable.class))).thenReturn(page);
        when(categoryMapper.toResponse(cat2)).thenReturn(aCategoryResponse(cat2));

        PageResponse<CategoryResponse> result = categoryService.getAllCategories(userId, "default", Pageable.ofSize(20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        verify(categoryRepository).findAllByUserIdIsNull(any(Pageable.class));
    }

    // --- createCategory -------
    @Test
    void createCategory_success() {
        CreateCategoryRequest request = aCreateRequest();
        Category entity = aCategory();
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(userRepository.findById(userId)).thenReturn(Optional.of(aUser()));
        when(categoryRepository.save(entity)).thenReturn(entity);
        when(categoryMapper.toResponse(entity)).thenReturn(aCategoryResponse(entity));

        CategoryResponse result = categoryService.createCategory(request, userId);
        assertThat(result.name()).isEqualTo("Food");
        verify(categoryRepository).save(entity);
    }

    @Test
    void createCategory_UserNotFound_ThrowsException() {
        CreateCategoryRequest request = aCreateRequest();
        when(categoryMapper.toEntity(request)).thenReturn(aCategory());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createCategory(request, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(categoryRepository, never()).save(any());
    }

    // ---- updateCategory -------
    @Test
    void updateCategory_success() {
        Category category = aCategory();
        UpdateCategoryRequest request = anUpdateRequest();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        doNothing().when(ownershipValidator).validateOwnership(userId, userId);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(aCategoryResponse(category));

        CategoryResponse result = categoryService.updateCategory(categoryId, request, userId);

        assertThat(result).isNotNull();
        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategory_notFound_throwsException() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, anUpdateRequest(), userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCategory_systemDefault_throwsException() {
        Category system = aSystemCategory();
        UpdateCategoryRequest request = anUpdateRequest();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(system));

        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, request, userId))
                .isInstanceOf(ResourceOwnershipException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategory_notOwner_throwsException() {
        Category category = aCategory();
        UUID otherId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        doThrow(new ResourceOwnershipException()).when(ownershipValidator).validateOwnership(userId, otherId);
        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, anUpdateRequest(), otherId))
                .isInstanceOf(ResourceOwnershipException.class);
        verify(categoryRepository, never()).save(any());
    }

    // ---- deleteCategory ------
    @Test
    void deleteCategory_success() {
        Category category = aCategory();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryId(categoryId)).thenReturn(false);
        when(recurringTransactionRepository.existsByCategoryId(categoryId)).thenReturn(false);

        categoryService.deleteCategory(categoryId, userId);
        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_notFound_throwsException() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_notOwner_throwsException() {
        Category category = aCategory();
        UUID otherId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        doThrow(new ResourceOwnershipException()).when(ownershipValidator)
                .validateOwnership(userId, otherId);
        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, otherId))
                .isInstanceOf(ResourceOwnershipException.class);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_systemDefault_throwsException() {
        Category system = aSystemCategory();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(system));
        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, userId))
                .isInstanceOf(ResourceOwnershipException.class);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_inUse_throwsException() {
        Category category = aCategory();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryId(categoryId)).thenReturn(true);
        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, userId))
                .isInstanceOf(CategoryInUseException.class);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_inUseByRecurring_throwsException() {
        Category category = aCategory();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryId(categoryId)).thenReturn(false);
        when(recurringTransactionRepository.existsByCategoryId(categoryId)).thenReturn(true);
        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, userId))
                .isInstanceOf(CategoryInUseException.class);
        verify(categoryRepository, never()).delete(any());
    }
}

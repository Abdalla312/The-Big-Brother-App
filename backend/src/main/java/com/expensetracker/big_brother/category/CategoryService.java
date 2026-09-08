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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
    private final CategoryMapper categoryMapper;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final OwnershipValidator ownershipValidator;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;

    public PageResponse<CategoryResponse> getAllCategories(UUID userId, TransactionType type, boolean defaultCategories, Pageable pageable) {
        Page<Category> categories = defaultCategories
                ? categoryRepository.findDefaultCategories(type, pageable)
                : categoryRepository.findUserCategories(userId, type, pageable);
        return PageResponse.from(categories.map(categoryMapper::toResponse));
    }

    public CategoryResponse createCategory(@Valid CreateCategoryRequest request, UUID userId) {
        Category category = categoryMapper.toEntity(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        category.setUser(user);
        categoryRepository.save(category);
        log.info("New category created for userId: {}", userId);
        return categoryMapper.toResponse(category);
    }

    public CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request, UUID currentUserId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (category.getUser() == null) {
            throw new ResourceOwnershipException();
        }
        ownershipValidator.validateOwnership(category.getUser().getId(), currentUserId);
        if (request.name() != null) category.setName(request.name());

        if (request.color() != null) category.setColor(request.color());

        if (request.icon() != null) category.setIcon(request.icon());

        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    public void deleteCategory(UUID categoryId, UUID userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category Not found"));
        if (category.getUser() == null) {
            throw new ResourceOwnershipException();
        }
        ownershipValidator.validateOwnership(category.getUser().getId(), userId);
        if (transactionRepository.existsByCategoryId(categoryId) || recurringTransactionRepository.existsByCategoryId(categoryId)){
            throw new CategoryInUseException();
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getDeletedCategories(UUID userId, Pageable pageable) {
        Page<Category> deletedCategories = categoryRepository.findDeletedCategories(userId, pageable);
        return deletedCategories.map(categoryMapper::toResponse);
    }

    @Transactional
    public CategoryResponse restoreDeletedCategory(UUID userId, UUID id) {
        Category category = categoryRepository.findDeletedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        if (category.getUser() == null) throw new ResourceOwnershipException();
        ownershipValidator.validateOwnership(category.getUser().getId(), userId);
        category.setDeletedAt(null);
        categoryRepository.save(category);
        return categoryMapper.toResponse(category);
    }
}

package com.expensetracker.big_brother.category;

import com.expensetracker.big_brother.category.dto.CategoryResponse;
import com.expensetracker.big_brother.category.dto.CreateCategoryRequest;
import com.expensetracker.big_brother.category.dto.UpdateCategoryRequest;
import com.expensetracker.big_brother.common.validation.OwnershipValidator;
import com.expensetracker.big_brother.exception.CategoryInUseException;
import com.expensetracker.big_brother.exception.ResourceNotFoundException;
import com.expensetracker.big_brother.exception.ResourceOwnershipException;
import com.expensetracker.big_brother.transaction.TransactionRepository;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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

    // returns all categories visible to user
    // - system defaults userId is NULL
    // - user's own custom categories
    public List<CategoryResponse> getAllCategories(UUID userId) {
        List<Category> categories = categoryRepository.findAllByUserIdOrUserIsNull(userId);
        return categoryMapper.toResponseList(categories);
    }

    // creates a new custom category for the user
    // it's owned by the user other users cannot see it
    public CategoryResponse createCategory(@Valid CreateCategoryRequest request, UUID userId) {
        Category category = categoryMapper.toEntity(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        category.setUser(user);
        categoryRepository.save(category);
        log.info("New category created for userId: {}", userId);
        return categoryMapper.toResponse(category);
    }

    // updates existing category.
    // rules:
    // - category must exist (else 404)
    // - category must belong to the current user (else 403)
    // - system defaults (user_id  = NULL) cannot be edited (else 403)
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
    // deletes a category
    // must check if any transactions use this category before deleting.
    public void deleteCategory(UUID categoryId, UUID userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category Not found"));
        if (category.getUser() == null) {
            throw new ResourceOwnershipException();
        }
        ownershipValidator.validateOwnership(category.getUser().getId(), userId);
        if (transactionRepository.existsByCategoryId(categoryId)){
            throw new CategoryInUseException();
        }
        categoryRepository.delete(category);
    }

}

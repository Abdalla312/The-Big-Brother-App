package com.expensetracker.big_brother.recurring;

import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.category.CategoryRepository;
import com.expensetracker.big_brother.common.PageResponse;
import com.expensetracker.big_brother.common.validation.OwnershipValidator;
import com.expensetracker.big_brother.exception.ResourceNotFoundException;
import com.expensetracker.big_brother.exception.ResourceOwnershipException;
import com.expensetracker.big_brother.recurring.dto.CreateRecurringTransactionRequest;
import com.expensetracker.big_brother.recurring.dto.RecurringTransactionResponse;
import com.expensetracker.big_brother.recurring.dto.UpdateRecurringTransactionRequest;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
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
public class RecurringTransactionService {
    private final RecurringTransactionMapper recurringTransactionMapper;

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final OwnershipValidator ownershipValidator;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // CRUD Operations
    @Transactional(readOnly = true)
    public PageResponse<RecurringTransactionResponse> getAllRecurringTransactions(UUID userId, Pageable pageable) {
        Page<RecurringTransaction> recurringTransactionPage = recurringTransactionRepository.findAllByUserId(userId, pageable);

        return PageResponse.from(recurringTransactionPage.map(recurringTransactionMapper::toDto));
    }

    @Transactional(readOnly = true)
    public RecurringTransactionResponse getRecurringTransaction(UUID userId, UUID id) {
        RecurringTransaction recurringTransaction = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction", id));
        ownershipValidator.validateOwnership(recurringTransaction.getUser().getId(), userId);

        return recurringTransactionMapper.toDto(recurringTransaction);
    }

    @Transactional
    public RecurringTransactionResponse createRecurringTransaction(CreateRecurringTransactionRequest request, UUID userId) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        if (category.getUser() != null && !category.getUser().getId().equals(userId))
            throw new ResourceOwnershipException();

        if (category.getType() != request.type())
            throw new IllegalArgumentException("Transaction type doesn't match category type");


        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        RecurringTransaction recurringTransaction = recurringTransactionMapper.toEntity(request);
        recurringTransaction.setUser(user);
        recurringTransaction.setCategory(category);

        return recurringTransactionMapper.toDto(recurringTransactionRepository.save(recurringTransaction));
    }

    @Transactional
    public RecurringTransactionResponse updateRecurringTransaction(UUID id, UpdateRecurringTransactionRequest request, UUID userId) {
        RecurringTransaction recurringTransaction = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction", id));

        ownershipValidator.validateOwnership(recurringTransaction.getUser().getId(), userId);

        if (request.categoryId() != null && !request.categoryId().equals(recurringTransaction.getCategory().getId())) {
            Category newCategory = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

            if (newCategory.getUser() != null)
                ownershipValidator.validateOwnership(newCategory.getUser().getId(), userId);
            recurringTransaction.setCategory(newCategory);
        }
        RecurringTransaction updatedRecurringTransaction = recurringTransactionMapper.partialUpdate(request, recurringTransaction);

        return recurringTransactionMapper.toDto(recurringTransactionRepository.save(updatedRecurringTransaction));
    }

    @Transactional
    public RecurringTransactionResponse toggleActive(UUID id, UUID userId) {
        RecurringTransaction recurringTransaction = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction", id));

        ownershipValidator.validateOwnership(recurringTransaction.getUser().getId(), userId);

        recurringTransaction.setActive(!recurringTransaction.isActive());
        return recurringTransactionMapper.toDto(
                recurringTransactionRepository.save(recurringTransaction));
    }

    @Transactional
    public void deleteRecurringTransaction(UUID id, UUID userId) {
        RecurringTransaction recurringTransaction = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction", id));

        ownershipValidator.validateOwnership(recurringTransaction.getUser().getId(), userId);

        recurringTransactionRepository.delete(recurringTransaction);
    }

    @Transactional(readOnly = true)
    public Page<RecurringTransactionResponse> getDeletedRecurringTransactions(UUID userId, Pageable pageable) {
        Page<RecurringTransaction> deletedRules = recurringTransactionRepository.findDeletedRecurringTransactions(userId, pageable);
        return deletedRules.map(recurringTransactionMapper::toDto);
    }

    @Transactional
    public RecurringTransactionResponse restoreDeletedRule(UUID userId, UUID id) {
        RecurringTransaction deletedRule = recurringTransactionRepository.findDeletedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deleted recurring transaction", id));
        ownershipValidator.validateOwnership(deletedRule.getUser().getId(), userId);

        deletedRule.setDeletedAt(null);
        recurringTransactionRepository.save(deletedRule);
        return recurringTransactionMapper.toDto(deletedRule);
    }
}

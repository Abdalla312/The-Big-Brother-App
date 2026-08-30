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
import com.expensetracker.big_brother.transaction.Transaction;
import com.expensetracker.big_brother.transaction.TransactionRepository;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionService {
    private final RecurringTransactionMapper recurringTransactionMapper;

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final OwnershipValidator ownershipValidator;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    @Lazy @Autowired private RecurringTransactionService self;

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleRule(RecurringTransaction rule) {
        Transaction tx = new Transaction();
        tx.setUser(rule.getUser());
        tx.setCategory(rule.getCategory());
        tx.setType(rule.getType());
        tx.setAmount(rule.getAmount());
        tx.setTransactionDate(rule.getNextExecutionDate());
        tx.setPaymentMethod(rule.getPaymentMethod());
        tx.setNote("[Auto-recurring] " + (rule.getNote() != null ? rule.getNote() : ""));
        transactionRepository.save(tx);

        rule.setNextExecutionDate(calculateNextDate(rule.getNextExecutionDate(), rule.getFrequency()));
        recurringTransactionRepository.save(rule);
    }

    // Scheduler Processing Engine
    @Transactional
    public long processDueTransactions() {
        LocalDate today = LocalDate.now();
        Pageable batch = PageRequest.of(0, 100);
        Page<RecurringTransaction> page;
        long count = 0;
        do {
            page = recurringTransactionRepository.findDueBatch(today, batch);
            for (RecurringTransaction rule : page.getContent()) {
                try {
                    self.processSingleRule(rule);
                    count++;
                } catch (Exception e) {
                    log.error("Failed to process recurring rule id={}: {}", rule.getId(), e.getMessage());
                }
            }
        } while (page.hasNext());

        return count;
    }

    private LocalDate calculateNextDate(LocalDate currentDate, RecurrenceFrequency frequency) {
        return switch (frequency) {
            case DAILY -> currentDate.plusDays(1);
            case WEEKLY -> currentDate.plusWeeks(1);
            case MONTHLY -> currentDate.plusMonths(1);
            case YEARLY -> currentDate.plusYears(1);
        };
    }

}

package com.expensetracker.big_brother.transaction;

import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.category.CategoryRepository;
import com.expensetracker.big_brother.common.PageResponse;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.common.validation.OwnershipValidator;
import com.expensetracker.big_brother.exception.ResourceNotFoundException;
import com.expensetracker.big_brother.exception.ResourceOwnershipException;
import com.expensetracker.big_brother.transaction.dto.TransactionRequest;
import com.expensetracker.big_brother.transaction.dto.TransactionResponse;
import com.expensetracker.big_brother.transaction.dto.UpdateTransactionRequest;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;
    private final OwnershipValidator ownershipValidator;
    private final UserRepository userRepository;

    // list all authed user's transactions
    @Transactional
    public PageResponse<TransactionResponse> getTransactions(
            UUID userId, String month, UUID categoryId,
            TransactionType type, int page, int size) {
        List<Specification<Transaction>> specs = new ArrayList<>();
        specs.add(TransactionSpecification.belongsToUser(userId));
        if (month != null) specs.add(TransactionSpecification.inMonth(month));
        if (categoryId != null) specs.add(TransactionSpecification.hasCategory(categoryId));
        if (type != null) specs.add(TransactionSpecification.hasType(type));

        Specification<Transaction> finalSpec = Specification.allOf(specs);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
        Page<Transaction> result = transactionRepository.findAll(finalSpec, pageable);

        Page<TransactionResponse> responsePage = result.map(transactionMapper::toResponse);
        return PageResponse.from(responsePage);
    }
    // get certain user's transaction
    @Transactional
    public TransactionResponse getTransaction(UUID transactionId, UUID userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));
        ownershipValidator.validateOwnership(transaction.getUser().getId(), userId);
        return transactionMapper.toResponse(transaction);
    }
    // create new transaction assigned to user
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, UUID currentUserId) {
        // get category of the transaction
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        // check ownership of the transaction to user or global category

        if (category.getUser() != null && !category.getUser().getId().equals(currentUserId)) {
            throw new ResourceOwnershipException();
        }

        Transaction newTransaction = transactionMapper.toEntity(request);
        newTransaction.setCategory(category);
        User user = userRepository.getReferenceById(currentUserId);
        newTransaction.setUser(user);

        Transaction saved = transactionRepository.save(newTransaction);
        return transactionMapper.toResponse(saved);
    }
    // update transaction assigned to user
    @Transactional
    public TransactionResponse updateTransaction(
            UUID transactionId, UpdateTransactionRequest request, UUID currentUserId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));
        ownershipValidator.validateOwnership(transaction.getUser().getId(), currentUserId);
        if (request.type() != null) transaction.setType(request.type());
        if (request.amount() != null) transaction.setAmount(request.amount());
        if (request.transactionDate() != null) transaction.setTransactionDate(request.transactionDate());
        if (request.note() != null) transaction.setNote(request.note());
        if (request.paymentMethod() != null) transaction.setPaymentMethod(request.paymentMethod());
        // category update with ownership check
        if (request.categoryId() != null && !transaction.getCategory().getId().equals(request.categoryId())) {
            Category newCategory = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
            if (newCategory.getUser() != null && !newCategory.getUser().getId().equals(currentUserId)) {
                throw new ResourceOwnershipException();
            }
            transaction.setCategory(newCategory);
        }
        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }
    // delete certain transaction assigned to user
    public void deleteTransaction(UUID transactionId, UUID currentUserId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));
        ownershipValidator.validateOwnership(transaction.getUser().getId(), currentUserId);
        transactionRepository.delete(transaction);
    }
}

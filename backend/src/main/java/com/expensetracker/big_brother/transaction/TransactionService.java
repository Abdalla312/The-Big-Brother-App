package com.expensetracker.big_brother.transaction;

import com.expensetracker.big_brother.category.CategoryRepository;
import com.expensetracker.big_brother.common.PageResponse;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.common.validation.OwnershipValidator;
import com.expensetracker.big_brother.transaction.dto.TransactionResponse;
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

    // list all authed user's transactions
    public PageResponse<TransactionResponse> getTransactions(UUID userId, String month, UUID categoryId,
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
    // create new transaction assigned to user
    // update transaction assigned to user
    // delete certain transaction assigned to user
}

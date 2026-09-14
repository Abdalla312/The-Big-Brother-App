package com.expensetracker.big_brother.transaction;

import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.category.CategoryRepository;
import com.expensetracker.big_brother.common.PageResponse;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.common.validation.OwnershipValidator;
import com.expensetracker.big_brother.exception.ResourceNotFoundException;
import com.expensetracker.big_brother.exception.ResourceOwnershipException;
import com.expensetracker.big_brother.transaction.dto.TransactionExportFilter;
import com.expensetracker.big_brother.transaction.dto.TransactionRequest;
import com.expensetracker.big_brother.transaction.dto.TransactionResponse;
import com.expensetracker.big_brother.transaction.dto.UpdateTransactionRequest;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
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
    private final JdbcTemplate jdbcTemplate;

    // list all authed user's transactions
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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

        if (!(category.getType().toString()).equals(request.type().toString()))
            throw new IllegalArgumentException("Transaction type mismatch with category");

        Transaction newTransaction = transactionMapper.toEntity(request);
        newTransaction.setPaymentMethod(normalizePaymentMethod(request.paymentMethod()));
        newTransaction.setCategory(category);
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));
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

        if (request.categoryId() != null && !transaction.getCategory().getId().equals(request.categoryId())) {
            Category newCategory = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
            if (newCategory.getUser() != null && !newCategory.getUser().getId().equals(currentUserId)) {
                throw new ResourceOwnershipException();
            }
            transaction.setCategory(newCategory);
        }
        return transactionMapper.toResponse(
                transactionRepository.save(
                        transactionMapper.partialUpdate(request, transaction)));
    }

    @Transactional
    public void deleteTransaction(UUID transactionId, UUID currentUserId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));
        ownershipValidator.validateOwnership(transaction.getUser().getId(), currentUserId);
        transactionRepository.delete(transaction);
    }

    private String normalizePaymentMethod(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        StringBuilder result = new StringBuilder();
        for (String word : trimmed.split("\\s+")) {
            if (!result.isEmpty()) result.append(" ");
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }

    @Transactional(readOnly = true)
    public void exportTransactionsCsv(UUID userId, TransactionExportFilter filter, OutputStream out) throws IOException {

        SqlQuery query = buildExportQuery(userId, filter);

        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        writer.write('\uFEFF');
        try (CSVPrinter csvPrinter = new CSVPrinter(writer,
                CSVFormat.DEFAULT.builder().setHeader("Date", "Type", "Category", "Amount", "Payment Method", "Note").get())) {


            jdbcTemplate.query(
                    connection -> {
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        query.sql(),
                                        ResultSet.TYPE_FORWARD_ONLY,
                                        ResultSet.CONCUR_READ_ONLY);
                        for (int i = 0; i < query.parameters.size(); i++) {
                            statement.setObject(i + 1, query.parameters.get(i));
                        }
                        statement.setFetchSize(1000);
                        return statement;
                    },
                    resultSet -> {
                        try {
                            csvPrinter.printRecord(
                                    resultSet.getObject("transaction_date", LocalDate.class),
                                    resultSet.getString("type"),
                                    resultSet.getString("category_name"),
                                    resultSet.getBigDecimal("amount"),
                                    resultSet.getString("payment_method"),
                                    resultSet.getString("note")
                            );
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
            );
            csvPrinter.flush();
        }
    }

    SqlQuery buildExportQuery(UUID userId, TransactionExportFilter filter) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    t.transaction_date,
                    t.type,
                    c.name AS category_name,
                    t.amount,
                    t.payment_method,
                    t.note
                FROM transactions t
                LEFT JOIN categories c
                    ON c.id = t.category_id
                WHERE t.user_id = ?
                """);

        List<Object> params = new ArrayList<>();

        params.add(userId);

        if (filter.from() != null) {
            sql.append(" AND t.transaction_date >= ?");
            params.add(filter.from());
        }

        if (filter.to() != null) {
            sql.append(" AND t.transaction_date <= ?");
            params.add(filter.to());
        }

        if (filter.type() != null) {
            sql.append(" AND t.type = ?");
            params.add(filter.type().name());
        }

        if (filter.categoryId() != null) {
            sql.append(" AND t.category_id = ?");
            params.add(filter.categoryId());
        }

        sql.append(" ORDER BY t.transaction_date DESC, t.id DESC");

        return new SqlQuery(sql.toString(),params);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsTrash(UUID userId, Pageable pageable) {
        Page<Transaction> resultPage = transactionRepository.findDeletedTransactions(userId, pageable);
        return resultPage.map(transactionMapper::toResponse);
    }

    @Transactional
    public TransactionResponse restoreTransaction(UUID userId, UUID id) {
        Transaction transaction = transactionRepository.findDeletedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deleted transaction", id));

        ownershipValidator.validateOwnership(transaction.getUser().getId(), userId);

        transaction.setDeletedAt(null);
        transactionRepository.save(transaction);
        return transactionMapper.toResponse(transaction);
    }

    record SqlQuery(
            String sql,
            List<Object> parameters
    ) {}
}

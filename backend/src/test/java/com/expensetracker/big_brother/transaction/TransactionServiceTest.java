package com.expensetracker.big_brother.transaction;

import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.category.CategoryRepository;
import com.expensetracker.big_brother.category.dto.CategoryResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID transactionId = UUID.randomUUID();
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private OwnershipValidator ownershipValidator;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private TransactionService transactionService;

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
                c.getId(), c.getName(), c.getType(), "#FFFFFF", "icon", c.getUser() == null);
    }


    private Transaction aTransaction() {
        Transaction t = new Transaction();
        t.setId(transactionId);
        t.setUser(aUser());
        t.setCategory(aCategory());
        t.setAmount(new BigDecimal("400.0"));
        t.setTransactionDate(LocalDate.now());
        return t;
    }

    private TransactionResponse aTransactionResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getType() != null ? t.getType() : TransactionType.EXPENSE,
                t.getAmount(),
                t.getTransactionDate(),
                t.getNote(),
                t.getPaymentMethod(),
                aCategoryResponse(t.getCategory()),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private TransactionRequest aTransactionRequest() {
        return new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("50.00"), LocalDate.now(), "Dinner", "Cash", categoryId);
    }

    @Test
    void createTransaction_success() {
        TransactionRequest request = aTransactionRequest();
        Category category = aCategory();
        Transaction transaction = aTransaction();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(userRepository.findById(userId)).thenReturn(Optional.of(aUser()));
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(aTransactionResponse(transaction));

        TransactionResponse response = transactionService.createTransaction(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.category().name()).isEqualTo("Food");

        verify(transactionRepository).save(transaction);
    }

    @Test
    void createTransaction_TransactionTypeMismatchWithCategory_ThrowsException() {
        TransactionRequest request = new TransactionRequest(TransactionType.INCOME, new BigDecimal("200.0"),LocalDate.now(), null, null, categoryId);
        Category category = aCategory();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        assertThatThrownBy(() -> transactionService.createTransaction(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction type mismatch with category");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_categoryNotFound_throwsException() {
        TransactionRequest request = aTransactionRequest();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> transactionService.createTransaction(request, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_OtherUsersCategory_throwsException() {
        TransactionRequest request = aTransactionRequest();
        Category userBCategory = aCategory();
        userBCategory.getUser().setId(UUID.randomUUID());

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(userBCategory));
        assertThatThrownBy(() -> transactionService.createTransaction(request, userId))
                .isInstanceOf(ResourceOwnershipException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void getTransactions_success() {
        Transaction transaction = aTransaction();
        TransactionResponse response = aTransactionResponse(transaction);
        PageRequest pageRequest = PageRequest.of(
                0, 10, Sort.Direction.DESC, "transactionDate");
        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction), pageRequest, 1);
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(transactionPage);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);
        PageResponse<TransactionResponse> result = transactionService.
                getTransactions(userId, YearMonth.now().toString(), null, null, 0, 10);

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().id()).isEqualTo(transaction.getId());
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.numberOfElements()).isEqualTo(1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);

        assertThat(Objects.requireNonNull(capturedPageable.getSort().getOrderFor("transactionDate")).getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getTransaction_success() {
        Transaction transaction = aTransaction();
        TransactionResponse response = aTransactionResponse(transaction);
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);
        TransactionResponse result = transactionService.getTransaction(transaction.getId(), userId);
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(transaction.getId());
        verify(transactionRepository).findById(transaction.getId());
    }

    @Test
    void getTransaction_NotFoundTransaction_throwsException() {
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> transactionService.getTransaction(transactionId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(transactionMapper, never()).toResponse(any());
    }

    @Test
    void getTransaction_OtherUsersTransaction_throwsException() {
        Transaction transaction = aTransaction();
        UUID userBId = UUID.randomUUID();
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        doThrow(new ResourceOwnershipException()).when(ownershipValidator).validateOwnership(userId, userBId);

        assertThatThrownBy(() -> transactionService.getTransaction(transactionId, userBId))
                .isInstanceOf(ResourceOwnershipException.class);

        verify(transactionMapper, never()).toResponse(any());
    }

    @Test
    void updateTransaction_Success() {
        Transaction transaction = aTransaction();
        UpdateTransactionRequest request = new UpdateTransactionRequest(TransactionType.INCOME, null, null, null, null, null);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(aTransactionResponse(transaction));

        TransactionResponse result = transactionService.updateTransaction(transactionId, request, userId);

        assertThat(result).isNotNull();
        verify(transactionRepository).save(transaction);
    }

    @Test
    void updateTransaction_ChangeCategory_Success() {
        Transaction transaction = aTransaction();
        Category newCategory = aCategory();
        newCategory.setId(UUID.randomUUID());

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                null, null, null, null, null, newCategory.getId());

        TransactionResponse response = aTransactionResponse(transaction);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(categoryRepository.findById(newCategory.getId())).thenReturn(Optional.of(newCategory));

        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(response);
        TransactionResponse result = transactionService.updateTransaction(transactionId, request, userId);
        assertThat(result).isNotNull();
        assertThat(transaction.getCategory().getId()).isEqualTo(newCategory.getId());
        verify(ownershipValidator).validateOwnership(userId, userId);
        verify(categoryRepository).findById(newCategory.getId());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void updateTransaction_TransactionNotFound_ThrowsException() {
        UpdateTransactionRequest request = new UpdateTransactionRequest(TransactionType.INCOME, null, null, null, null, null);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> transactionService.updateTransaction(transactionId, request, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_OtherUsersTransaction_ThrowsException() {
        User userB = aUser();
        userB.setId(UUID.randomUUID());
        Transaction transaction = aTransaction();
        transaction.setUser(userB);
        UpdateTransactionRequest request = new UpdateTransactionRequest(TransactionType.INCOME, null, null, null, null, null);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        doThrow(new ResourceOwnershipException()).when(ownershipValidator).validateOwnership(transaction.getUser().getId(), userId);
        assertThatThrownBy(() -> transactionService.updateTransaction(transactionId, request, userId))
                .isInstanceOf(ResourceOwnershipException.class);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_CategoryNotFound_ThrowsException() {
        Transaction transaction = aTransaction();
        UUID categoryId = UUID.randomUUID();
        UpdateTransactionRequest request = new UpdateTransactionRequest(null, null, null, null, null, categoryId);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> transactionService.updateTransaction(transactionId, request, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_OtherUsersCategory_ThrowsException() {
        Transaction transaction = aTransaction();

        UUID userBId = UUID.randomUUID();
        User userB = aUser();
        userB.setId(userBId);

        Category userBCategory = aCategory();
        userBCategory.setUser(userB);

        userBCategory.setId(UUID.randomUUID());

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                null, null, null, null, null, userBCategory.getId());

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(categoryRepository.findById(userBCategory.getId())).thenReturn(Optional.of(userBCategory));

        assertThatThrownBy(() -> transactionService.updateTransaction(transactionId, request, userId))
                .isInstanceOf(ResourceOwnershipException.class);

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void deleteTransaction_Success() {
        Transaction transaction = aTransaction();
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        transactionService.deleteTransaction(transactionId, userId);
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void deleteTransaction_NotFound_ThrowsException() {
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(transactionId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    //    Case 3: Ownership Violation
    //      Stubbing: Stub lookup to succeed. Stub validator to throw ResourceOwnershipException.
    //      Assertion: Assert ResourceOwnershipException.
    @Test
    void deleteTransaction_OtherUsersTransaction_ThrowsException() {
        Transaction transaction = aTransaction();

        User userB = aUser();
        UUID userBId = UUID.randomUUID();
        userB.setId(userBId);
        transaction.setUser(userB);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        doThrow(new ResourceOwnershipException()).when(ownershipValidator).validateOwnership(transaction.getUser().getId(), userId);
        assertThatThrownBy(() -> transactionService.deleteTransaction(transactionId, userId))
                .isInstanceOf(ResourceOwnershipException.class);
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }
}

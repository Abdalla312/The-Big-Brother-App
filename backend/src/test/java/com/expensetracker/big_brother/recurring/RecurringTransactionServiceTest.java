package com.expensetracker.big_brother.recurring;

import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.category.CategoryRepository;
import com.expensetracker.big_brother.category.dto.CategoryResponse;
import com.expensetracker.big_brother.common.PageResponse;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.common.validation.OwnershipValidator;
import com.expensetracker.big_brother.exception.ResourceNotFoundException;
import com.expensetracker.big_brother.exception.ResourceOwnershipException;
import com.expensetracker.big_brother.recurring.dto.CreateRecurringTransactionRequest;
import com.expensetracker.big_brother.recurring.dto.RecurringTransactionResponse;
import com.expensetracker.big_brother.recurring.dto.UpdateRecurringTransactionRequest;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecurringTransactionServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID recurringId = UUID.randomUUID();

    @Mock
    private RecurringTransactionRepository repository;
    @Mock
    private RecurringTransactionMapper mapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OwnershipValidator ownershipValidator;
    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    RecurringTransactionService service;


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

    private Category aCategoryBelongingTo(UUID ownerId) {
        Category c = aCategory();
        c.setId(UUID.randomUUID());
        User owner = aUser();
        owner.setId(ownerId);
        c.setUser(owner);
        return c;
    }

    private RecurringTransaction aRecurringTransaction() {
        RecurringTransaction r = new RecurringTransaction();
        r.setId(recurringId);
        r.setUser(aUser());
        r.setCategory(aCategory());
        r.setType(TransactionType.EXPENSE);
        r.setAmount(new BigDecimal("100.0"));
        r.setFrequency(RecurrenceFrequency.MONTHLY);
        r.setNextExecutionDate(LocalDate.now().plusMonths(1));
        r.setActive(true);
        return r;
    }

    private RecurringTransactionResponse aRecurringTransactionResponse(RecurringTransaction r) {
        return new RecurringTransactionResponse(
                r.getId(), r.getType(), r.getAmount(),
                new CategoryResponse(r.getCategory().getId(), r.getCategory().getName(),
                        r.getCategory().getType(), "#FFFFFF", "icon",
                        r.getCategory().getUser() == null, null),
                r.getFrequency(), r.getNextExecutionDate(), r.isActive(),
                r.getPaymentMethod(), r.getNote(), LocalDateTime.now(), null);
    }

    private CreateRecurringTransactionRequest aCreateRequest() {
        return new CreateRecurringTransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("100.00"), categoryId,
                RecurrenceFrequency.MONTHLY, LocalDate.now().plusMonths(1),
                "Credit Card", "Rent");
    }

    private UpdateRecurringTransactionRequest aUpdateRequest() {
        return new UpdateRecurringTransactionRequest(
                null, null, null, null, null, null);
    }

    // Create
    @Test
    void createRecurringTransaction_Success() {
        RecurringTransaction entity = aRecurringTransaction();
        CreateRecurringTransactionRequest request = aCreateRequest();
        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(aCategory()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(aUser()));
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(aRecurringTransactionResponse(entity));
        RecurringTransactionResponse result = service.createRecurringTransaction(request, userId);
        assertThat(result.isActive()).isTrue();
        verify(repository).save(entity);
    }

    @Test
    void createRecurringTransaction_CategoryNotFound_ThrowsException() {
        CreateRecurringTransactionRequest request = aCreateRequest();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createRecurringTransaction(request, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: " + categoryId);
    }

    @Test
    void createRecurringTransaction_otherUsersCategory_ThrowsException() {
        CreateRecurringTransactionRequest request = aCreateRequest();
        User userB = aUser();
        userB.setId(UUID.randomUUID());
        Category userBCategory = aCategory();
        userBCategory.setUser(userB);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(userBCategory));
        assertThatThrownBy(() -> service.createRecurringTransaction(request, userId))
                .isInstanceOf(ResourceOwnershipException.class);
    }

    @Test
    void createRecurringTransaction_TypeMismatchWithCategory_ThrowsException() {
        CreateRecurringTransactionRequest request = new CreateRecurringTransactionRequest(
                TransactionType.INCOME, new BigDecimal("100.00"), categoryId,
                RecurrenceFrequency.MONTHLY, LocalDate.now().plusMonths(1),
                "Credit Card", "Rent");
        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(aCategory()));
        assertThatThrownBy(() -> service.createRecurringTransaction(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction type doesn't match category type");
    }

    @Test
    void createRecurringTransaction_UserNotFound_ThrowsException() {
        CreateRecurringTransactionRequest request = aCreateRequest();
        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(aCategory()));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createRecurringTransaction(request, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: " + userId);
    }

    @Test
    void mapper_MapsStartDateToNextExecutionDate() {
        RecurringTransactionMapper mapper = new RecurringTransactionMapperImpl();
        CreateRecurringTransactionRequest request = new CreateRecurringTransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("100.00"), categoryId,
                RecurrenceFrequency.MONTHLY, LocalDate.now(), "Cash", "Rent");
        RecurringTransaction e = mapper.toEntity(request);
        assertThat(e.getNextExecutionDate()).isEqualTo(LocalDate.now());
        assertThat(e.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(e.getUser()).isNull();
        assertThat(e.getCategory()).isNull();

    }

    // Get All
    @Test
    void getAllRecurringTransactions_Success() {
        RecurringTransaction entity = aRecurringTransaction();
        RecurringTransactionResponse response = aRecurringTransactionResponse(entity);
        Category category = aCategory();
        PageRequest pageRequest = PageRequest.of(
                0, 20, Sort.Direction.DESC, "nextExecutionDate");
        Page<RecurringTransaction> entityPage = new PageImpl<>(List.of(entity), pageRequest, 1);
        when(repository.findAllByUserId(userId, pageRequest)).thenReturn(entityPage);
        when(mapper.toDto(entity)).thenReturn(response);

        PageResponse<RecurringTransactionResponse> result = service.getAllRecurringTransactions(userId, pageRequest);

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().id()).isEqualTo(entity.getId());
        assertThat(result.content().getFirst().isActive()).isTrue();
    }

    // Get one record
    @Test
    void getRecurringTransaction_Success() {
        RecurringTransaction entity = aRecurringTransaction();
        RecurringTransactionResponse response = aRecurringTransactionResponse(entity);

        when(repository.findById(recurringId)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(response);

        RecurringTransactionResponse result = service.getRecurringTransaction(userId, recurringId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(entity.getId());
        verify(repository).findById(recurringId);
    }

    @Test
    void getRecurringTransaction_NonExistentRecord_ThrowsException() {
        when(repository.findById(recurringId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getRecurringTransaction(userId, recurringId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Recurring transaction not found with id: " + recurringId);
    }

    @Test
    void getRecurringTransaction_OtherUsersRecord_ThrowsException() {
        UUID userBId = UUID.randomUUID();
        when(repository.findById(recurringId)).thenReturn(Optional.of(aRecurringTransaction()));
        doThrow(new ResourceOwnershipException()).when(ownershipValidator).validateOwnership(userId, userBId);
        assertThatThrownBy(() -> service.getRecurringTransaction(userBId, recurringId))
                .isInstanceOf(ResourceOwnershipException.class)
                .hasMessage("You do not have permission to access this resource");
        verify(mapper, never()).toDto(any());
    }

    // Update
    @Test
    void updateRecurringTransaction_Success() {
        RecurringTransaction entity = aRecurringTransaction();
        UpdateRecurringTransactionRequest request = aUpdateRequest();
        RecurringTransactionResponse response = aRecurringTransactionResponse(entity);

        when(repository.findById(recurringId)).thenReturn(Optional.of(entity));
        when(mapper.partialUpdate(request, entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(response);
        when(repository.save(entity)).thenReturn(entity);

        RecurringTransactionResponse result = service.updateRecurringTransaction(recurringId, request, userId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(recurringId);
    }

    @Test
    void updateRecurringTransaction_RecordNotFound_ThrowsException() {
        when(repository.findById(recurringId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateRecurringTransaction(recurringId, aUpdateRequest(), userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Recurring transaction not found with id: " + recurringId);
    }

    @Test
    void updateRecurringTransaction_OtherUsersRecord_ThrowsException() {
        RecurringTransaction entity = aRecurringTransaction();
        UUID userBId = UUID.randomUUID();
        when(repository.findById(recurringId)).thenReturn(Optional.of(entity));
        doThrow(new ResourceOwnershipException()).when(ownershipValidator).validateOwnership(userId, userBId);
        assertThatThrownBy(() -> service.updateRecurringTransaction(recurringId, aUpdateRequest(), userBId))
                .isInstanceOf(ResourceOwnershipException.class)
                .hasMessage("You do not have permission to access this resource");
        verify(repository, never()).save(any());
    }

    @Test
    void updateRecurringTransaction_CategoryChanged_Success() {
        RecurringTransaction entity = aRecurringTransaction();
        Category newCategory = aCategory();
        UUID newCategoryId = UUID.randomUUID();
        newCategory.setId(newCategoryId);
        UpdateRecurringTransactionRequest request = new UpdateRecurringTransactionRequest(
                null, newCategory.getId(), null, null, null, null);
        RecurringTransaction updatedEntity = aRecurringTransaction();
        updatedEntity.setCategory(newCategory);
        RecurringTransactionResponse response = aRecurringTransactionResponse(updatedEntity);
        when(repository.findById(recurringId)).thenReturn(Optional.of(entity));
        when(categoryRepository.findById(newCategory.getId())).thenReturn(Optional.of(newCategory));
        when(mapper.partialUpdate(request, entity)).thenReturn(updatedEntity);
        when(mapper.toDto(updatedEntity)).thenReturn(response);
        when(repository.save(updatedEntity)).thenReturn(updatedEntity);

        RecurringTransactionResponse result = service.updateRecurringTransaction(recurringId, request, userId);

        assertThat(result).isNotNull();
        assertThat(result.category().id()).isEqualTo(newCategoryId);
    }

    @Test
    void updateRecurringTransaction_NewCategoryNotFound_ThrowsException() {
        RecurringTransaction entity = aRecurringTransaction();
        UUID newCategoryId = UUID.randomUUID();
        UpdateRecurringTransactionRequest request = new UpdateRecurringTransactionRequest(
                null, newCategoryId, null, null, null, null);

        when(repository.findById(recurringId)).thenReturn(Optional.of(entity));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateRecurringTransaction(recurringId, request, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: " + newCategoryId);
        verify(repository, never()).save(any());
    }

    @Test
    void updateRecurringTransaction_OtherUsersNewCategory_ThrowsException() {
        RecurringTransaction entity = aRecurringTransaction();

        User userB = aUser();
        userB.setId(UUID.randomUUID());

        Category userBCategory = aCategory();
        UUID userBCategoryId = UUID.randomUUID();
        userBCategory.setId(userBCategoryId);
        userBCategory.setUser(userB);

        UpdateRecurringTransactionRequest request = new UpdateRecurringTransactionRequest(
                null, userBCategory.getId(), null, null, null, null);
        when(repository.findById(recurringId)).thenReturn(Optional.of(entity));
        when(categoryRepository.findById(userBCategoryId)).thenReturn(Optional.of(userBCategory));
        doNothing().when(ownershipValidator).validateOwnership(userId, userId);
        doThrow(new ResourceOwnershipException()).when(ownershipValidator).validateOwnership(userB.getId(), userId);
        assertThatThrownBy(() -> service.updateRecurringTransaction(recurringId, request, userId))
                .isInstanceOf(ResourceOwnershipException.class);
        verify(repository, never()).save(any());
    }

    // Toggle
    @Test
    void toggleActive_Success() {
        RecurringTransaction entity = aRecurringTransaction();
        when(repository.findById(recurringId)).thenReturn(Optional.of(entity));
        entity.setActive(false);
        RecurringTransactionResponse response = aRecurringTransactionResponse(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(response);

        RecurringTransactionResponse result = service.toggleActive(recurringId, userId);

        assertThat(result).isNotNull();
        assertThat(result.isActive()).isFalse();
        verify(repository).save(entity);
    }

    @Test
    void toggleActive_RecordNotFound_ThrowsException() {
        when(repository.findById(recurringId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.toggleActive(recurringId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void toggleActive_OtherUsersRecord_ThrowsException() {
        RecurringTransaction entity = aRecurringTransaction();
        UUID userBId = UUID.randomUUID();
        User userB = aUser();
        userB.setId(userBId);
        entity.setUser(userB);
        when(repository.findById(recurringId)).thenReturn(Optional.of(entity));
        doThrow(new ResourceOwnershipException()).when(ownershipValidator).validateOwnership(userBId, userId);
        assertThatThrownBy(() -> service.toggleActive(recurringId, userId))
                .isInstanceOf(ResourceOwnershipException.class);
        verify(repository, never()).save(any());
    }

    // Delete
    @Test
    void deleteRecurringTransaction_Success() {
        RecurringTransaction entity = aRecurringTransaction();
        when(repository.findById(recurringId)).thenReturn(Optional.of(entity));

        service.deleteRecurringTransaction(recurringId, userId);
        verify(repository).delete(entity);
    }

    @Test
    void deleteRecurringTransaction_RecordNotFound_ThrowsException() {
        when(repository.findById(recurringId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteRecurringTransaction(recurringId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteRecurringTransaction_OtherUsersRecord_ThrowsException() {
        UUID userBId = UUID.randomUUID();
        User userB = aUser();
        userB.setId(userBId);
        RecurringTransaction entity = aRecurringTransaction();
        entity.setUser(userB);
        when(repository.findById(recurringId)).thenReturn(Optional.of(entity));
        doThrow(new ResourceOwnershipException()).when(ownershipValidator).validateOwnership(userBId, userId);
        assertThatThrownBy(() -> service.deleteRecurringTransaction(recurringId, userId))
                .isInstanceOf(ResourceOwnershipException.class);
    }
}

package com.expensetracker.big_brother.recurring;

import com.expensetracker.big_brother.category.Category;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.transaction.Transaction;
import com.expensetracker.big_brother.transaction.TransactionRepository;
import com.expensetracker.big_brother.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecurringTransactionProcessorTest {
    @Mock
    private RecurringTransactionRepository repository;
    @Mock
    private TransactionRepository transactionRepository;
    @InjectMocks
    private RecurringTransactionProcessor processor;

    private static final UUID userId = UUID.randomUUID();
    private static final UUID categoryId = UUID.randomUUID();


    private User aUser() {
        User u = new User();
        u.setId(userId);
        return u;
    }

    private Category aCategory() {
        Category c = new Category();
        c.setId(categoryId);
        c.setType(TransactionType.EXPENSE);
        c.setUser(aUser());
        return c;
    }

    private RecurringTransaction aRule(RecurrenceFrequency frequency, LocalDate nextDate) {
        RecurringTransaction r = new RecurringTransaction();
        r.setId(UUID.randomUUID());
        r.setUser(aUser());
        r.setCategory(aCategory());
        r.setType(TransactionType.EXPENSE);
        r.setAmount(new BigDecimal("100.00"));
        r.setFrequency(frequency);
        r.setNextExecutionDate(nextDate);
        r.setActive(true);
        return r;
    }

    private Page<RecurringTransaction> aPage(List<RecurringTransaction> rules) {
        return new PageImpl<>(rules);
    }

    @SuppressWarnings("unchecked")
    private Page<RecurringTransaction> aMockPage(List<RecurringTransaction> rules, boolean hasNext) {
        Page<RecurringTransaction> page = mock(Page.class);
        when(page.getContent()).thenReturn(rules);
        when(page.hasNext()).thenReturn(hasNext);
        return page;
    }

    @Test
    void processSingleRule_createsTransactionCopiesFields() {
        RecurringTransaction rule = aRule(RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 9, 15));
        rule.setNote("Rent");
        rule.setPaymentMethod("Cash");

        processor.processSingleRule(rule);

        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(tx.capture());

        Transaction saved = tx.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(userId);
        assertThat(saved.getCategory().getId()).isEqualTo(categoryId);
        assertThat(saved.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(saved.getAmount()).isEqualByComparingTo("100.00");
        assertThat(saved.getTransactionDate()).isEqualTo(LocalDate.of(2026, 9, 15)); // = rule.nextExecutionDate
        assertThat(saved.getPaymentMethod()).isEqualTo("Cash");
        assertThat(saved.getNote()).isEqualTo("[Auto-recurring] Rent");
    }

    @Test
    void processSingleRule_NullNote_UsesEmptySuffix() {
        RecurringTransaction rule = aRule(RecurrenceFrequency.DAILY, LocalDate.now());
        rule.setNote(null);

        processor.processSingleRule(rule);

        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(tx.capture());
        assertThat(tx.getValue().getNote()).isEqualTo("[Auto-recurring] ");
    }

    @Test
    void ProcessSingleRule_advancesNextDate_DAILY() {
        RecurringTransaction rule = aRule(RecurrenceFrequency.DAILY, LocalDate.of(2026, 9, 15));
        processor.processSingleRule(rule);
        verify(repository).save(rule);
        assertThat(rule.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 9, 16));
    }

    @Test
    void processSingleRule_advancesNextDate_WEEKLY() {
        RecurringTransaction rule = aRule(RecurrenceFrequency.WEEKLY, LocalDate.of(2026, 9, 15));
        processor.processSingleRule(rule);
        assertThat(rule.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 9, 22));
    }

    @Test
    void processSingleRule_advancesNextDate_MONTHLY() {
        RecurringTransaction rule = aRule(RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 9, 15));
        processor.processSingleRule(rule);
        assertThat(rule.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void processSingleRule_advancesNextDate_MONTHLY_RollsOverMonthEnd() {
        RecurringTransaction rule = aRule(RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 1, 31));
        processor.processSingleRule(rule);
        assertThat(rule.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void processSingleRule_advancesNextDate_YEARLY() {
        RecurringTransaction rule = aRule(RecurrenceFrequency.YEARLY, LocalDate.of(2026, 9, 15));
        processor.processSingleRule(rule);
        assertThat(rule.getNextExecutionDate()).isEqualTo(LocalDate.of(2027, 9, 15));
    }

    @Test
    void processSingleRule_advancesNextDate_YEARLY_LeapDayRollsToFeb28() {
        RecurringTransaction rule = aRule(RecurrenceFrequency.YEARLY, LocalDate.of(2024, 2, 29));
        processor.processSingleRule(rule);
        assertThat(rule.getNextExecutionDate()).isEqualTo(LocalDate.of(2025, 2, 28));
    }

    @Test
    void processDueTransactions_ProcessesDueActiveRules() {
        RecurringTransaction r1 = aRule(RecurrenceFrequency.DAILY, LocalDate.now().minusDays(1));
        RecurringTransaction r2 = aRule(RecurrenceFrequency.WEEKLY, LocalDate.now());

        when(repository.findDueBatch(any(LocalDate.class), any(Pageable.class))).thenReturn(aPage(List.of(r1, r2)));

        long count = processor.processDueTransactions();

        assertThat(count).isEqualTo(2);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(repository, times(2)).save(any(RecurringTransaction.class));
        verify(repository).findDueBatch(LocalDate.now(), PageRequest.of(0, 100));
    }

    @Test
    void processDueTransactions_IteratesMultiplePages() {
        RecurringTransaction r1 = aRule(RecurrenceFrequency.DAILY, LocalDate.now());
        RecurringTransaction r2 = aRule(RecurrenceFrequency.WEEKLY, LocalDate.now());
        RecurringTransaction r3 = aRule(RecurrenceFrequency.MONTHLY, LocalDate.now());

        Page<RecurringTransaction> page1 = aMockPage(List.of(r1, r2), true);
        Page<RecurringTransaction> page2 = aMockPage(List.of(r3), false);

        when(repository.findDueBatch(any(LocalDate.class), any(Pageable.class)))
                .thenReturn(page1)
                .thenReturn(page2);
        long count = processor.processDueTransactions();

        assertThat(count).isEqualTo(3);
        verify(repository, times(2)).findDueBatch(any(LocalDate.class), any(Pageable.class));
        verify(transactionRepository, times(3)).save(any(Transaction.class));
    }

    @Test
    void processDueTransactions_ExceptionOnOneRule_ContinuesOthers() {
        RecurringTransaction r1 = aRule(RecurrenceFrequency.DAILY, LocalDate.now().minusDays(1));
        RecurringTransaction r2 = aRule(RecurrenceFrequency.WEEKLY, LocalDate.now());

        Page<RecurringTransaction> page = aPage(List.of(r1, r2));

        when(repository.findDueBatch(any(LocalDate.class), any(Pageable.class))).thenReturn(page);
        when(transactionRepository.save(any(Transaction.class)))
                .thenThrow(new RuntimeException("boom"))
                .thenReturn(null);
        long count = processor.processDueTransactions();

        assertThat(count).isEqualTo(1);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }
}

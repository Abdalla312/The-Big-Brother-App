package com.expensetracker.big_brother.recurring;

import com.expensetracker.big_brother.transaction.Transaction;
import com.expensetracker.big_brother.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@RequiredArgsConstructor
@Slf4j
@Service
public class RecurringTransactionProcessor {
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionRepository transactionRepository;

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
                    processSingleRule(rule);
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

package com.expensetracker.big_brother.recurring;

import com.expensetracker.big_brother.transaction.Transaction;
import com.expensetracker.big_brother.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class RecurringTransactionProcessor {

    private static final int BATCH_SIZE = 100;

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final PlatformTransactionManager transactionManager;

    public void processSingleRule(RecurringTransaction rule) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        
        txTemplate.executeWithoutResult(status -> {
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
        });
    }

    // Scheduler Processing Engine
    public long processDueTransactions() {
        LocalDate today = LocalDate.now();
        Set<UUID> failedIds = new HashSet<>();
        long count = 0;
        while (true) {
            Page<RecurringTransaction> page = recurringTransactionRepository
                    .findDueBatch(today, PageRequest.of(0, BATCH_SIZE));

            var dueRules = page.getContent().stream()
                    .filter(r -> !failedIds.contains(r.getId())).toList();

            if (dueRules.isEmpty()) break;

            for (RecurringTransaction rule : page.getContent()) {
                try {
                    processSingleRule(rule);
                    count++;
                } catch (Exception e) {
                    log.error("Failed to process recurring rule id={}: {}", rule.getId(), e.getMessage());
                    failedIds.add(rule.getId());
                }
            }
        }

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

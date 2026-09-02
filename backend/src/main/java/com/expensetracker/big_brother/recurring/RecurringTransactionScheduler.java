package com.expensetracker.big_brother.recurring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionScheduler {
    private final RecurringTransactionProcessor processor;

    @Scheduled(cron = "0 0 1 * * ?")
    public void runDailyRecurringProcessing() {
        log.info("Starting daily processing of recurring transactions...");
        long totalProcessed = processor.processDueTransactions();
        log.info("Finished processing recurring transactions. Executed: {}", totalProcessed);
    }
}

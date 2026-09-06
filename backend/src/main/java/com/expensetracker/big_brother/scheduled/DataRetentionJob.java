package com.expensetracker.big_brother.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataRetentionJob {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0 2 * * ?")
    public void purgeExpiredDeletedRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

        log.info("Starting purge of deleted records. Cutoff: {}", cutoff);
        int recurringTransactions = jdbcTemplate.update("DELETE FROM recurring_transactions WHERE deleted_at < ?", cutoff);
        log.info("Deleted {} expired recurring transactions", recurringTransactions);

        int transactions = jdbcTemplate.update("DELETE FROM transactions WHERE deleted_at < ?", cutoff);
        log.info("Deleted {} expired transactions", transactions);

        int budgets = jdbcTemplate.update("DELETE FROM budgets WHERE deleted_at < ?", cutoff);
        log.info("Deleted {} expired budgets transactions", budgets);

        int categories = jdbcTemplate.update("DELETE FROM categories WHERE deleted_at < ?", cutoff);
        log.info("Deleted {} expired categories", categories);

        int users = jdbcTemplate.update("DELETE FROM users WHERE deleted_at < ?", cutoff);
        log.info("Deleted {} expired users", users);

        log.info("Purge completed. Total records deleted: {}",
                recurringTransactions +
                        transactions +
                        budgets +
                        categories +
                        users);

    }
}

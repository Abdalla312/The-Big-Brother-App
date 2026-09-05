package com.expensetracker.big_brother.scheduled;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataRetentionJob {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0 2 * * ?")
    public void purgeExpiredDeletedRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

        jdbcTemplate.update("DELETE FROM recurring_transactions WHERE deleted_at < ?", cutoff);
        jdbcTemplate.update("DELETE FROM transactions WHERE deleted_at < ?", cutoff);
        jdbcTemplate.update("DELETE FROM budgets WHERE deleted_at < ?", cutoff);
        jdbcTemplate.update("DELETE FROM categories WHERE deleted_at < ?", cutoff);
        jdbcTemplate.update("DELETE FROM users WHERE deleted_at < ?", cutoff);
    }
}

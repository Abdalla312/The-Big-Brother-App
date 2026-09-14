package com.expensetracker.big_brother.scheduled;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataRetentionJobTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DataRetentionJob job;

    @Test
    void purgeExpiredDeletedRecords_ExecutesAllFiveDeletes() {
        when(jdbcTemplate.update(anyString(), any(LocalDateTime.class))).thenReturn(0);

        job.purgeExpiredDeletedRecords();

        verify(jdbcTemplate, times(5)).update(anyString(), any(LocalDateTime.class));
    }

    @Test
    void purgeExpiredDeletedRecords_VerifiesCorrectSqlAndCaptor() {
        when(jdbcTemplate.update(anyString(), any(LocalDateTime.class))).thenReturn(0);

        job.purgeExpiredDeletedRecords();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(jdbcTemplate, times(5)).update(sqlCaptor.capture(), cutoffCaptor.capture());

        List<String> sqls = sqlCaptor.getAllValues();
        assertThat(sqls).containsExactly(
                "DELETE FROM recurring_transactions WHERE deleted_at < ?",
                "DELETE FROM transactions WHERE deleted_at < ?",
                "DELETE FROM budgets WHERE deleted_at < ?",
                "DELETE FROM categories WHERE deleted_at < ?",
                "DELETE FROM users WHERE deleted_at < ?");
    }

    @Test
    void purgeExpiredDeletedRecords_ZeroRecordsDeleted() {
        when(jdbcTemplate.update(anyString(), any(LocalDateTime.class))).thenReturn(0);
        job.purgeExpiredDeletedRecords();
        verify(jdbcTemplate).update(eq("DELETE FROM recurring_transactions WHERE deleted_at < ?"), any(LocalDateTime.class));
        verify(jdbcTemplate).update(eq("DELETE FROM transactions WHERE deleted_at < ?"), any(LocalDateTime.class));
        verify(jdbcTemplate).update(eq("DELETE FROM budgets WHERE deleted_at < ?"), any(LocalDateTime.class));
        verify(jdbcTemplate).update(eq("DELETE FROM categories WHERE deleted_at < ?"), any(LocalDateTime.class));
        verify(jdbcTemplate).update(eq("DELETE FROM users WHERE deleted_at < ?"), any(LocalDateTime.class));
    }

    @Test
    void purgeExpiredDeletedRecords_VerifiesCutoffDateIs30DaysAgo() {
        when(jdbcTemplate.update(anyString(), any(LocalDateTime.class)))
                .thenReturn(0);

        LocalDateTime before = LocalDateTime.now().minusDays(30);
        job.purgeExpiredDeletedRecords();
        LocalDateTime after = LocalDateTime.now().minusDays(30);

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(jdbcTemplate, times(5)).update(anyString(), cutoffCaptor.capture());

        LocalDateTime captured = cutoffCaptor.getValue();
        assertThat(captured).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void purgeExpiredDeletedRecords_VerifiesTotalLogSum() {
        when(jdbcTemplate.update(eq("DELETE FROM recurring_transactions WHERE deleted_at < ?"), any(LocalDateTime.class))).thenReturn(10);
        when(jdbcTemplate.update(eq("DELETE FROM transactions WHERE deleted_at < ?"), any(LocalDateTime.class))).thenReturn(20);
        when(jdbcTemplate.update(eq("DELETE FROM budgets WHERE deleted_at < ?"), any(LocalDateTime.class))).thenReturn(30);
        when(jdbcTemplate.update(eq("DELETE FROM categories WHERE deleted_at < ?"), any(LocalDateTime.class))).thenReturn(40);
        when(jdbcTemplate.update(eq("DELETE FROM users WHERE deleted_at < ?"), any(LocalDateTime.class))).thenReturn(50);

        job.purgeExpiredDeletedRecords();

        verify(jdbcTemplate).update(eq("DELETE FROM recurring_transactions WHERE deleted_at < ?"), any(LocalDateTime.class));
        verify(jdbcTemplate).update(eq("DELETE FROM transactions WHERE deleted_at < ?"), any(LocalDateTime.class));
        verify(jdbcTemplate).update(eq("DELETE FROM budgets WHERE deleted_at < ?"), any(LocalDateTime.class));
        verify(jdbcTemplate).update(eq("DELETE FROM categories WHERE deleted_at < ?"), any(LocalDateTime.class));
        verify(jdbcTemplate).update(eq("DELETE FROM users WHERE deleted_at < ?"), any(LocalDateTime.class));
    }
}

package com.expensetracker.big_brother.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class TokenBucketTest {

    private static final int CAPACITY = 5;
    private static final int REFILL_RATE = 1;
    private static final int REFILL_PERIOD = 60;

    private RateLimitProperties aDefaultProps() {
        return new RateLimitProperties(true, CAPACITY, REFILL_RATE, REFILL_PERIOD);
    }

    @Test
    void tryConsume_WithinCapacity_returnsTrue() {
        TokenBucket bucket = new TokenBucket(aDefaultProps());
        assertThat(bucket.tryConsume()).isTrue();
    }

    @Test
    void tryConsume_ExhaustsToken_ReturnsFalse() {
        TokenBucket bucket = new TokenBucket(aDefaultProps());
        for (int i = 0; i < CAPACITY; i++) {
            assertThat(bucket.tryConsume()).isTrue();
        }
        assertThat(bucket.tryConsume()).isFalse();
    }

    @Test
    void tryConsume_RefillsOverTime() throws Exception {
        TokenBucket bucket = new TokenBucket(aDefaultProps());
        for (int i = 0; i < CAPACITY; i++) {
            Field lastRefillField = TokenBucket.class.getDeclaredField("lastRefill");
            lastRefillField.setAccessible(true);
            lastRefillField.set(bucket, Instant.now().minusSeconds(REFILL_PERIOD));
            assertThat(bucket.tryConsume()).isTrue();
        }
    }

    @Test
    void tryConsume_Refill_DoesNotExceedCapacity() throws Exception {
        TokenBucket bucket = new TokenBucket(aDefaultProps());
        Field lastRefillField = TokenBucket.class.getDeclaredField("lastRefill");
        lastRefillField.setAccessible(true);
        lastRefillField.set(bucket, Instant.now().minusSeconds(REFILL_PERIOD * 100));
        bucket.tryConsume();
        assertThat(bucket.tryConsume()).isTrue();
    }

    @Test
    void lastRefill_UpdateOnConsume() {
        TokenBucket bucket = new TokenBucket(aDefaultProps());
        Instant before = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        bucket.tryConsume();
        Instant after = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        assertThat(bucket.lastRefill().truncatedTo(ChronoUnit.MILLIS))
                .isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }
}

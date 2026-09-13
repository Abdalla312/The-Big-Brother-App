package com.expensetracker.big_brother.config;

import com.expensetracker.big_brother.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RateLimitingFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private Function<String, TokenBucket> bucketFactory;
    @Mock
    private TokenBucket tokenBucket;

    private RateLimitingFilter filter;

    @BeforeEach
    void setup() throws Exception {
        filter = new RateLimitingFilter(new RateLimitProperties(true, 5, 1, 60), objectMapper, bucketFactory);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private void stubPassingBucket() {
        when(bucketFactory.apply(any())).thenReturn(tokenBucket);
        when(tokenBucket.tryConsume()).thenReturn(true);
    }

    @Test
    void doFilterInternal_RateLimitExceeded_Returns429() throws Exception {
        when(bucketFactory.apply(any())).thenReturn(tokenBucket);
        when(tokenBucket.tryConsume()).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilterInternal(request(), response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(chain.getRequest()).isNull();

        ErrorResponse body = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(body.status()).isEqualTo(429);
        assertThat(body.error()).isEqualTo("TOO_MANY_REQUESTS");
        assertThat(body.message()).isEqualTo("Rate limit exceeded. Try again later.");
        assertThat(body.path()).isEqualTo("/api/v1/auth/login");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void doFilterInternal_RateLimitOk_PassesThrough() throws Exception {
        stubPassingBucket();

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilterInternal(request(), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        verify(bucketFactory).apply("127.0.0.1");
    }

    @Test
    void doInternalFilter_Disabled_PassesThrough() throws Exception {
        RateLimitingFilter disabled = new RateLimitingFilter(new RateLimitProperties(false, 5, 1, 60), objectMapper, bucketFactory);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        disabled.doFilterInternal(request(), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        verify(bucketFactory, never()).apply(any());
    }

    @Test
    void doInternalFilter_RetryAfter_MatchesRefillPeriod() throws ServletException, IOException {
        when(bucketFactory.apply(any())).thenReturn(tokenBucket);
        when(tokenBucket.tryConsume()).thenReturn(false);

        RateLimitingFilter filter = new RateLimitingFilter(new RateLimitProperties(true, 5, 1, 30), objectMapper, bucketFactory);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request(), response, new MockFilterChain());
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");

    }

    @Test
    void shouldNotFilter_NonAuthEndpoint_ReturnsTrue() {
        MockHttpServletRequest request = request();
        request.setRequestURI("/api/v1/transactions");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_AuthEndpoint_ReturnsFalse() {
        assertThat(filter.shouldNotFilter(request())).isFalse();
    }

    @Test
    void shouldNotFilter_AuthSubpath_ReturnsFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/forgot-password");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void extractIp_XForwardedFor_ReturnsFirstIp() throws Exception {
        stubPassingBucket();
        MockHttpServletRequest request = request();
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(bucketFactory).apply("1.2.3.4");
    }

    @Test
    void extractIp_XForwardedForBlank_ReturnsRemoteAddr() throws Exception {
        stubPassingBucket();
        MockHttpServletRequest request = request();
        request.addHeader("X-Forwarded-For", " ");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(bucketFactory).apply("127.0.0.1");
    }

    @Test
    void extractIp_NoXForwardedFor_ReturnsRemoteAddr() throws Exception {
        stubPassingBucket();
        filter.doFilterInternal(request(), new MockHttpServletResponse(), new MockFilterChain());
        verify(bucketFactory).apply("127.0.0.1");
    }
    @Test
    void cleanUp_RemovesExpiredBuckets() throws Exception {
        stubPassingBucket();
        when(tokenBucket.lastRefill()).thenReturn(Instant.now().minusSeconds(601));

        filter.doFilterInternal(request(), new MockHttpServletResponse(), new MockFilterChain());
        filter.cleanUp();
        filter.doFilterInternal(request(), new MockHttpServletResponse(), new MockFilterChain());

        verify(bucketFactory, times(2)).apply("127.0.0.1");
    }

    @Test
    void cleanUp_KeepsRecentBuckets() throws Exception {
        stubPassingBucket();
        when(tokenBucket.lastRefill()).thenReturn(Instant.now());

        filter.doFilterInternal(request(), new MockHttpServletResponse(), new MockFilterChain());
        filter.cleanUp();
        filter.doFilterInternal(request(), new MockHttpServletResponse(), new MockFilterChain());

        verify(bucketFactory, times(1)).apply("127.0.0.1");
    }

    @Test
    void defaultConstructor_Enabled_PassesThrough() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(new RateLimitProperties(true, 5, 1, 60), objectMapper);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Retry-After")).isNull();
    }
}

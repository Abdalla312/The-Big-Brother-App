package com.expensetracker.big_brother.config;

import com.expensetracker.big_brother.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Function<String, TokenBucket> bucketFactory;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Autowired
    RateLimitingFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, k -> new TokenBucket(properties));
    }

    RateLimitingFilter(RateLimitProperties properties, ObjectMapper objectMapper, Function<String, TokenBucket> bucketFactory) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.bucketFactory = bucketFactory;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (!properties.enabled()) {
            chain.doFilter(request, response);
            return;
        }

        String ip = extractIp(request);
        TokenBucket bucket = buckets.computeIfAbsent(ip, bucketFactory);

        if (!bucket.tryConsume()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(properties.refillPeriodSeconds()));
            response.setContentType("application/json");
            ErrorResponse error = ErrorResponse.of(
                    429, "TOO_MANY_REQUESTS", "Rate limit exceeded. Try again later.", request.getRequestURI());
            objectMapper.writeValue(response.getOutputStream(), error);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/auth/");
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanUp() {
        Instant cutoff = Instant.now().minusSeconds(600);
        buckets.entrySet().removeIf(e -> e.getValue().lastRefill().isBefore(cutoff));
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}

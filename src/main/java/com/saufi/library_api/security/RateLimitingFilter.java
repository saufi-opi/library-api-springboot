package com.saufi.library_api.security;

import com.saufi.library_api.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${application.security.rate-limit.auth-endpoints.capacity}")
    private int authCapacity;

    @Value("${application.security.rate-limit.auth-endpoints.refill-tokens}")
    private int authRefillTokens;

    @Value("${application.security.rate-limit.auth-endpoints.refill-duration-minutes}")
    private int authRefillDurationMinutes;

    @Value("${application.security.rate-limit.general-endpoints.capacity}")
    private int generalCapacity;

    @Value("${application.security.rate-limit.general-endpoints.refill-tokens}")
    private int generalRefillTokens;

    @Value("${application.security.rate-limit.general-endpoints.refill-duration-minutes}")
    private int generalRefillDurationMinutes;

    // Cache of buckets per IP address
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIP(request);
        String path = request.getRequestURI();

        // Determine if this is an auth endpoint
        boolean isAuthEndpoint = path.startsWith("/api/v1/auth/");

        Bucket bucket = resolveBucket(ip, isAuthEndpoint);

        if (bucket.tryConsume(1)) {
            // Request allowed
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded - use configured refill duration
            long waitForRefill = isAuthEndpoint ? authRefillDurationMinutes * 60 : generalRefillDurationMinutes * 60;
            log.warn("Rate limit exceeded for IP: {} on path: {}", ip, path);
            throw new RateLimitExceededException(waitForRefill);
        }
    }

    private Bucket resolveBucket(String ip, boolean isAuthEndpoint) {
        if (isAuthEndpoint) {
            return authBuckets.computeIfAbsent(ip, k -> createAuthBucket());
        } else {
            return generalBuckets.computeIfAbsent(ip, k -> createGeneralBucket());
        }
    }

    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(authCapacity)
                .refillIntervally(authRefillTokens, Duration.ofMinutes(authRefillDurationMinutes))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createGeneralBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(generalCapacity)
                .refillIntervally(generalRefillTokens, Duration.ofMinutes(generalRefillDurationMinutes))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}

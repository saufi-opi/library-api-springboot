package com.saufi.library_api.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class LoginAttemptService {

    @Value("${application.security.brute-force.max-attempts}")
    private int maxAttempts;

    @Value("${application.security.brute-force.lockout-duration-minutes}")
    private int lockoutDurationMinutes;

    // Cache to store failed login attempts: key = username, value = attempt count
    private final Cache<String, Integer> attemptsCache;

    public LoginAttemptService() {
        this.attemptsCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15)) // Auto-unlock after 15 minutes
                .build();
    }

    /**
     * Record a failed login attempt
     */
    public void loginFailed(String username) {
        int attempts = attemptsCache.get(username, k -> 0) + 1;
        attemptsCache.put(username, attempts);
        log.warn("Failed login attempt for user: {}. Attempt count: {}", username, attempts);
    }

    /**
     * Clear failed attempts on successful login
     */
    public void loginSucceeded(String username) {
        attemptsCache.invalidate(username);
        log.info("Login succeeded for user: {}. Attempts cleared.", username);
    }

    /**
     * Check if user is locked out
     */
    public boolean isBlocked(String username) {
        Integer attempts = attemptsCache.getIfPresent(username);
        boolean blocked = attempts != null && attempts >= maxAttempts;

        if (blocked) {
            log.warn("User {} is locked out due to {} failed attempts", username, attempts);
        }

        return blocked;
    }

    /**
     * Get remaining attempts before lockout
     */
    public int getRemainingAttempts(String username) {
        Integer attempts = attemptsCache.getIfPresent(username);
        return maxAttempts - (attempts != null ? attempts : 0);
    }

    /**
     * Manually unlock a user (for admin purposes)
     */
    public void unlockUser(String username) {
        attemptsCache.invalidate(username);
        log.info("User {} manually unlocked", username);
    }
}

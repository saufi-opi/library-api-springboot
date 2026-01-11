package com.saufi.library_api.service;

import com.saufi.library_api.domain.entity.TokenBlacklist;
import com.saufi.library_api.dto.request.LoginRequest;
import com.saufi.library_api.dto.response.TokenResponse;
import com.saufi.library_api.repository.TokenBlacklistRepository;
import com.saufi.library_api.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String email = request.getEmail();

        // Check if user is locked out
        if (loginAttemptService.isBlocked(email)) {
            throw new LockedException(
                    "Account is locked due to multiple failed login attempts. Please try again later.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtService.generateToken(userDetails);

            // Clear failed attempts on successful login
            loginAttemptService.loginSucceeded(email);

            log.info("User logged in successfully: {}", userDetails.getUsername());

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .expiresIn(jwtService.getExpirationInSeconds())
                    .build();

        } catch (AuthenticationException e) {
            // Record failed login attempt
            loginAttemptService.loginFailed(email);
            log.warn("Failed login attempt for user: {}", email);
            throw e;
        }
    }

    @Transactional
    public void logout(String token) {
        try {
            // Extract token ID and expiration
            String tokenId = jwtService.extractTokenId(token);
            LocalDateTime expiresAt = jwtService.extractExpiration(token)
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();

            // Add token to blacklist
            TokenBlacklist blacklistedToken = TokenBlacklist.builder()
                    .tokenId(tokenId)
                    .expiresAt(expiresAt)
                    .build();

            tokenBlacklistRepository.save(blacklistedToken);

            log.info("Token blacklisted successfully: {}", tokenId);
        } catch (Exception e) {
            log.error("Error during logout", e);
            throw new RuntimeException("Logout failed", e);
        }
    }
}

package com.saufi.library_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saufi.library_api.dto.request.LoginRequest;
import com.saufi.library_api.dto.response.TokenResponse;
import com.saufi.library_api.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for login (access token)")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;

        @Operation(summary = "Login to get access token", description = "Authenticate with email and password to receive a JWT access token. Rate limited to 5 requests per 60 seconds.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Login successful", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
                        @ApiResponse(responseCode = "429", description = "Rate limit exceeded", content = @Content)
        })
        @PostMapping("/access-token")
        public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest) {
                TokenResponse tokenResponse = authService.login(loginRequest);
                return ResponseEntity.ok(tokenResponse);
        }

        @Operation(summary = "Logout", description = "Revoke the current access token by adding it to the blacklist")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Logout successful", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Invalid or missing token", content = @Content)
        })
        @PostMapping("/logout")
        public ResponseEntity<Void> logout(
                        @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authHeader) {
                // Extract token from Bearer header
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        authService.logout(token);
                }
                return ResponseEntity.ok().build();
        }

}

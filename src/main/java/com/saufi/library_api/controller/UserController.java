package com.saufi.library_api.controller;

import com.saufi.library_api.domain.entity.User;
import com.saufi.library_api.dto.request.UserRequest;
import com.saufi.library_api.dto.response.UserResponse;
import com.saufi.library_api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for user management (ADMIN and LIBRARIAN only)")
public class UserController {

        private final UserService userService;

        @Operation(summary = "Register a new borrower", description = "Create a new user account with MEMBER role. Only accessible by ADMIN and LIBRARIAN.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "User created successfully", content = @Content),
                        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or LIBRARIAN role", content = @Content),
                        @ApiResponse(responseCode = "409", description = "Email already registered", content = @Content)
        })
        @PostMapping
        @PreAuthorize("hasAuthority('users:create')")
        public ResponseEntity<UserResponse> registerBorrower(@Valid @RequestBody UserRequest request) {
                User user = userService.createUser(
                                request.getEmail(),
                                request.getPassword(),
                                request.getFullName());

                return new ResponseEntity<>(mapToResponse(user), HttpStatus.CREATED);
        }

        private UserResponse mapToResponse(User user) {
                return UserResponse.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .fullName(user.getFullName())
                                .roles(user.getRoles().stream()
                                                .map(role -> role.getName())
                                                .collect(Collectors.toSet()))
                                .createdAt(user.getCreatedAt())
                                .build();
        }
}

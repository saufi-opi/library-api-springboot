package com.saufi.library_api.service;

import org.springframework.stereotype.Service;

import com.saufi.library_api.domain.entity.User;
import com.saufi.library_api.dto.request.LoginRequest;
import com.saufi.library_api.dto.response.TokenResponse;
import com.saufi.library_api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public TokenResponse login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found")); // TODO: Create custom exception

        // TODO: validate password
        // TODO: generate access token

        return TokenResponse.builder()
                .accessToken("access_token")
                .build();
    }

}

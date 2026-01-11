package com.saufi.library_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saufi.library_api.BaseIntegrationTest;
import com.saufi.library_api.dto.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends BaseIntegrationTest {

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Test
        void testLoginSuccess() throws Exception {
                LoginRequest loginRequest = LoginRequest.builder()
                                .email("admin@example.com")
                                .password("aaAA1234")
                                .build();

                mockMvc.perform(post("/api/v1/auth/access-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists());
        }

        @Test
        void testLoginFailureIncorrectPassword() throws Exception {
                LoginRequest loginRequest = LoginRequest.builder()
                                .email("admin@example.com")
                                .password("wrongpassword")
                                .build();

                mockMvc.perform(post("/api/v1/auth/access-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void testAccessProtectedRouteWithoutToken() throws Exception {
                mockMvc.perform(get("/api/v1/borrows/me"))
                                .andExpect(status().isForbidden()); // Spring Security returns 403 by default for
                                                                    // unauthenticated if not
                                                                    // configured otherwise
        }
}

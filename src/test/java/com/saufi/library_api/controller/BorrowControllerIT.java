package com.saufi.library_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saufi.library_api.BaseIntegrationTest;
import com.saufi.library_api.TestSecurityUtils;
import com.saufi.library_api.domain.entity.Book;
import com.saufi.library_api.domain.enums.RoleEnum;
import com.saufi.library_api.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BorrowControllerIT extends BaseIntegrationTest {

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Autowired
        private BookRepository bookRepository;

        @Autowired
        private TestSecurityUtils testSecurityUtils;

        @Test
        void testBorrowAndReturnFlow() throws Exception {
                String token = testSecurityUtils.generateToken("member@example.com", RoleEnum.MEMBER);

                // Find an available book (seeded)
                Book book = bookRepository.findAll().stream()
                                .filter(Book::isAvailable)
                                .findFirst()
                                .orElseThrow();
                UUID bookId = book.getId();

                // 1. Borrow
                String response = mockMvc.perform(post("/api/v1/borrows/book/" + bookId)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.bookId").value(bookId.toString()))
                                .andReturn().getResponse().getContentAsString();

                String recordId = objectMapper.readTree(response).get("id").asText();

                // 2. Return
                mockMvc.perform(post("/api/v1/borrows/" + recordId + "/return")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.returnedAt").exists());
        }

        @Test
        void testGetMyBorrows() throws Exception {
                String token = testSecurityUtils.generateToken("member@example.com", RoleEnum.MEMBER);

                mockMvc.perform(get("/api/v1/borrows/me")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());
        }

        @Test
        void testGetAllBorrowsAsLibrarian() throws Exception {
                String token = testSecurityUtils.generateToken("librarian@example.com", RoleEnum.LIBRARIAN);

                mockMvc.perform(get("/api/v1/borrows")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());
        }

        @Test
        void testGetAllBorrowsAsMemberForbidden() throws Exception {
                String token = testSecurityUtils.generateToken("member@example.com", RoleEnum.MEMBER);

                mockMvc.perform(get("/api/v1/borrows")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden());
        }
}

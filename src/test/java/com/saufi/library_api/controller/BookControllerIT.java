package com.saufi.library_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saufi.library_api.BaseIntegrationTest;
import com.saufi.library_api.TestSecurityUtils;
import com.saufi.library_api.domain.enums.RoleEnum;
import com.saufi.library_api.dto.request.BookRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookControllerIT extends BaseIntegrationTest {

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Autowired
        private TestSecurityUtils testSecurityUtils;

        @Test
        void testCreateBookAsLibrarian() throws Exception {
                String token = testSecurityUtils.generateToken("librarian@example.com", RoleEnum.LIBRARIAN);

                BookRequest request = BookRequest.builder()
                                .isbn("978-0134685991")
                                .title("The Pragmatic Programmer")
                                .author("David Thomas")
                                .build();

                mockMvc.perform(post("/api/v1/books")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.isbn").value("9780134685991"))
                                .andExpect(jsonPath("$.title").value("The Pragmatic Programmer"));
        }

        @Test
        void testCreateBookAsMemberForbidden() throws Exception {
                String token = testSecurityUtils.generateToken("member@example.com", RoleEnum.MEMBER);

                BookRequest request = BookRequest.builder()
                                .isbn("978-0134685991")
                                .title("Unauthorized")
                                .author("Member")
                                .build();

                mockMvc.perform(post("/api/v1/books")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testListBooks() throws Exception {
                String token = testSecurityUtils.generateToken("member@example.com", RoleEnum.MEMBER);

                mockMvc.perform(get("/api/v1/books")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.total").isNumber())
                                .andExpect(jsonPath("$.skip").value(0))
                                .andExpect(jsonPath("$.limit").value(100));
        }

        @Test
        void testListBooksWithPagination() throws Exception {
                String token = testSecurityUtils.generateToken("member@example.com", RoleEnum.MEMBER);

                mockMvc.perform(get("/api/v1/books")
                                .param("skip", "0")
                                .param("limit", "10")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.total").isNumber())
                                .andExpect(jsonPath("$.skip").value(0))
                                .andExpect(jsonPath("$.limit").value(10));
        }

        @Test
        void testListBooksWithSearch() throws Exception {
                String token = testSecurityUtils.generateToken("member@example.com", RoleEnum.MEMBER);

                mockMvc.perform(get("/api/v1/books")
                                .param("search", "Pragmatic")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.total").isNumber());
        }

        @Test
        void testListBooksWithAvailableOnlyFilter() throws Exception {
                String token = testSecurityUtils.generateToken("member@example.com", RoleEnum.MEMBER);

                mockMvc.perform(get("/api/v1/books")
                                .param("availableOnly", "true")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray());
        }
}

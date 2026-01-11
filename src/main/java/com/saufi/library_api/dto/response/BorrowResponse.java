package com.saufi.library_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowResponse {
    private UUID id;
    private UUID bookId;
    private String bookTitle;
    private String borrowerEmail;
    private LocalDateTime borrowedAt;
    private LocalDateTime returnedAt;
}

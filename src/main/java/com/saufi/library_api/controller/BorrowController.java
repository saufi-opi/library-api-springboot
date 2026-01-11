package com.saufi.library_api.controller;

import com.saufi.library_api.domain.entity.BorrowRecord;
import com.saufi.library_api.dto.response.BorrowResponse;
import com.saufi.library_api.service.BorrowService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/borrows")
@RequiredArgsConstructor
@Tag(name = "Borrows", description = "Endpoints for managing book borrowing and returns")
public class BorrowController {

    private final BorrowService borrowService;

    @PostMapping("/book/{bookId}")
    @PreAuthorize("hasAuthority('borrows:create')")
    public ResponseEntity<BorrowResponse> borrowBook(
            @PathVariable UUID bookId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return new ResponseEntity<>(mapToResponse(borrowService.borrowBook(bookId, userDetails.getUsername())),
                HttpStatus.CREATED);
    }

    @PostMapping("/{recordId}/return")
    @PreAuthorize("hasAuthority('borrows:return')")
    public ResponseEntity<BorrowResponse> returnBook(
            @PathVariable UUID recordId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(mapToResponse(borrowService.returnBook(recordId, userDetails.getUsername())));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('borrows:read')")
    public ResponseEntity<List<BorrowResponse>> getMyBorrows(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(borrowService.getUserBorrowRecords(userDetails.getUsername(), activeOnly).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('borrows:read_all')")
    public ResponseEntity<List<BorrowResponse>> getAllBorrows() {
        return ResponseEntity.ok(borrowService.getAllBorrowRecords().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    private BorrowResponse mapToResponse(BorrowRecord record) {
        return BorrowResponse.builder()
                .id(record.getId())
                .bookId(record.getBook().getId())
                .bookTitle(record.getBook().getTitle())
                .borrowerEmail(record.getBorrower().getEmail())
                .borrowedAt(record.getBorrowedAt())
                .returnedAt(record.getReturnedAt())
                .build();
    }
}

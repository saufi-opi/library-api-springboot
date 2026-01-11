package com.saufi.library_api.controller;

import com.saufi.library_api.domain.entity.BorrowRecord;
import com.saufi.library_api.dto.request.BorrowRequest;
import com.saufi.library_api.dto.response.BorrowResponse;
import com.saufi.library_api.dto.response.PaginatedResponse;
import com.saufi.library_api.service.BorrowService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    @PostMapping
    @PreAuthorize("hasAuthority('borrows:create')")
    public ResponseEntity<BorrowResponse> borrowBook(
            @Valid @RequestBody BorrowRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return new ResponseEntity<>(
                mapToResponse(borrowService.borrowBook(request.getBookId(), userDetails.getUsername())),
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
    public ResponseEntity<PaginatedResponse<BorrowResponse>> getMyBorrows(
            @RequestParam(defaultValue = "0") Integer skip,
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) UUID bookId,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails userDetails) {
        Page<BorrowRecord> page = borrowService.getUserBorrowRecordsPaginated(
                userDetails.getUsername(), skip, limit, activeOnly, bookId, sort);

        List<BorrowResponse> borrowResponses = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        PaginatedResponse<BorrowResponse> response = PaginatedResponse.<BorrowResponse>builder()
                .data(borrowResponses)
                .total(page.getTotalElements())
                .skip(skip != null ? skip : 0)
                .limit(limit != null ? limit : 100)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('borrows:read_all')")
    public ResponseEntity<PaginatedResponse<BorrowResponse>> getAllBorrows(
            @RequestParam(defaultValue = "0") Integer skip,
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) UUID bookId,
            @RequestParam(required = false) UUID borrowerId,
            @RequestParam(required = false) String sort) {
        Page<BorrowRecord> page = borrowService.getAllBorrowRecordsPaginated(
                skip, limit, activeOnly, bookId, borrowerId, sort);

        List<BorrowResponse> borrowResponses = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        PaginatedResponse<BorrowResponse> response = PaginatedResponse.<BorrowResponse>builder()
                .data(borrowResponses)
                .total(page.getTotalElements())
                .skip(skip != null ? skip : 0)
                .limit(limit != null ? limit : 100)
                .build();

        return ResponseEntity.ok(response);
    }

    private BorrowResponse mapToResponse(BorrowRecord record) {
        return BorrowResponse.builder()
                .id(record.getId())
                .bookId(record.getBook().getId())
                .borrowerId(record.getBorrower().getId())
                .borrowedAt(record.getBorrowedAt())
                .returnedAt(record.getReturnedAt())
                .build();
    }
}

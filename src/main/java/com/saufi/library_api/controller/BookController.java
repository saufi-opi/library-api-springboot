package com.saufi.library_api.controller;

import com.saufi.library_api.domain.entity.Book;
import com.saufi.library_api.dto.request.BookRequest;
import com.saufi.library_api.dto.response.BookResponse;
import com.saufi.library_api.service.BookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Endpoints for managing book copies")
public class BookController {

    private final BookService bookService;

    @GetMapping
    @PreAuthorize("hasAuthority('books:read')")
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('books:read')")
    public ResponseEntity<BookResponse> getBookById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapToResponse(bookService.getBookById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('books:create')")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
        Book book = Book.builder()
                .isbn(request.getIsbn())
                .title(request.getTitle())
                .author(request.getAuthor())
                .isAvailable(request.isAvailable())
                .build();
        return new ResponseEntity<>(mapToResponse(bookService.createBook(book)), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('books:update')")
    public ResponseEntity<BookResponse> updateBook(@PathVariable UUID id, @Valid @RequestBody BookRequest request) {
        Book book = Book.builder()
                .isbn(request.getIsbn())
                .title(request.getTitle())
                .author(request.getAuthor())
                .isAvailable(request.isAvailable())
                .build();
        return ResponseEntity.ok(mapToResponse(bookService.updateBook(id, book)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('books:delete')")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    private BookResponse mapToResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isAvailable(book.isAvailable())
                .createdAt(book.getCreatedAt())
                .build();
    }
}

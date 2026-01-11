package com.saufi.library_api.service;

import com.saufi.library_api.domain.entity.Book;
import com.saufi.library_api.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public Page<Book> getAllBooks(Integer skip, Integer limit, String search, String isbn, Boolean availableOnly,
            String sort) {
        Specification<Book> spec = (root, query, cb) -> cb.conjunction();

        if (search != null && !search.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("author")), "%" + search.toLowerCase() + "%")));
        }

        if (isbn != null && !isbn.isBlank()) {
            String normalizedIsbn = normalizeIsbn(isbn);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isbn"), normalizedIsbn));
        }

        if (availableOnly != null && availableOnly) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isAvailable"), true));
        }

        Sort sortObj = Sort.unsorted();
        if (sort != null && !sort.isBlank()) {
            boolean descending = sort.startsWith("-");
            String field = descending ? sort.substring(1) : sort;
            sortObj = Sort.by(descending ? Sort.Direction.DESC : Sort.Direction.ASC, field);
        }

        int pageSize = (limit != null && limit > 0 && limit <= 1000) ? limit : 100;
        int pageNumber = (skip != null && skip >= 0) ? (skip / pageSize) : 0;

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortObj);

        return bookRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Book getBookById(UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    @Transactional
    public Book createBook(Book book) {
        book.setIsbn(normalizeIsbn(book.getIsbn()));
        validateIsbnConsistency(book);
        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(UUID id, Book bookDetails) {
        Book book = getBookById(id);

        String normalizedIsbn = normalizeIsbn(bookDetails.getIsbn());
        if (!book.getIsbn().equals(normalizedIsbn)) {
            bookDetails.setIsbn(normalizedIsbn);
            validateIsbnConsistency(bookDetails);
        }

        book.setIsbn(normalizedIsbn);
        book.setTitle(bookDetails.getTitle());
        book.setAuthor(bookDetails.getAuthor());
        book.setAvailable(bookDetails.isAvailable());

        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(UUID id) {
        Book book = getBookById(id);
        bookRepository.delete(book);
    }

    private void validateIsbnConsistency(Book book) {
        bookRepository.findFirstByIsbn(book.getIsbn()).ifPresent(existing -> {
            if (!existing.getTitle().equals(book.getTitle())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format(
                                "ISBN %s already exists with title '%s'. Cannot register with different title '%s'.",
                                book.getIsbn(), existing.getTitle(), book.getTitle()));
            }
            if (!existing.getAuthor().equals(book.getAuthor())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format(
                                "ISBN %s already exists with author '%s'. Cannot register with different author '%s'.",
                                book.getIsbn(), existing.getAuthor(), book.getAuthor()));
            }
        });
    }

    private String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return null;
        }
        return isbn.replaceAll("[\\s-]", "");
    }
}

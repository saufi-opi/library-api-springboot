package com.saufi.library_api.service;

import com.saufi.library_api.domain.entity.Book;
import com.saufi.library_api.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Book getBookById(UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    @Transactional
    public Book createBook(Book book) {
        validateIsbnConsistency(book);
        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(UUID id, Book bookDetails) {
        Book book = getBookById(id);

        if (!book.getIsbn().equals(bookDetails.getIsbn())) {
            validateIsbnConsistency(bookDetails);
        }

        book.setIsbn(bookDetails.getIsbn());
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
}

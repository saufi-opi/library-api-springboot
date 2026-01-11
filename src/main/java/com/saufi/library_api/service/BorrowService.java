package com.saufi.library_api.service;

import com.saufi.library_api.domain.entity.Book;
import com.saufi.library_api.domain.entity.BorrowRecord;
import com.saufi.library_api.domain.entity.User;
import com.saufi.library_api.repository.BookRepository;
import com.saufi.library_api.repository.BorrowRecordRepository;
import com.saufi.library_api.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Transactional
    public BorrowRecord borrowBook(UUID bookId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));

        if (!book.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Book '%s' (ID: %s) is not available for borrowing", book.getTitle(), bookId));
        }

        // Check if book is already borrowed (redundant but safe)
        borrowRecordRepository.findByBookIdAndReturnedAtIsNull(bookId).ifPresent(record -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book is already borrowed");
        });

        BorrowRecord record = BorrowRecord.builder()
                .book(book)
                .borrower(user)
                .build();

        book.setAvailable(false);
        bookRepository.save(book);

        return borrowRecordRepository.save(record);
    }

    @Transactional
    public BorrowRecord returnBook(UUID recordId, String userEmail) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Borrow record not found"));

        if (record.getReturnedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This book has already been returned");
        }

        if (!record.getBorrower().getEmail().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only return books that you borrowed");
        }

        record.setReturnedAt(LocalDateTime.now());

        Book book = record.getBook();
        book.setAvailable(true);
        bookRepository.save(book);

        return borrowRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<BorrowRecord> getUserBorrowRecords(String userEmail, boolean activeOnly) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (activeOnly) {
            return borrowRecordRepository.findByBorrowerAndReturnedAtIsNull(user);
        }
        return borrowRecordRepository.findByBorrower(user);
    }

    @Transactional(readOnly = true)
    public Page<BorrowRecord> getUserBorrowRecordsPaginated(String userEmail, Integer skip, Integer limit,
            Boolean activeOnly, UUID bookId, String sort) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Specification<BorrowRecord> spec = (root, query, cb) -> cb.equal(root.get("borrower"), user);

        if (activeOnly != null && activeOnly) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("returnedAt")));
        }

        if (bookId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("book").get("id"), bookId));
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

        return borrowRecordRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<BorrowRecord> getAllBorrowRecords() {
        return borrowRecordRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<BorrowRecord> getAllBorrowRecordsPaginated(Integer skip, Integer limit, Boolean activeOnly,
            UUID bookId, UUID borrowerId, String sort) {
        Specification<BorrowRecord> spec = (root, query, cb) -> cb.conjunction();

        if (activeOnly != null && activeOnly) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("returnedAt")));
        }

        if (bookId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("book").get("id"), bookId));
        }

        if (borrowerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("borrower").get("id"), borrowerId));
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

        return borrowRecordRepository.findAll(spec, pageable);
    }
}

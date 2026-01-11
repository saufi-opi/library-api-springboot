package com.saufi.library_api.repository;

import com.saufi.library_api.domain.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {
    List<Book> findByIsbn(String isbn);

    Optional<Book> findFirstByIsbn(String isbn);
}

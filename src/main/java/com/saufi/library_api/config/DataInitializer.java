package com.saufi.library_api.config;

import com.saufi.library_api.domain.entity.Book;
import com.saufi.library_api.domain.entity.Permission;
import com.saufi.library_api.domain.entity.Role;
import com.saufi.library_api.domain.entity.User;
import com.saufi.library_api.domain.enums.PermissionEnum;
import com.saufi.library_api.domain.enums.RoleEnum;
import com.saufi.library_api.repository.BookRepository;
import com.saufi.library_api.repository.PermissionRepository;
import com.saufi.library_api.repository.RoleRepository;
import com.saufi.library_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:admin@example.com}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:aaAA1234}")
    private String adminPassword;

    @Value("${ADMIN_FULLNAME:System Admin}")
    private String adminFullname;

    @Value("${LIBRARIAN_EMAIL:librarian@example.com}")
    private String librarianEmail;

    @Value("${LIBRARIAN_PASSWORD:aaAA1234}")
    private String librarianPassword;

    @Value("${LIBRARIAN_FULLNAME:System Librarian}")
    private String librarianFullname;

    @Value("${MEMBER_EMAIL:member@example.com}")
    private String memberEmail;

    @Value("${MEMBER_PASSWORD:aaAA1234}")
    private String memberPassword;

    @Value("${MEMBER_FULLNAME:Regular Member}")
    private String memberFullname;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        seedPermissions();
        seedRoles();
        seedUsers();
        seedBooks();
    }

    private void seedPermissions() {
        log.info("Seeding permissions...");
        Arrays.stream(PermissionEnum.values()).forEach(permEnum -> {
            if (permissionRepository.findByCodeName(permEnum.getCodeName()).isEmpty()) {
                Permission permission = Permission.builder()
                        .name(permEnum.getDescription())
                        .codeName(permEnum.getCodeName())
                        .build();
                permissionRepository.save(permission);
            }
        });
    }

    private void seedRoles() {
        log.info("Seeding roles...");
        Arrays.stream(RoleEnum.values()).forEach(roleEnum -> {
            Role role = roleRepository.findByName(roleEnum.getName())
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name(roleEnum.getName())
                            .build()));

            // Link permissions to role
            Set<Permission> permissions = PermissionEnum.getPermissionsByRole(roleEnum).stream()
                    .map(permEnum -> permissionRepository.findByCodeName(permEnum.getCodeName())
                            .orElseThrow(() -> new RuntimeException("Permission not found: " + permEnum.getCodeName())))
                    .collect(Collectors.toSet());

            role.setPermissions(permissions);
            roleRepository.save(role);
        });
    }

    private void seedUsers() {
        log.info("Seeding users...");

        createUser(adminEmail, adminFullname, adminPassword, RoleEnum.ADMIN);
        createUser(librarianEmail, librarianFullname, librarianPassword, RoleEnum.LIBRARIAN);
        createUser(memberEmail, memberFullname, memberPassword, RoleEnum.MEMBER);
    }

    private void seedBooks() {
        log.info("Seeding books...");

        createBookCopy("978-0743273565", "The Great Gatsby", "F. Scott Fitzgerald");
        createBookCopy("978-0743273565", "The Great Gatsby", "F. Scott Fitzgerald");
        createBookCopy("978-0451524935", "1984", "George Orwell");
        createBookCopy("978-0132350884", "Clean Code", "Robert C. Martin");
    }

    private void createBookCopy(String isbn, String title, String author) {
        // Since id is UUID, we can't easily check for exact copy existence without
        // business logic
        // But for seeding, we can check if a copy with this title and author exists to
        // avoid duplicates on restart
        // if the database is persistent.
        long existingCount = bookRepository.findByIsbn(isbn).size();

        // This is a simple heuristic for seeding: if we expect 2 and have 2, skip.
        // For more robust seeding, we might need a custom 'seed_id' or similar.
        // Here we just add if not present at all for simplicity in this demo.
        if (existingCount == 0) {
            Book book = Book.builder()
                    .isbn(isbn)
                    .title(title)
                    .author(author)
                    .isAvailable(true)
                    .build();
            bookRepository.save(book);
        }
    }

    private void createUser(String email, String fullName, String password, RoleEnum roleEnum) {
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("User already exists: {}", email);
            return;
        }

        Role role = roleRepository.findByName(roleEnum.getName())
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleEnum.getName()));

        User user = User.builder()
                .email(email)
                .fullName(fullName)
                .hashedPassword(passwordEncoder.encode(password))
                .roles(new HashSet<>(Set.of(role)))
                .build();

        userRepository.save(user);
        log.info("Test user created: {}", email);
    }
}

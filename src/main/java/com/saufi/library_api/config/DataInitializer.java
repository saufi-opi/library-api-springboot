package com.saufi.library_api.config;

import com.saufi.library_api.domain.entity.Role;
import com.saufi.library_api.domain.entity.User;
import com.saufi.library_api.repository.RoleRepository;
import com.saufi.library_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        createFirstSuperuser();
    }

    private void createFirstSuperuser() {
        String email = "admin@example.com";

        if (userRepository.findByEmail(email).isPresent()) {
            log.info("First superuser already exists: {}", email);
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    Role newRole = Role.builder()
                            .name("ADMIN")
                            .build();
                    return roleRepository.save(newRole);
                });

        User superuser = User.builder()
                .email(email)
                .hashedPassword(passwordEncoder.encode("aaAA1234"))
                .fullName("ADMIN")
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();

        userRepository.save(superuser);
        log.info("First superuser created: {}", email);
    }
}

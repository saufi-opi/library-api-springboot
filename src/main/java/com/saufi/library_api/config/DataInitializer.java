package com.saufi.library_api.config;

import com.saufi.library_api.domain.entity.Permission;
import com.saufi.library_api.domain.entity.Role;
import com.saufi.library_api.domain.entity.User;
import com.saufi.library_api.domain.enums.PermissionEnum;
import com.saufi.library_api.domain.enums.RoleEnum;
import com.saufi.library_api.repository.PermissionRepository;
import com.saufi.library_api.repository.RoleRepository;
import com.saufi.library_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        seedPermissions();
        seedRoles();
        seedUsers();
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

        createTestUser("admin@example.com", "System Admin", "aaAA1234", RoleEnum.ADMIN);
        createTestUser("librarian@example.com", "System Librarian", "aaAA1234", RoleEnum.LIBRARIAN);
        createTestUser("member@example.com", "Regular Member", "aaAA1234", RoleEnum.MEMBER);
    }

    private void createTestUser(String email, String fullName, String password, RoleEnum roleEnum) {
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

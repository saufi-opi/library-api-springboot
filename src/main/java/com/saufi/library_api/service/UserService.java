package com.saufi.library_api.service;

import com.saufi.library_api.domain.entity.Role;
import com.saufi.library_api.domain.entity.User;
import com.saufi.library_api.repository.RoleRepository;
import com.saufi.library_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(String email, String password, String fullName) {
        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        // Get MEMBER role (default for new users)
        Role memberRole = roleRepository.findByName("MEMBER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "MEMBER role not found"));

        // Create user with MEMBER role
        Set<Role> roles = new HashSet<>();
        roles.add(memberRole);

        User user = User.builder()
                .email(email)
                .fullName(fullName)
                .hashedPassword(passwordEncoder.encode(password))
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user created: {} with role MEMBER", savedUser.getEmail());

        return savedUser;
    }
}

package com.saufi.library_api;

import com.saufi.library_api.domain.enums.RoleEnum;
import com.saufi.library_api.security.JwtService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class TestSecurityUtils {

    private final JwtService jwtService;

    public TestSecurityUtils(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String generateToken(String email, RoleEnum role) {
        UserDetails userDetails = User.builder()
                .username(email)
                .password("password") // dummy
                .authorities(role.name())
                .build();
        return jwtService.generateToken(userDetails);
    }
}

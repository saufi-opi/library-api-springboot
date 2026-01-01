package com.saufi.library_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
            "status", "UP",
            "message", "Welcome to Library API",
            "documentation", "/swagger-ui.html"
        );
    }
}

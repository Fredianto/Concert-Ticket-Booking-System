package com.cinema.ticketing.controller;

import com.cinema.ticketing.dto.auth.AuthRequest;
import com.cinema.ticketing.dto.auth.AuthResponse;
import com.cinema.ticketing.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/token")
    public AuthResponse token(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
    }
}

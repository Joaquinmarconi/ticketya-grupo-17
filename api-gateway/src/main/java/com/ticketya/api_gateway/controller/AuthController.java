package com.ticketya.api_gateway.controller;

import com.ticketya.api_gateway.security.AuthDtos.AuthResponse;
import com.ticketya.api_gateway.security.AuthDtos.LoginRequest;
import com.ticketya.api_gateway.security.AuthDtos.RegisterRequest;
import com.ticketya.api_gateway.security.JwtService;
import com.ticketya.api_gateway.security.Role;
import com.ticketya.api_gateway.security.UserAccount;
import com.ticketya.api_gateway.security.UserStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserStore userStore;
    private final JwtService jwtService;
    private final long expirationMinutes;

    public AuthController(
            UserStore userStore,
            JwtService jwtService,
            @Value("${ticketya.security.jwt-expiration-minutes:60}") long expirationMinutes) {
        this.userStore = userStore;
        this.jwtService = jwtService;
        this.expirationMinutes = expirationMinutes;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return userStore.authenticate(request.username(), request.password())
                .map(account -> ResponseEntity.ok(buildResponse(account)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        Role role = request.role();
        if (role == null || role == Role.ADMIN || role == Role.PERSONAL_ACCESO) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return userStore.register(request.username(), request.password(), role)
                .map(account -> ResponseEntity.status(HttpStatus.CREATED).body(buildResponse(account)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    private AuthResponse buildResponse(UserAccount account) {
        return new AuthResponse(
                jwtService.generateToken(account),
                account.id(),
                account.username(),
                account.role().name(),
                expirationMinutes);
    }
}

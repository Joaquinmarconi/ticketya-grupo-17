package com.ticketya.api_gateway.security;

public class AuthDtos {

    public record LoginRequest(String username, String password) {
    }

    public record RegisterRequest(String username, String password, Role role) {
    }

    public record AuthResponse(String token, String usuarioId, String username, String rol, long expiresInMinutes) {
    }
}

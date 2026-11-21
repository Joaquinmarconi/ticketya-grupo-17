package com.ticketya.api_gateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createUser(
                "cliente-1",
                "cliente123",
                Role.CLIENTE);

        createUser(
                "org-1",
                "organizador123",
                Role.ORGANIZADOR);

        createUser(
                "admin-1",
                "admin123",
                Role.ADMIN);

        createUser(
                "acceso-1",
                "acceso123",
                Role.PERSONAL_ACCESO);
    }

    private void createUser(
            String username,
            String password,
            Role role) {

        if (usuarioRepository.existsByUsername(username)) {
            return;
        }

        Usuario usuario = Usuario.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build();

        usuarioRepository.save(usuario);
    }
}
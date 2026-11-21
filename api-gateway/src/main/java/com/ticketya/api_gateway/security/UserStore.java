package com.ticketya.api_gateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserStore {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<UserAccount> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        return usuarioRepository.findByUsername(username)
                .filter(usuario ->
                        passwordEncoder.matches(password, usuario.getPasswordHash()))
                .map(this::toUserAccount);
    }

    public synchronized Optional<UserAccount> register(
            String username,
            String password,
            Role role) {

        if (username == null
                || username.isBlank()
                || password == null
                || password.length() < 8
                || role == null
                || usuarioRepository.existsByUsername(username)) {
            return Optional.empty();
        }

        Usuario usuario = Usuario.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build();

        usuario = usuarioRepository.save(usuario);

        return Optional.of(toUserAccount(usuario));
    }

    private UserAccount toUserAccount(Usuario usuario) {
        return new UserAccount(
                usuario.getUsername(),
                usuario.getUsername(),
                usuario.getPasswordHash(),
                usuario.getRole());
    }
}
package com.ticketya.ventas_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorization =
                request.getHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        try {
            Claims claims = jwtService.parseClaims(token);

            String usuarioId = claims.getSubject();
            String rol = String.valueOf(claims.get("role"));

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            usuarioId,
                            null,
                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_" + rol)));

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            request.setAttribute("usuarioId", usuarioId);
            request.setAttribute("rol", rol);

            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(
                    "{\"title\":\"Unauthorized\","
                            + "\"status\":401,"
                            + "\"detail\":\"Token JWT invalido o vencido\"}");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

package com.ticketya.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return path.startsWith("/api/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/")
                || path.startsWith("/catalogo/v3/")
                || path.startsWith("/ventas/v3/")
                || ("GET".equals(method) && (
                path.startsWith("/api/eventos")
                        || path.startsWith("/api/lugares")));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Se requiere un token JWT");
            return;
        }

        try {
            Claims claims = jwtService.parseClaims(authorization.substring(7));
            String userId = claims.getSubject();
            String role = String.valueOf(claims.get("role"));

            if (!isRoleAllowed(request, role)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                        "El rol no tiene permiso para esta operacion");
                return;
            }

            String correlationId = request.getHeader("X-Correlation-Id");
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            Map<String, String> verifiedHeaders = new LinkedHashMap<>();
            verifiedHeaders.put("X-User-Id", userId);
            verifiedHeaders.put("X-User-Role", role);
            verifiedHeaders.put("X-Correlation-Id", correlationId);

            response.setHeader("X-Correlation-Id", correlationId);
            filterChain.doFilter(new VerifiedHeaderRequest(request, verifiedHeaders), response);
        } catch (JwtException | IllegalArgumentException ex) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token JWT invalido o vencido");
        }
    }

    private boolean isRoleAllowed(HttpServletRequest request, String role) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (path.startsWith("/api/accesos")) {
            return "PERSONAL_ACCESO".equals(role) || "ADMIN".equals(role);
        }
        if (path.startsWith("/api/reportes")) {
            return "ORGANIZADOR".equals(role) || "ADMIN".equals(role);
        }
        if (path.startsWith("/api/devoluciones") && "PUT".equals(method)) {
            return "ADMIN".equals(role);
        }
        if (path.startsWith("/api/reservas")
                || path.startsWith("/api/compras")
                || (path.startsWith("/api/devoluciones") && "POST".equals(method))) {
            return "CLIENTE".equals(role);
        }
        if (path.startsWith("/api/lugares") && !"GET".equals(method)) {
            return "ADMIN".equals(role) || "ORGANIZADOR".equals(role);
        }
        if (path.startsWith("/api/eventos")) {
            if ("POST".equals(method) || path.endsWith("/publicar")) {
                return "ORGANIZADOR".equals(role);
            }
            if (path.endsWith("/suspender") || path.endsWith("/reactivar")) {
                return "ADMIN".equals(role);
            }
            if (path.endsWith("/cancelar")) {
                return "ADMIN".equals(role) || "ORGANIZADOR".equals(role);
            }
        }
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"title\":\"" + (status == 401 ? "Unauthorized" : "Forbidden")
                        + "\",\"status\":" + status
                        + ",\"detail\":\"" + detail + "\"}");
    }

    private static class VerifiedHeaderRequest extends HttpServletRequestWrapper {

        private final Map<String, String> verifiedHeaders;

        private VerifiedHeaderRequest(HttpServletRequest request, Map<String, String> verifiedHeaders) {
            super(request);
            this.verifiedHeaders = verifiedHeaders;
        }

        @Override
        public String getHeader(String name) {
            if (name != null) {
                String verified = verifiedHeaders.get(name);
                if (verified != null) {
                    return verified;
                }
                if (name.equalsIgnoreCase("X-User-Id")
                        || name.equalsIgnoreCase("X-User-Role")) {
                    return null;
                }
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (name != null) {
                String verified = verifiedHeaders.get(name);
                if (verified != null) {
                    return Collections.enumeration(Collections.singletonList(verified));
                }
                if (name.equalsIgnoreCase("X-User-Id")
                        || name.equalsIgnoreCase("X-User-Role")) {
                    return Collections.emptyEnumeration();
                }
            }
            return super.getHeaders(name);
        }
    }
}

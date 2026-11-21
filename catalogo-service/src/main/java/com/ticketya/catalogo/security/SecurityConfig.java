package com.ticketya.catalogo.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/h2-console/**")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/eventos/**",
                                "/api/lugares/**")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/lugares/**")
                        .hasAnyRole("ADMIN", "ORGANIZADOR")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/eventos")
                        .hasRole("ORGANIZADOR")

                        .requestMatchers(
                                "/api/eventos/*/publicar")
                        .hasRole("ORGANIZADOR")

                        .requestMatchers(
                                "/api/eventos/*/suspender",
                                "/api/eventos/*/reactivar")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/eventos/*/cancelar")
                        .hasAnyRole("ADMIN", "ORGANIZADOR")

                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                (request,
                                 response,
                                 exception) -> {
                                    response.setStatus(
                                            HttpServletResponse.SC_UNAUTHORIZED);
                                    response.setContentType(
                                            "application/json");

                                    response.getWriter().write(
                                            "{\"title\":\"Unauthorized\","
                                                    + "\"status\":401,"
                                                    + "\"detail\":\"Autenticacion requerida\"}");
                                })
                        .accessDeniedHandler(
                                (request,
                                 response,
                                 exception) -> {
                                    response.setStatus(
                                            HttpServletResponse.SC_FORBIDDEN);
                                    response.setContentType(
                                            "application/json");

                                    response.getWriter().write(
                                            "{\"title\":\"Forbidden\","
                                                    + "\"status\":403,"
                                                    + "\"detail\":\"Rol sin permiso\"}");
                                }));

        http.addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
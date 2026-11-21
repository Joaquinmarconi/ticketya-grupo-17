package com.ticketya.ventas_service.security;

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
    private final InternalServiceFilter internalServiceFilter;

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
                                "/v3/api-docs/**")
                        .permitAll()

                        .requestMatchers(
                                "/api/reservas/**",
                                "/api/compras/**")
                        .hasRole("CLIENTE")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/devoluciones/**")
                        .hasRole("CLIENTE")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/devoluciones/**")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/reportes/**")
                        .hasAnyRole("ADMIN", "ORGANIZADOR")

                        .requestMatchers(
                                "/api/accesos/**")
                        .hasAnyRole("ADMIN", "PERSONAL_ACCESO")

                        .requestMatchers(
                                "/internal/**")
                        .permitAll()

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
        http.addFilterBefore(
                internalServiceFilter,
                JwtAuthenticationFilter.class);

        return http.build();
    }
}
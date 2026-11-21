package com.ticketya.catalogo.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class VentasClient {

    private final RestClient restClient;
    private final String internalSecret;

    public VentasClient(
            @Value("${ticketya.ventas.url}") String ventasUrl,
            @Value("${ticketya.internal.secret}") String internalSecret) {
        this.internalSecret = internalSecret;
        this.restClient = RestClient.builder()
                .baseUrl(ventasUrl)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set(
                            "X-Internal-Token",
                            internalSecret);
                    ServletRequestAttributes attributes =
                            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        String authorization =
                                attributes.getRequest().getHeader("Authorization");
                        if (authorization != null && !authorization.isBlank()) {
                            request.getHeaders().set("Authorization", authorization);
                        }

                        String correlationId =
                                attributes.getRequest().getHeader("X-Correlation-Id");
                        if (correlationId != null && !correlationId.isBlank()) {
                            request.getHeaders().set("X-Correlation-Id", correlationId);
                        }
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    // Avisa a ventas que el evento se cancela para que reintegre las entradas
    public void procesarCancelacion(Long eventoId) {
        try {
            restClient.post()
                    .uri("/internal/eventos/{id}/cancelacion", eventoId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo procesar el reintegro de entradas en ventas-service. El evento no fue cancelado.");
        }
    }
}

package com.ticketya.ventas_service.client;

import com.ticketya.ventas_service.config.TrazabilidadHttpFilter;
import com.ticketya.ventas_service.dto.EventoCatalogoDTO;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.client.RestClient;

@Component
public class CatalogoClient {

    private final RestClient restClient;

    public CatalogoClient(
            @Value("${ticketya.catalogo.url}") String catalogoUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(catalogoUrl)
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = MDC.get(TrazabilidadHttpFilter.CORRELATION_ID_MDC_KEY);
                    if (correlationId != null && !correlationId.isBlank()) {
                        request.getHeaders().set(TrazabilidadHttpFilter.CORRELATION_ID_HEADER, correlationId);
                    }

                    ServletRequestAttributes attributes =
                            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        String authorization =
                                attributes.getRequest().getHeader("Authorization");
                        if (authorization != null && !authorization.isBlank()) {
                            request.getHeaders().set("Authorization", authorization);
                        }

                        if (correlationId == null || correlationId.isBlank()) {
                            String requestCorrelationId =
                                    attributes.getRequest().getHeader(
                                            TrazabilidadHttpFilter.CORRELATION_ID_HEADER);
                            if (requestCorrelationId != null
                                    && !requestCorrelationId.isBlank()) {
                                request.getHeaders().set(
                                        TrazabilidadHttpFilter.CORRELATION_ID_HEADER,
                                        requestCorrelationId);
                            }
                        }
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    public EventoCatalogoDTO obtenerEvento(Long eventoId) {
        EventoCatalogoDTO evento = restClient.get()
                .uri("/api/eventos/{id}", eventoId)
                .retrieve()
                .body(EventoCatalogoDTO.class);

        if (evento == null || evento.getFechaHora() == null) {
            throw new IllegalStateException(
                    "Catálogo no devolvió la fecha del evento " + eventoId);
        }

        return evento;
    }
}
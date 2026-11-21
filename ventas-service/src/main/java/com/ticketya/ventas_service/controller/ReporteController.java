package com.ticketya.ventas_service.controller;

import com.ticketya.ventas_service.dto.ReporteEventoDTO;
import com.ticketya.ventas_service.service.VentasServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reportes", description = "Reportes de ventas, devoluciones, ocupación y recaudación por sector (Req. 12)")
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final VentasServices ventasService;

    @Operation(summary = "Consultar reporte de un evento por sector",
            description = "Para ORGANIZADOR (dueño del evento) o ADMIN. Devuelve entradas vendidas, utilizadas, devueltas, porcentaje de ocupación y recaudación por sector y total.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte generado"),
            @ApiResponse(responseCode = "403", description = "El usuario no es ORGANIZADOR/ADMIN o el evento pertenece a otro organizador"),
            @ApiResponse(responseCode = "404", description = "El evento no existe")
    })
    @GetMapping("/eventos/{eventoId}")
    public ResponseEntity<ReporteEventoDTO> obtenerReporteEvento(
            @PathVariable Long eventoId,
            Authentication authentication) {
        String userId = authentication.getName();
        String role = authentication.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", "");
        return ResponseEntity.ok(ventasService.obtenerReporteEvento(eventoId, userId, role));
    }
}
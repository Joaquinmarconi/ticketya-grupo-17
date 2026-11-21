package com.ticketya.ventas_service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VentasServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	private String crearToken(String usuarioId, String rol) {
		Instant ahora = Instant.now();
		return Jwts.builder()
				.subject(usuarioId)
				.claim("role", rol)
				.issuedAt(Date.from(ahora))
				.expiration(Date.from(ahora.plusSeconds(3600)))
				.signWith(Keys.hmacShaKeyFor(
						"change-this-in-production-change-this-in-production"
								.getBytes(StandardCharsets.UTF_8)))
				.compact();
	}

	@Test
	void sinTokenRechazaReserva() throws Exception {
		mockMvc.perform(post("/api/reservas")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void organizadorNoPuedeReservarEntradas() throws Exception {
		mockMvc.perform(post("/api/reservas")
						.header("Authorization", "Bearer " + crearToken("org-1", "ORGANIZADOR"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void clientePuedeIntentarCrearReserva() throws Exception {
		mockMvc.perform(post("/api/reservas")
						.header("Authorization", "Bearer " + crearToken("cliente-1", "CLIENTE"))
						.header("X-User-Id", "cliente-falso")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void clienteNoPuedeValidarQrComoPersonalDeAcceso() throws Exception {
		mockMvc.perform(post("/api/accesos/validar-qr")
						.header("Authorization", "Bearer " + crearToken("cliente-1", "CLIENTE"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventoId\":1,\"codigoQR\":\"QR-prueba\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void personalDeAccesoPuedeIntentarValidarQr() throws Exception {
		mockMvc.perform(post("/api/accesos/validar-qr")
						.header("Authorization", "Bearer " + crearToken("acceso-1", "PERSONAL_ACCESO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventoId\":1,\"codigoQR\":\"QR-prueba\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void endpointInternoRechazaCredencialInternaIncorrecta() throws Exception {
		mockMvc.perform(post("/internal/eventos/1/cancelacion")
						.header("X-Internal-Token", "secreto-incorrecto"))
				.andExpect(status().isUnauthorized());
	}
}

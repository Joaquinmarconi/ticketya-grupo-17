package com.ticketya.catalogo.catalogo_service;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogoServiceApplicationTests {

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
	void sinTokenRechazaOperacionProtegida() throws Exception {
		mockMvc.perform(post("/api/eventos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void clienteNoPuedeCrearEvento() throws Exception {
		mockMvc.perform(post("/api/eventos")
						.header("Authorization", "Bearer " + crearToken("cliente-1", "CLIENTE"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void organizadorPuedeIntentarCrearEvento() throws Exception {
		mockMvc.perform(post("/api/eventos")
						.header("Authorization", "Bearer " + crearToken("org-1", "ORGANIZADOR"))
						.header("X-User-Id", "cliente-falso")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Evento de prueba\",\"tipo\":\"RECITAL\","
								+ "\"fechaHora\":\"2026-12-15T20:00:00\","
								+ "\"lugarId\":999,\"precios\":[]}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void consultaPublicaDeEventosNoNecesitaToken() throws Exception {
		mockMvc.perform(get("/api/eventos"))
				.andExpect(status().isOk());
	}
}

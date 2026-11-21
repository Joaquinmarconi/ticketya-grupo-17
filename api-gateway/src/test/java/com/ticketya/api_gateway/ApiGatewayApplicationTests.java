package com.ticketya.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.ticketya.api_gateway.security.Usuario;
import static org.assertj.core.api.Assertions.assertThat;
import com.ticketya.api_gateway.security.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiGatewayApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	

	@Test
	void contextLoads() {
	}

	@BeforeEach
	void limpiarUsuariosDePrueba() {
		usuarioRepository
				.findByUsername("cliente-test")
				.ifPresent(usuarioRepository::delete);
	}

	@Test
	void usuarioSeedExisteEnBase() {
		assertThat(usuarioRepository.findByUsername("cliente-1"))
				.isPresent();
	}

	@Test
	void registroPersisteUsuario() throws Exception {
		String username = "nuevo-cliente-" + System.currentTimeMillis();

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"" + username
								+ "\",\"password\":\"password123\",\"role\":\"CLIENTE\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"" + username
								+ "\",\"password\":\"password123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isString());
	}


	@Test
	void noPersistePasswordSinCifrar() {
		Usuario usuario = usuarioRepository.findByUsername("cliente-1")
				.orElseThrow();

		assertThat(usuario.getPasswordHash())
				.isNotEqualTo("cliente123");

		assertThat(usuario.getPasswordHash())
				.startsWith("$2");
	}

	@Test
	void loginDevuelveJWTYRolTest() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"cliente-1\",\"password\":\"cliente123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isString())
				.andExpect(jsonPath("$.usuarioId").value("cliente-1"))
				.andExpect(jsonPath("$.rol").value("CLIENTE"));
	}

	@Test
	void loginRechazaCredencialesInvalidasTest() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"cliente-1\",\"password\":\"incorrecta\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void endpointProtegidoRechazaTokenFaltanteTest() throws Exception {
		mockMvc.perform(post("/api/reservas")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void organizadorNoPuedeReservarParaOtroRolTest() throws Exception {
		String login = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"org-1\",\"password\":\"organizador123\"}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = login.substring(login.indexOf("\"token\":\"") + 9, login.indexOf("\"", login.indexOf("\"token\":\"") + 9));

		mockMvc.perform(post("/api/reservas")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void registroCreaClienteYLoginDevuelveTokenTest() throws Exception {
		String username = "cliente-test-" + System.currentTimeMillis();
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"" + username
								+ "\",\"password\":\"password123\",\"role\":\"CLIENTE\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.rol").value("CLIENTE"));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"" + username
								+ "\",\"password\":\"password123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isString());
	}
}

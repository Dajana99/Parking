package com.smartpark.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integracioni test API-ja (spec. 8.3, prvi od pet: registracija i prijava).
 * Pokrece celu aplikaciju nad pravom PostgreSQL bazom u Docker-u.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String uniqueEmail() {
        return "vozac-" + UUID.randomUUID() + "@example.com";
    }

    private static String registerJson(String name, String email, String password) {
        return """
                {"name":"%s","email":"%s","password":"%s"}
                """.formatted(name, email, password);
    }

    private static String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    @Test
    void registracijaPrijavaIPristupZasticenojPutanji() throws Exception {
        String email = uniqueEmail();

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Ana Anic", email, "tajna1234")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("VOZAC"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());

        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email.toUpperCase(), "tajna1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        String token = json.get("token").asText();

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Ana Anic"));
    }

    @Test
    void dvostrukaRegistracijaVraca409() throws Exception {
        String email = uniqueEmail();
        String json = registerJson("Ana", email, "tajna1234");

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void neispravniPodaciVracaju400SaPorukomPoPolju() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("<script>", "nije-email", "kratka")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void pogresnaLozinkaVraca401() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Ana", email, "tajna1234")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "pogresna1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Pogresan e-mail ili lozinka."));
    }

    @Test
    void zasticenaPutanjaBezTokenaVraca401() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/me").header("Authorization", "Bearer ovo.nije.token"))
                .andExpect(status().isUnauthorized());
    }
}

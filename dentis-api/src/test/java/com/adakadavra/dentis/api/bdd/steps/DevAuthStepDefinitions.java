package com.adakadavra.dentis.api.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class DevAuthStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private int lastStatus;
    private String lastBody;

    @Before
    public void reset() {
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, password, role, full_name, active) VALUES (?, ?, ?, ?, ?, ?, true)",
                UUID.randomUUID(),
                "jbello",
                "jbello@dentis.dev",
                passwordEncoder.encode("Admin@2026!"),
                "SUPER_ADMIN",
                "J. Bello"
        );
        lastStatus = 0;
        lastBody = null;
    }

    @When("dev auth api logs in with username {string} and password {string}")
    public void devAuthLoginWithCredentials(String username, String password) throws Exception {
        Map<String, Object> payload = Map.of(
                "username", username,
                "password", password
        );
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("dev auth api logs in with incomplete payload")
    public void devAuthLoginWithIncompletePayload() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"jbello\"}"))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @Then("dev auth response status should be {int}")
    public void devAuthResponseStatusShouldBe(int expected) {
        assertThat(lastStatus).isEqualTo(expected);
    }

    @And("dev auth response contains a token")
    public void devAuthResponseContainsToken() throws Exception {
        JsonNode data = objectMapper.readTree(lastBody).path("data");
        assertThat(data.path("token").asText()).isNotBlank();
        assertThat(data.path("type").asText()).isEqualTo("Bearer");
    }
}


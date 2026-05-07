package com.adakadavra.dentis.api.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class ClinicStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID clinicId;
    private UUID clinicUserId;
    private String currentRole;
    private int lastStatus;
    private String lastBody;

    @Before
    public void reset() {
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM clinics");
        clinicId = null;
        clinicUserId = null;
        currentRole = "SUPER_ADMIN";
        lastStatus = 0;
        lastBody = null;
    }

    @Given("clinic api request is authenticated as role {string}")
    public void clinicApiAuthenticatedAsRole(String role) {
        this.currentRole = role;
    }

    @Given("clinic api has a valid create payload")
    public void clinicApiHasValidCreatePayload() {
        // no-op, payload built in helper
    }

    @Given("clinic api has an existing clinic")
    public void clinicApiHasExistingClinic() throws Exception {
        this.currentRole = "SUPER_ADMIN";
        executePost("/api/v1/clinics", validClinicPayload("Clinic Alpha"));
        assertThat(lastStatus).isEqualTo(201);
        clinicId = UUID.fromString(parseData().path("id").asText());
    }

    @Given("clinic api has an existing clinic user")
    public void clinicApiHasExistingClinicUser() throws Exception {
        clinicApiHasExistingClinic();
        this.currentRole = "ADMIN";
        executePost("/api/v1/clinics/" + clinicId + "/users", validClinicUserPayload("doc.user@dentis.dev"));
        assertThat(lastStatus).isEqualTo(201);
        clinicUserId = UUID.fromString(parseData().path("id").asText());
    }

    @When("clinic api creates a clinic")
    public void clinicApiCreatesClinic() throws Exception {
        executePost("/api/v1/clinics", validClinicPayload("Clinic Prime"));
        if (lastStatus == 201) {
            clinicId = UUID.fromString(parseData().path("id").asText());
        }
    }

    @When("clinic api fetches clinic by id as role {string}")
    public void clinicApiFetchesByIdAsRole(String role) throws Exception {
        this.currentRole = role;
        execute(get("/api/v1/clinics/{id}", clinicId));
    }

    @When("clinic api lists clinics as role {string}")
    public void clinicApiListsAsRole(String role) throws Exception {
        this.currentRole = role;
        execute(get("/api/v1/clinics?page=0&size=20"));
    }

    @When("clinic api lists active clinics as role {string}")
    public void clinicApiListsActiveAsRole(String role) throws Exception {
        this.currentRole = role;
        execute(get("/api/v1/clinics/active"));
    }

    @When("clinic api updates that clinic as role {string}")
    public void clinicApiUpdatesAsRole(String role) throws Exception {
        this.currentRole = role;
        execute(put("/api/v1/clinics/{id}", clinicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validClinicPayload("Clinic Updated"))));
    }

    @When("clinic api deactivates that clinic as role {string}")
    public void clinicApiDeactivatesAsRole(String role) throws Exception {
        this.currentRole = role;
        execute(delete("/api/v1/clinics/{id}", clinicId));
    }

    @When("clinic api creates a clinic user as role {string}")
    public void clinicApiCreatesUserAsRole(String role) throws Exception {
        this.currentRole = role;
        executePost("/api/v1/clinics/" + clinicId + "/users", validClinicUserPayload("new.user@dentis.dev"));
        if (lastStatus == 201) {
            clinicUserId = UUID.fromString(parseData().path("id").asText());
        }
    }

    @When("clinic api lists clinic users as role {string}")
    public void clinicApiListsUsersAsRole(String role) throws Exception {
        this.currentRole = role;
        execute(get("/api/v1/clinics/{clinicId}/users", clinicId));
    }

    @When("clinic api deactivates clinic user as role {string}")
    public void clinicApiDeactivatesUserAsRole(String role) throws Exception {
        this.currentRole = role;
        execute(delete("/api/v1/clinics/{clinicId}/users/{userId}", clinicId, clinicUserId));
    }

    @Then("clinic response status should be {int}")
    public void clinicResponseStatusShouldBe(int expected) {
        assertThat(lastStatus).isEqualTo(expected);
    }

    @And("clinic response contains an id")
    public void clinicResponseContainsId() throws Exception {
        assertThat(parseData().path("id").asText()).isNotBlank();
    }

    @And("clinic response id matches stored clinic id")
    public void clinicResponseIdMatches() throws Exception {
        assertThat(parseData().path("id").asText()).isEqualTo(clinicId.toString());
    }

    @And("clinic page contains at least one clinic")
    public void clinicPageContainsAtLeastOne() throws Exception {
        JsonNode content = parseData().path("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isGreaterThan(0);
    }

    @And("clinic active list is not empty")
    public void clinicActiveListNotEmpty() throws Exception {
        JsonNode data = parseData();
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThan(0);
    }

    @And("clinic name is updated")
    public void clinicNameUpdated() throws Exception {
        assertThat(parseData().path("name").asText()).isEqualTo("Clinic Updated");
    }

    @And("clinic user response contains an id")
    public void clinicUserResponseContainsId() throws Exception {
        assertThat(parseData().path("id").asText()).isNotBlank();
    }

    @And("clinic users list is not empty")
    public void clinicUsersListNotEmpty() throws Exception {
        JsonNode data = parseData();
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThan(0);
    }

    private void executePost(String url, Object payload) throws Exception {
        execute(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));
    }

    private void execute(MockHttpServletRequestBuilder builder) throws Exception {
        SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor principal =
                SecurityMockMvcRequestPostProcessors.user("bdd-user").roles(currentRole);
        MvcResult result = mockMvc.perform(builder.with(principal)).andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    private JsonNode parseData() throws Exception {
        return objectMapper.readTree(lastBody).path("data");
    }

    private Map<String, Object> validClinicPayload(String name) {
        return Map.of(
                "name", name,
                "nif", "NIF-" + UUID.randomUUID().toString().substring(0, 8),
                "address", "Main Street 123",
                "city", "Madrid",
                "province", "Madrid",
                "zipCode", "28001",
                "phone", "+34 900 100 100",
                "email", "clinic@dentis.dev"
        );
    }

    private Map<String, Object> validClinicUserPayload(String email) {
        return Map.of(
                "username", "user_" + UUID.randomUUID().toString().substring(0, 8),
                "email", email,
                "password", "StrongPass123!",
                "fullName", "Doctor BDD",
                "role", "MEDICO"
        );
    }
}


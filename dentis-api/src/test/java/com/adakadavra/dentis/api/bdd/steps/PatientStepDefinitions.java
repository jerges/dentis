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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class PatientStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Map<String, Object> createPayload;
    private UUID patientId;
    private int lastStatus;
    private String lastBody;
    private Exception lastException;

    @Before
    public void reset() {
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM budget_items");
        jdbcTemplate.execute("DELETE FROM budgets");
        jdbcTemplate.execute("DELETE FROM appointments");
        jdbcTemplate.execute("DELETE FROM patients");
        createPayload = null;
        patientId = null;
        lastStatus = 0;
        lastBody = null;
        lastException = null;
    }

    @Given("patient api has a valid create payload")
    public void patientApiHasValidCreatePayload() {
        createPayload = buildCreatePayload("DOC-" + UUID.randomUUID().toString().substring(0, 8));
    }

    @Given("patient api has an existing patient")
    public void patientApiHasExistingPatient() throws Exception {
        patientApiHasValidCreatePayload();
        executePost("/api/v1/patients", createPayload);
        assertThat(lastStatus).isEqualTo(201);
        patientId = UUID.fromString(parseData().path("id").asText());
    }

    @When("patient api creates a patient")
    public void patientApiCreatesPatient() throws Exception {
        executePost("/api/v1/patients", createPayload);
        if (lastStatus == 201) {
            patientId = UUID.fromString(parseData().path("id").asText());
        }
    }

    @When("patient api fetches that patient by id")
    public void patientApiFetchesById() throws Exception {
        executeGet("/api/v1/patients/" + patientId);
    }

    @When("patient api lists patients")
    public void patientApiListsPatients() throws Exception {
        executeGet("/api/v1/patients?page=0&size=20");
    }

    @When("patient api searches patients by name")
    public void patientApiSearchesByName() throws Exception {
        executeGet("/api/v1/patients/search?name=John&page=0&size=20");
    }

    @When("patient api updates that patient")
    public void patientApiUpdatesPatient() throws Exception {
        Map<String, Object> updatePayload = Map.of(
                "firstName", "Updated",
                "contactInfo", Map.of("email", "updated.patient@dentis.dev", "phoneNumber", "+34 600000002")
        );

        MvcResult result = mockMvc.perform(put("/api/v1/patients/{id}", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("patient api deactivates that patient")
    public void patientApiDeactivatesPatient() throws Exception {
        MvcResult result = mockMvc.perform(delete("/api/v1/patients/{id}", patientId)).andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("patient api tries to create another patient with same id document")
    public void patientApiCreatesDuplicateDocument() throws Exception {
        executePost("/api/v1/patients", createPayload);
    }

    @When("patient api fetches an unknown patient")
    public void patientApiFetchesUnknownPatient() throws Exception {
        executeGet("/api/v1/patients/00000000-0000-0000-0000-000000000000");
    }

    @Then("patient response status should be {int}")
    public void patientResponseStatusShouldBe(int expected) {
        assertThat(lastStatus)
                .withFailMessage(
                        "Expected status %s but was %s. Response body: %s. Resolved exception: %s",
                        expected,
                        lastStatus,
                        lastBody,
                        lastException == null ? "<none>" : lastException.getClass().getName() + " - " + lastException.getMessage())
                .isEqualTo(expected);
    }

    @Then("patient deactivate status should be {int}")
    public void patientDeactivateStatusShouldBe(int expected) {
        assertThat(lastStatus).isEqualTo(expected);
    }

    @And("patient response contains an id")
    public void patientResponseContainsId() throws Exception {
        assertThat(parseData().path("id").asText()).isNotBlank();
    }

    @And("patient response id matches stored patient id")
    public void patientResponseIdMatches() throws Exception {
        assertThat(parseData().path("id").asText()).isEqualTo(patientId.toString());
    }

    @And("patient list contains at least one patient")
    public void patientListContainsAtLeastOne() throws Exception {
        JsonNode content = parseData().path("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isGreaterThan(0);
    }

    @And("patient search result is not empty")
    public void patientSearchIsNotEmpty() throws Exception {
        JsonNode content = parseData().path("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isGreaterThan(0);
    }

    @And("patient first name is updated")
    public void patientFirstNameUpdated() throws Exception {
        assertThat(parseData().path("firstName").asText()).isEqualTo("Updated");
    }

    private void executePost(String url, Object payload) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
        lastException = result.getResolvedException();
    }

    private void executeGet(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url)).andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
        lastException = result.getResolvedException();
    }

    private JsonNode parseData() throws Exception {
        return objectMapper.readTree(lastBody).path("data");
    }

    private Map<String, Object> buildCreatePayload(String idDocument) {
        return Map.of(
                "firstName", "John",
                "lastName", "Doe",
                "idDocument", idDocument,
                "birthDate", LocalDate.of(1990, 2, 10).toString(),
                "sex", "MALE",
                "gender", "MALE",
                "contactInfo", Map.of(
                        "email", "john.doe@dentis.dev",
                        "phoneNumber", "+34 600000001"
                )
        );
    }
}


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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class ClinicalStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID patientId;
    private UUID dentistId;
    private int lastStatus;
    private String lastBody;
    private Exception lastException;

    @Before
    public void reset() {
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM budget_items");
        jdbcTemplate.execute("DELETE FROM budgets");
        jdbcTemplate.execute("DELETE FROM appointments");
        jdbcTemplate.execute("DELETE FROM treatment_procedures");
        jdbcTemplate.execute("DELETE FROM treatment_plans");
        jdbcTemplate.execute("DELETE FROM clinical_evolutions");
        jdbcTemplate.execute("DELETE FROM diagnoses");
        jdbcTemplate.execute("DELETE FROM odontogram_teeth");
        jdbcTemplate.execute("DELETE FROM clinical_records");
        jdbcTemplate.execute("DELETE FROM patients");
        patientId = null;
        dentistId = UUID.randomUUID();
        lastStatus = 0;
        lastBody = null;
        lastException = null;
    }

    @Given("clinical api has an existing patient")
    public void clinicalApiHasExistingPatient() throws Exception {
        patientId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO patients (id, first_name, last_name, id_document, birth_date, sex, gender, active, created_at) " +
                        "VALUES (?, 'Clinical', 'Patient', ?, '1985-03-15', 'MALE', 'MALE', true, NOW())",
                patientId, "CLP-" + patientId.toString().substring(0, 8));
        executeGet("/api/v1/clinical-records/patient/" + patientId);
        lastStatus = 0;
        lastBody = null;
    }

    @When("clinical api fetches or creates the clinical record")
    public void clinicalApiFetchesOrCreates() throws Exception {
        executeGet("/api/v1/clinical-records/patient/" + patientId);
    }

    @When("clinical api fetches or creates the clinical record again")
    public void clinicalApiFetchesOrCreatesAgain() throws Exception {
        executeGet("/api/v1/clinical-records/patient/" + patientId);
    }

    @When("clinical api updates the odontogram with tooth conditions")
    public void clinicalApiUpdatesOdontogram() throws Exception {
        Map<String, Object> payload = Map.of(
                "teeth", List.of(
                        Map.of("toothNumber", 11, "condition", "CARIES",
                                "affectedSurfaces", List.of("BUCCAL", "OCCLUSAL")),
                        Map.of("toothNumber", 21, "condition", "RESTORED")
                )
        );
        executePut("/api/v1/clinical-records/patient/" + patientId + "/odontogram", payload);
    }

    @When("clinical api marks a tooth as absent with space status OPEN")
    public void clinicalApiMarksTootAbsentWithSpaceStatus() throws Exception {
        Map<String, Object> payload = Map.of(
                "teeth", List.of(
                        Map.of("toothNumber", 36, "condition", "ABSENT", "spaceStatus", "OPEN")
                )
        );
        executePut("/api/v1/clinical-records/patient/" + patientId + "/odontogram", payload);
    }

    @When("clinical api updates dentition type to {string}")
    public void clinicalApiUpdatesDentitionType(String type) throws Exception {
        MvcResult result = mockMvc.perform(
                patch("/api/v1/clinical-records/patient/{id}/dentition-type", patientId)
                        .param("dentitionType", type))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
        lastException = result.getResolvedException();
    }

    @When("clinical api adds a valid evolution")
    public void clinicalApiAddsValidEvolution() throws Exception {
        Map<String, Object> payload = Map.of(
                "dentistId", dentistId.toString(),
                "description", "Patient reports tooth sensitivity on the upper left.",
                "findings", "Visible enamel erosion on tooth 21.",
                "treatment", "Applied fluoride varnish."
        );
        executePost("/api/v1/clinical-records/patient/" + patientId + "/evolutions", payload);
    }

    @When("clinical api adds an evolution with blank description")
    public void clinicalApiAddsEvolutionBlankDescription() throws Exception {
        Map<String, Object> payload = Map.of(
                "dentistId", dentistId.toString(),
                "description", ""
        );
        executePost("/api/v1/clinical-records/patient/" + patientId + "/evolutions", payload);
    }

    @When("clinical api adds a valid diagnosis")
    public void clinicalApiAddsValidDiagnosis() throws Exception {
        Map<String, Object> payload = Map.of(
                "code", "K02.1",
                "description", "Caries of dentine",
                "dentistId", dentistId.toString()
        );
        executePost("/api/v1/clinical-records/patient/" + patientId + "/diagnoses", payload);
    }

    @When("clinical api adds a diagnosis without code")
    public void clinicalApiAddsDiagnosisWithoutCode() throws Exception {
        Map<String, Object> payload = Map.of(
                "code", "",
                "description", "Some diagnosis"
        );
        executePost("/api/v1/clinical-records/patient/" + patientId + "/diagnoses", payload);
    }

    @When("clinical api adds a valid treatment plan")
    public void clinicalApiAddsValidTreatmentPlan() throws Exception {
        Map<String, Object> payload = Map.of(
                "dentistId", dentistId.toString(),
                "title", "Full mouth rehabilitation",
                "description", "Comprehensive treatment including extractions and prosthetics."
        );
        executePost("/api/v1/clinical-records/patient/" + patientId + "/treatment-plans", payload);
    }

    @When("clinical api fetches clinical record for unknown patient")
    public void clinicalApiFetchesUnknownPatient() throws Exception {
        executeGet("/api/v1/clinical-records/patient/00000000-0000-0000-0000-000000000000");
    }

    @Then("clinical response status should be {int}")
    public void clinicalResponseStatusShouldBe(int expected) {
        assertThat(lastStatus)
                .withFailMessage(
                        "Expected status %s but was %s. Body: %s. Exception: %s",
                        expected, lastStatus, lastBody,
                        lastException == null ? "<none>" : lastException.getMessage())
                .isEqualTo(expected);
    }

    @And("clinical response contains a patient id")
    public void clinicalResponseContainsPatientId() throws Exception {
        assertThat(parseData().path("patientId").asText()).isEqualTo(patientId.toString());
    }

    @And("clinical odontogram contains updated teeth")
    public void clinicalOdontogramContainsUpdatedTeeth() throws Exception {
        JsonNode teeth = parseData().path("odontogram");
        assertThat(teeth.isArray()).isTrue();
        assertThat(teeth.size()).isGreaterThanOrEqualTo(2);
    }

    @And("clinical response contains a tooth with space status OPEN")
    public void clinicalResponseContainsToothWithSpaceStatus() throws Exception {
        JsonNode teeth = parseData().path("odontogram");
        assertThat(teeth.isArray()).isTrue();
        boolean found = false;
        for (JsonNode tooth : teeth) {
            if ("OPEN".equals(tooth.path("spaceStatus").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found).withFailMessage("No tooth with spaceStatus=OPEN found in response").isTrue();
    }

    @And("clinical response dentition type is {string}")
    public void clinicalResponseDentitionTypeIs(String expected) throws Exception {
        assertThat(parseData().path("dentitionType").asText()).isEqualTo(expected);
    }

    @And("clinical response contains at least one evolution")
    public void clinicalResponseContainsEvolution() throws Exception {
        JsonNode evolutions = parseData().path("evolutions");
        assertThat(evolutions.isArray()).isTrue();
        assertThat(evolutions.size()).isGreaterThan(0);
    }

    @And("clinical response contains at least one diagnosis")
    public void clinicalResponseContainsDiagnosis() throws Exception {
        JsonNode diagnoses = parseData().path("diagnoses");
        assertThat(diagnoses.isArray()).isTrue();
        assertThat(diagnoses.size()).isGreaterThan(0);
    }

    @And("clinical response treatment plan status is {string}")
    public void clinicalResponseTreatmentPlanStatusIs(String expected) throws Exception {
        JsonNode plans = parseData().path("treatmentPlans");
        assertThat(plans.isArray()).isTrue();
        assertThat(plans.size()).isGreaterThan(0);
        assertThat(plans.get(0).path("status").asText()).isEqualTo(expected);
    }

    private void executeGet(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url)).andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
        lastException = result.getResolvedException();
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

    private void executePut(String url, Object payload) throws Exception {
        MvcResult result = mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
        lastException = result.getResolvedException();
    }

    private JsonNode parseData() throws Exception {
        return objectMapper.readTree(lastBody).path("data");
    }
}

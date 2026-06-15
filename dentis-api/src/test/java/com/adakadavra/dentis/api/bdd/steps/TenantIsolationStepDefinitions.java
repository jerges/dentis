package com.adakadavra.dentis.api.bdd.steps;

import com.adakadavra.dentis.api.bdd.support.TenantAuthSupport;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class TenantIsolationStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID clinicAId;
    private UUID clinicBId;
    private UUID patientAId;
    private UUID patientBId;
    private UUID createdPatientId;
    private int lastStatus;
    private String lastBody;

    @Before
    public void reset() {
        jdbcTemplate.execute("DELETE FROM clinical_evolutions");
        jdbcTemplate.execute("DELETE FROM diagnoses");
        jdbcTemplate.execute("DELETE FROM treatment_plans");
        jdbcTemplate.execute("DELETE FROM odontogram_teeth");
        jdbcTemplate.execute("DELETE FROM clinical_records");
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM budget_items");
        jdbcTemplate.execute("DELETE FROM budgets");
        jdbcTemplate.execute("DELETE FROM appointments");
        jdbcTemplate.execute("DELETE FROM patients");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM clinics");
        clinicAId = null;
        clinicBId = null;
        patientAId = null;
        patientBId = null;
        createdPatientId = null;
        lastStatus = 0;
        lastBody = null;
    }

    @Given("two clinics A and B each have one patient")
    public void twoClinicsEachHaveOnePatient() {
        clinicAId = UUID.randomUUID();
        clinicBId = UUID.randomUUID();
        insertClinic(clinicAId, "Clinic Alpha");
        insertClinic(clinicBId, "Clinic Beta");

        patientAId = UUID.randomUUID();
        patientBId = UUID.randomUUID();
        insertPatientWithClinic(patientAId, "Alpha", clinicAId);
        insertPatientWithClinic(patientBId, "Beta", clinicBId);
    }

    @Given("a clinic exists for tenant isolation test")
    public void aClinicExistsForTenantTest() {
        clinicAId = UUID.randomUUID();
        insertClinic(clinicAId, "Tenant Test Clinic");
    }

    @When("clinic A admin lists patients")
    public void clinicAAdminListsPatients() throws Exception {
        execute(get("/api/v1/patients?page=0&size=20"), clinicAId);
    }

    @When("clinic A admin fetches the patient from clinic B")
    public void clinicAAdminFetchesPatientFromClinicB() throws Exception {
        execute(get("/api/v1/patients/{id}", patientBId), clinicAId);
    }

    @When("clinic A admin fetches the clinical record of the patient from clinic B")
    public void clinicAAdminFetchesClinicalRecordFromClinicB() throws Exception {
        execute(get("/api/v1/clinical-records/patient/{id}", patientBId), clinicAId);
    }

    @When("super admin fetches the patient from clinic B")
    public void superAdminFetchesPatientFromClinicB() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/api/v1/patients/{id}", patientBId)
                        .with(TenantAuthSupport.asSuperAdmin()))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("clinic admin creates a patient via the api")
    public void clinicAdminCreatesPatient() throws Exception {
        Map<String, Object> payload = Map.of(
                "firstName", "Tenant",
                "lastName", "TestPatient",
                "idDocument", "TEN-" + UUID.randomUUID().toString().substring(0, 8),
                "birthDate", LocalDate.of(1995, 6, 20).toString(),
                "sex", "FEMALE",
                "gender", "FEMALE",
                "contactInfo", Map.of("email", "tenant.test@dentis.dev", "phoneNumber", "+58 412 0000001")
        );
        MvcResult result = mockMvc.perform(
                post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .with(TenantAuthSupport.asClinicAdmin(clinicAId)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
        if (lastStatus == 201) {
            JsonNode data = objectMapper.readTree(lastBody).path("data");
            createdPatientId = UUID.fromString(data.path("id").asText());
        }
    }

    @Then("tenant response status should be {int}")
    public void tenantResponseStatusShouldBe(int expected) {
        assertThat(lastStatus)
                .withFailMessage("Expected status %s but was %s. Body: %s", expected, lastStatus, lastBody)
                .isEqualTo(expected);
    }

    @And("patient list contains exactly {int} patient")
    public void patientListContainsExactly(int expected) throws Exception {
        JsonNode content = objectMapper.readTree(lastBody).path("data").path("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isEqualTo(expected);
    }

    @And("the created patient belongs to the clinic")
    public void createdPatientBelongsToClinic() {
        assertThat(createdPatientId).isNotNull();
        UUID storedClinicId = jdbcTemplate.queryForObject(
                "SELECT clinic_id FROM patients WHERE id = ?",
                UUID.class, createdPatientId);
        assertThat(storedClinicId).isEqualTo(clinicAId);
    }

    private void execute(MockHttpServletRequestBuilder builder, UUID clinicId) throws Exception {
        MvcResult result = mockMvc.perform(builder.with(TenantAuthSupport.asClinicAdmin(clinicId))).andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    private void insertClinic(UUID id, String name) {
        jdbcTemplate.update(
                "INSERT INTO clinics (id, name, nif, address, city, active, created_at) " +
                        "VALUES (?, ?, ?, 'Test Street', 'Caracas', true, NOW())",
                id, name, "NIF-" + id.toString().substring(0, 8));
    }

    private void insertPatientWithClinic(UUID patientId, String lastName, UUID clinicId) {
        jdbcTemplate.update(
                "INSERT INTO patients (id, first_name, last_name, id_document, birth_date, sex, gender, active, clinic_id, created_at) " +
                        "VALUES (?, 'Test', ?, ?, '1990-01-01', 'MALE', 'MALE', true, ?, NOW())",
                patientId, lastName, "PAT-" + patientId.toString().substring(0, 8), clinicId);
    }
}

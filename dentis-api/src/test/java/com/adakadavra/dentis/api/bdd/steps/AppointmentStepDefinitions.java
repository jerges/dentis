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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class AppointmentStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID patientId;
    private UUID dentistId;
    private UUID appointmentId;
    private LocalDateTime appointmentStart;
    private LocalDateTime appointmentEnd;
    private int lastStatus;
    private String lastBody;

    @Before
    public void reset() {
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM budget_items");
        jdbcTemplate.execute("DELETE FROM budgets");
        jdbcTemplate.execute("DELETE FROM appointments");
        jdbcTemplate.execute("DELETE FROM patients");
        patientId = null;
        dentistId = UUID.randomUUID();
        appointmentId = null;
        appointmentStart = null;
        appointmentEnd = null;
        lastStatus = 0;
        lastBody = null;
    }

    @Given("appointment api has an existing patient and dentist")
    public void appointmentApiHasPatientAndDentist() {
        patientId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO patients (id, first_name, last_name, id_document, birth_date, sex, gender, active, created_at) " +
                        "VALUES (?, 'App', 'Patient', ?, '1992-05-05', 'FEMALE', 'FEMALE', true, NOW())",
                patientId, "APD-" + patientId.toString().substring(0, 8));
    }

    @Given("appointment api has an existing scheduled appointment")
    public void appointmentApiHasScheduledAppointment() throws Exception {
        appointmentApiHasPatientAndDentist();
        appointmentStart = LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0);
        appointmentEnd = appointmentStart.plusMinutes(45);
        executePost("/api/v1/appointments", validPayload(appointmentStart, appointmentEnd));
        assertThat(lastStatus).isEqualTo(201);
        appointmentId = UUID.fromString(parseData().path("id").asText());
    }

    @When("appointment api schedules a valid appointment")
    public void appointmentApiSchedulesValidAppointment() throws Exception {
        appointmentStart = LocalDateTime.now().plusDays(2).withMinute(0).withSecond(0).withNano(0);
        appointmentEnd = appointmentStart.plusMinutes(30);
        executePost("/api/v1/appointments", validPayload(appointmentStart, appointmentEnd));
        if (lastStatus == 201) {
            appointmentId = UUID.fromString(parseData().path("id").asText());
        }
    }

    @When("appointment api fetches appointment by id")
    public void appointmentApiFetchesById() throws Exception {
        executeGet("/api/v1/appointments/" + appointmentId);
    }

    @When("appointment api confirms appointment")
    public void appointmentApiConfirmsAppointment() throws Exception {
        MvcResult result = mockMvc.perform(patch("/api/v1/appointments/{id}/confirm", appointmentId)).andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("appointment api cancels appointment")
    public void appointmentApiCancelsAppointment() throws Exception {
        MvcResult result = mockMvc.perform(patch("/api/v1/appointments/{id}/cancel", appointmentId)).andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("appointment api reschedules appointment to another valid slot")
    public void appointmentApiReschedulesAppointment() throws Exception {
        LocalDateTime newStart = appointmentStart.plusDays(1);
        LocalDateTime newEnd = newStart.plusMinutes(30);
        String url = "/api/v1/appointments/" + appointmentId + "/reschedule?newStart=" + iso(newStart) + "&newEnd=" + iso(newEnd);
        MvcResult result = mockMvc.perform(patch(url)).andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
        appointmentStart = newStart;
    }

    @When("appointment api lists appointments by patient")
    public void appointmentApiListsByPatient() throws Exception {
        executeGet("/api/v1/appointments/patient/" + patientId);
    }

    @When("appointment api lists appointments by dentist and date range")
    public void appointmentApiListsByDentistDateRange() throws Exception {
        LocalDateTime from = appointmentStart.minusHours(2);
        LocalDateTime to = appointmentEnd.plusHours(2);
        executeGet("/api/v1/appointments/dentist/" + dentistId + "?from=" + iso(from) + "&to=" + iso(to));
    }

    @When("appointment api schedules another appointment in conflicting slot")
    public void appointmentApiSchedulesConflicting() throws Exception {
        LocalDateTime start = appointmentStart.plusMinutes(10);
        LocalDateTime end = start.plusMinutes(30);
        executePost("/api/v1/appointments", validPayload(start, end));
    }

    @When("appointment api schedules appointment with end before start")
    public void appointmentApiSchedulesInvalidDuration() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(3).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.minusMinutes(30);
        executePost("/api/v1/appointments", validPayload(start, end));
    }

    @When("appointment api confirms a non-existent appointment")
    public void appointmentApiConfirmsNonExistent() throws Exception {
        MvcResult result = mockMvc.perform(
                patch("/api/v1/appointments/{id}/confirm", "00000000-0000-0000-0000-000000000000"))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("appointment api cancels a non-existent appointment")
    public void appointmentApiCancelsNonExistent() throws Exception {
        MvcResult result = mockMvc.perform(
                patch("/api/v1/appointments/{id}/cancel", "00000000-0000-0000-0000-000000000000"))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @Then("appointment response status should be {int}")
    public void appointmentResponseStatusShouldBe(int expected) {
        assertThat(lastStatus).isEqualTo(expected);
    }

    @Then("appointment cancel status should be {int}")
    public void appointmentCancelStatusShouldBe(int expected) {
        assertThat(lastStatus).isEqualTo(expected);
    }

    @And("appointment response status field is {string}")
    public void appointmentResponseStatusFieldIs(String expectedStatus) throws Exception {
        assertThat(parseData().path("status").asText()).isEqualTo(expectedStatus);
    }

    @And("appointment response id matches stored appointment id")
    public void appointmentResponseIdMatches() throws Exception {
        assertThat(parseData().path("id").asText()).isEqualTo(appointmentId.toString());
    }

    @And("appointment response start date is updated")
    public void appointmentStartDateUpdated() throws Exception {
        assertThat(parseData().path("startDateTime").asText()).contains(appointmentStart.toLocalDate().toString());
    }

    @And("appointment list contains at least one record")
    public void appointmentListContainsAtLeastOne() throws Exception {
        JsonNode data = parseData();
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThan(0);
    }

    private Map<String, Object> validPayload(LocalDateTime start, LocalDateTime end) {
        return Map.of(
                "patientId", patientId.toString(),
                "dentistId", dentistId.toString(),
                "startDateTime", start.toString(),
                "endDateTime", end.toString(),
                "consultationReason", "Routine check"
        );
    }

    private void executePost(String url, Object payload) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    private void executeGet(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url)).andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    private JsonNode parseData() throws Exception {
        return objectMapper.readTree(lastBody).path("data");
    }

    private String iso(LocalDateTime value) {
        return value.format(DateTimeFormatter.ISO_DATE_TIME);
    }
}


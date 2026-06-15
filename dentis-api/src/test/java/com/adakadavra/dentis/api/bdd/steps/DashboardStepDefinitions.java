package com.adakadavra.dentis.api.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class DashboardStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int lastStatus;
    private String lastBody;

    @Before
    public void reset() {
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM budget_items");
        jdbcTemplate.execute("DELETE FROM budgets");
        jdbcTemplate.execute("DELETE FROM appointments");
        jdbcTemplate.execute("DELETE FROM patients");
        lastStatus = 0;
        lastBody = null;
    }

    @Given("dashboard has {int} patients and 1 payment of {double}")
    public void dashboardHasPatientsAndPayment(int count, double amount) {
        UUID tariffId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tariffs (id, code, name, category, base_price, discount_allowed, active) " +
                        "VALUES (?, ?, 'Dash Treatment', 'GENERAL_DENTISTRY', 1000.00, false, true)",
                tariffId, "DSH-" + tariffId.toString().substring(0, 6));

        UUID firstPatientId = null;
        for (int i = 0; i < count; i++) {
            UUID patientId = UUID.randomUUID();
            if (i == 0) firstPatientId = patientId;
            jdbcTemplate.update(
                    "INSERT INTO patients (id, first_name, last_name, id_document, birth_date, sex, gender, active, created_at) " +
                            "VALUES (?, 'Dash', 'Patient" + i + "', ?, '1990-01-01', 'MALE', 'MALE', true, NOW())",
                    patientId, "DSH-" + patientId.toString().substring(0, 8));
        }

        UUID budgetId = UUID.randomUUID();
        UUID dentistId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO budgets (id, patient_id, dentist_id, status, created_at) VALUES (?, ?, ?, 'APPROVED', NOW())",
                budgetId, firstPatientId, dentistId);

        UUID itemId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO budget_items (id, budget_id, tariff_id, description, quantity, unit_price, discount_percentage, performed) " +
                        "VALUES (?, ?, ?, 'Dash item', 1, ?, 0.0, false)",
                itemId, budgetId, tariffId, BigDecimal.valueOf(amount));

        UUID paymentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO payments (id, budget_id, patient_id, amount, payment_method, paid_at) " +
                        "VALUES (?, ?, ?, ?, 'CASH', NOW())",
                paymentId, budgetId, firstPatientId, BigDecimal.valueOf(amount));
    }

    @When("dashboard api fetches stats")
    public void dashboardApiFetchesStats() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/dashboard/stats")).andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @Then("dashboard response status should be {int}")
    public void dashboardResponseStatusShouldBe(int expected) {
        assertThat(lastStatus)
                .withFailMessage("Expected status %s but was %s. Body: %s", expected, lastStatus, lastBody)
                .isEqualTo(expected);
    }

    @And("dashboard response contains stats fields")
    public void dashboardResponseContainsStatsFields() throws Exception {
        JsonNode data = parseData();
        assertThat(data.has("totalPatients")).isTrue();
        assertThat(data.has("todayAppointments")).isTrue();
        assertThat(data.has("weekAppointments")).isTrue();
        assertThat(data.has("totalRevenue")).isTrue();
    }

    @And("dashboard total patients is at least {int}")
    public void dashboardTotalPatientsAtLeast(int min) throws Exception {
        long total = parseData().path("totalPatients").asLong();
        assertThat(total).isGreaterThanOrEqualTo(min);
    }

    @And("dashboard total revenue is at least {double}")
    public void dashboardTotalRevenueAtLeast(double min) throws Exception {
        BigDecimal revenue = parseData().path("totalRevenue").decimalValue();
        assertThat(revenue).isGreaterThanOrEqualTo(BigDecimal.valueOf(min));
    }

    @And("dashboard total revenue is {double}")
    public void dashboardTotalRevenueIs(double expected) throws Exception {
        BigDecimal revenue = parseData().path("totalRevenue").decimalValue();
        assertThat(revenue).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }

    private JsonNode parseData() throws Exception {
        return objectMapper.readTree(lastBody).path("data");
    }
}

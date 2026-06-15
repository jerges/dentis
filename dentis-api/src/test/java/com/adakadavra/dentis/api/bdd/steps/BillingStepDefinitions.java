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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Cucumber step definitions for the BillingController integration tests.
 * State is reset before each scenario via {@link #cleanDatabase()}.
 */
public class BillingStepDefinitions {

    // ── Spring beans ──────────────────────────────────────────────────────────
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // ── Scenario state ────────────────────────────────────────────────────────
    private UUID patientId;
    private UUID tariffId;
    private UUID dentistId;
    private UUID budgetId;
    private int lastStatus;
    private String lastBody;
    private Exception lastException;

    // ── Setup / teardown ──────────────────────────────────────────────────────

    @Before
    public void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM budget_items");
        jdbcTemplate.execute("DELETE FROM budgets");
        jdbcTemplate.execute("DELETE FROM tariffs");
        jdbcTemplate.execute("DELETE FROM patients");
        patientId = null;
        tariffId = null;
        dentistId = UUID.randomUUID();
        budgetId = null;
        lastStatus = 0;
        lastBody = null;
        lastException = null;
    }

    // ── Background ────────────────────────────────────────────────────────────

    @Given("a tariff and a patient exist in the database")
    public void aTariffAndPatientExist() {
        patientId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO patients (id, first_name, last_name, id_document, birth_date, sex, gender, active, created_at) " +
                        "VALUES (?, 'Test', 'Patient', ?, '1990-01-01', 'MALE', 'MALE', true, NOW())",
                patientId, "DOC-" + patientId.toString().substring(0, 8));

        tariffId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tariffs (id, code, name, category, base_price, discount_allowed, active) " +
                        "VALUES (?, ?, 'Test Treatment', 'GENERAL_DENTISTRY', 200.00, true, true)",
                tariffId, "TST-" + tariffId.toString().substring(0, 6));
    }

    // ── Budget Given steps ────────────────────────────────────────────────────

    @Given("a budget has been created")
    public void aBudgetHasBeenCreated() throws Exception {
        createDraftBudget();
        assertThat(budgetId).isNotNull();
    }

    @Given("an approved budget exists")
    public void anApprovedBudgetExists() throws Exception {
        createDraftBudget();
        approveBudgetRequest();
        assertThat(lastStatus).isEqualTo(200);
    }

    @Given("an approved budget exists with a registered payment of {double}")
    public void anApprovedBudgetExistsWithPayment(double amount) throws Exception {
        anApprovedBudgetExists();
        registerPayment(BigDecimal.valueOf(amount));
        assertThat(lastStatus).isEqualTo(201);
    }

    // ── Budget When steps ─────────────────────────────────────────────────────

    @When("I create a budget with valid items")
    public void iCreateABudgetWithValidItems() throws Exception {
        executePost("/api/v1/billing/budgets", buildBudgetPayload());
    }

    @When("I get the budget by its ID")
    public void iGetTheBudgetById() throws Exception {
        executeGet("/api/v1/billing/budgets/" + budgetId);
    }

    @When("I get budget with unknown ID")
    public void iGetBudgetWithUnknownId() throws Exception {
        executeGet("/api/v1/billing/budgets/00000000-0000-0000-0000-000000000000");
    }

    @When("I approve the budget")
    public void iApproveTheBudget() throws Exception {
        approveBudgetRequest();
    }

    @When("I get the budget summary")
    public void iGetTheBudgetSummary() throws Exception {
        executeGet("/api/v1/billing/budgets/" + budgetId + "/summary");
    }

    @When("I list budgets for the patient")
    public void iListBudgetsForThePatient() throws Exception {
        executeGet("/api/v1/billing/budgets/patient/" + patientId);
    }

    // ── Payment When steps ────────────────────────────────────────────────────

    @When("I register a payment of {double} for the budget")
    public void iRegisterAPaymentOfAmountForTheBudget(double amount) throws Exception {
        registerPayment(BigDecimal.valueOf(amount));
    }

    @When("I register a payment exceeding the budget balance")
    public void iRegisterAPaymentExceedingBudgetBalance() throws Exception {
        registerPayment(new BigDecimal("99999.00"));
    }

    @When("I list payments for the budget")
    public void iListPaymentsForTheBudget() throws Exception {
        executeGet("/api/v1/billing/payments/budget/" + budgetId);
    }

    @When("I list payments for the patient")
    public void iListPaymentsForThePatient() throws Exception {
        executeGet("/api/v1/billing/payments/patient/" + patientId);
    }

    @When("I approve a non-existent budget")
    public void iApproveNonExistentBudget() throws Exception {
        MvcResult result = mockMvc.perform(
                patch("/api/v1/billing/budgets/{id}/approve", "00000000-0000-0000-0000-000000000000"))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
        lastException = result.getResolvedException();
    }

    // ── Then steps ────────────────────────────────────────────────────────────

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        assertThat(lastStatus)
                .withFailMessage(
                        "Expected status %s but was %s. Response body: %s. Resolved exception: %s",
                        expectedStatus,
                        lastStatus,
                        lastBody,
                        lastException == null ? "<none>" : lastException.getClass().getName() + " - " + lastException.getMessage())
                .isEqualTo(expectedStatus);
    }

    @And("the budget status in response should be {string}")
    public void theBudgetStatusInResponseShouldBe(String expectedStatus) throws Exception {
        JsonNode data = parseData();
        assertThat(data.path("status").asText()).isEqualTo(expectedStatus);
    }

    @And("the response contains the budget data")
    public void theResponseContainsTheBudgetData() throws Exception {
        JsonNode data = parseData();
        assertThat(data.path("id").asText()).isEqualTo(budgetId.toString());
    }

    @And("the summary shows a pending payment status")
    public void theSummaryShowsPendingPaymentStatus() throws Exception {
        JsonNode data = parseData();
        assertThat(data.path("status").asText()).isEqualTo("PENDING");
        assertThat(data.path("totalPaid").decimalValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @And("the budget list is not empty")
    public void theBudgetListIsNotEmpty() throws Exception {
        JsonNode data = parseData();
        // Page response wraps content in "content" array
        JsonNode content = data.has("content") ? data.path("content") : data;
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isGreaterThan(0);
    }

    @And("the payment amount in response is {double}")
    public void thePaymentAmountInResponseIs(double expected) throws Exception {
        JsonNode data = parseData();
        assertThat(data.path("amount").decimalValue())
                .isEqualByComparingTo(BigDecimal.valueOf(expected));
    }

    @And("the payment list contains {int} item")
    public void thePaymentListContainsItems(int expectedCount) throws Exception {
        JsonNode data = parseData();
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(expectedCount);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void createDraftBudget() throws Exception {
        executePost("/api/v1/billing/budgets", buildBudgetPayload());
        assertThat(lastStatus).isEqualTo(201);
        JsonNode data = parseData();
        budgetId = UUID.fromString(data.path("id").asText());
    }

    private void approveBudgetRequest() throws Exception {
        MvcResult result = mockMvc.perform(patch("/api/v1/billing/budgets/{id}/approve", budgetId))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
        lastException = result.getResolvedException();
    }

    private void registerPayment(BigDecimal amount) throws Exception {
        Map<String, Object> payload = Map.of(
                "patientId", patientId.toString(),
                "budgetId", budgetId.toString(),
                "amount", amount,
                "paymentMethod", "CASH"
        );
        executePost("/api/v1/billing/payments", payload);
    }

    private void executeGet(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
        lastException = result.getResolvedException();
    }

    private void executePost(String url, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    private Map<String, Object> buildBudgetPayload() {
        return Map.of(
                "patientId", patientId.toString(),
                "dentistId", dentistId.toString(),
                "items", List.of(Map.of(
                        "tariffId", tariffId.toString(),
                        "description", "Test dental procedure",
                        "quantity", 1,
                        "unitPrice", 200.00,
                        "discountPercentage", 0.00
                )),
                "notes", "Cucumber integration test"
        );
    }

    private JsonNode parseData() throws Exception {
        JsonNode root = objectMapper.readTree(lastBody);
        return root.path("data");
    }
}


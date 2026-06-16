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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class DocumentStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // ── Scenario state ────────────────────────────────────────────────────────
    private UUID clinicAId;
    private UUID clinicBId;
    private UUID kbFolderId;
    private int lastStatus;
    private String lastBody;

    // ── Setup / teardown ──────────────────────────────────────────────────────

    @Before
    public void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM document_shares");
        jdbcTemplate.execute("DELETE FROM document_files");
        jdbcTemplate.execute("DELETE FROM document_folders");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM clinics");
        clinicAId = null;
        clinicBId = null;
        kbFolderId = null;
        lastStatus = 0;
        lastBody = null;
    }

    // ── Given ─────────────────────────────────────────────────────────────────

    @Given("a clinic exists for documents tests")
    public void aClinicExistsForDocumentsTests() {
        clinicAId = UUID.randomUUID();
        insertClinic(clinicAId, "Clinic Docs A");
    }

    @Given("the knowledge base folder exists for the clinic")
    public void kbFolderExistsForClinic() {
        kbFolderId = UUID.randomUUID();
        insertFolder(kbFolderId, clinicAId, "Base de Conocimiento",
                "KNOWLEDGE_BASE", "PRIVATE", true);
    }

    @Given("clinic B has a folder named {string}")
    public void clinicBHasFolder(String name) {
        clinicBId = UUID.randomUUID();
        insertClinic(clinicBId, "Clinic Docs B");
        insertFolder(UUID.randomUUID(), clinicBId, name, "NORMAL", "PUBLIC", false);
    }

    @Given("clinic admin creates a PRIVATE folder named {string}")
    public void clinicAdminCreatesPrivateFolder(String name) throws Exception {
        createFolderViaApi(name, "PRIVATE", clinicAId);
    }

    @Given("clinic admin creates a PUBLIC folder named {string}")
    public void clinicAdminCreatesPublicFolder(String name) throws Exception {
        createFolderViaApi(name, "PUBLIC", clinicAId);
    }

    // ── When ──────────────────────────────────────────────────────────────────

    @When("clinic admin creates a folder named {string}")
    public void clinicAdminCreatesFolder(String name) throws Exception {
        Map<String, Object> body = Map.of("name", name, "visibility", "PRIVATE");
        MvcResult result = mockMvc.perform(
                post("/api/v1/documents/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(TenantAuthSupport.asClinicAdmin(clinicAId)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("clinic admin lists root folders")
    public void clinicAdminListsRootFolders() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/api/v1/documents/folders")
                        .with(TenantAuthSupport.asClinicAdmin(clinicAId)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("clinic user requests the knowledge base folder")
    public void clinicUserRequestsKb() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/api/v1/documents/kb")
                        .with(TenantAuthSupport.asClinicUser(clinicAId)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("clinic admin requests the knowledge base folder")
    public void clinicAdminRequestsKb() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/api/v1/documents/kb")
                        .with(TenantAuthSupport.asClinicAdmin(clinicAId)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("clinic admin deletes the knowledge base folder")
    public void clinicAdminDeletesKbFolder() throws Exception {
        MvcResult result = mockMvc.perform(
                delete("/api/v1/documents/folders/{id}", kbFolderId)
                        .with(TenantAuthSupport.asClinicAdmin(clinicAId)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("clinic A admin lists root folders")
    public void clinicAAdminListsRootFolders() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/api/v1/documents/folders")
                        .with(TenantAuthSupport.asClinicAdmin(clinicAId)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("super admin lists folders for clinic B")
    public void superAdminListsFoldersForClinicB() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/api/v1/documents/folders?clinicId={clinicId}", clinicBId)
                        .with(TenantAuthSupport.asSuperAdmin()))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    @When("another user of same clinic lists root folders")
    public void anotherUserListsRootFolders() throws Exception {
        UUID anotherUserId = UUID.randomUUID();
        MvcResult result = mockMvc.perform(
                get("/api/v1/documents/folders")
                        .with(TenantAuthSupport.asClinicUser(clinicAId)))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    // ── Then / And ────────────────────────────────────────────────────────────

    @Then("documents response status should be {int}")
    public void documentsResponseStatusShouldBe(int expected) {
        assertThat(lastStatus)
                .withFailMessage("Expected %d but was %d. Body: %s", expected, lastStatus, lastBody)
                .isEqualTo(expected);
    }

    @And("the folder name in response is {string}")
    public void folderNameInResponseIs(String expectedName) throws Exception {
        JsonNode data = objectMapper.readTree(lastBody).path("data");
        assertThat(data.path("name").asText()).isEqualTo(expectedName);
    }

    @And("the folder type in response is {string}")
    public void folderTypeInResponseIs(String expectedType) throws Exception {
        JsonNode data = objectMapper.readTree(lastBody).path("data");
        assertThat(data.path("type").asText()).isEqualTo(expectedType);
    }

    @And("the folder list contains {string}")
    public void folderListContains(String name) throws Exception {
        JsonNode data = objectMapper.readTree(lastBody).path("data");
        assertThat(data.isArray()).isTrue();
        boolean found = false;
        for (JsonNode node : data) {
            if (name.equals(node.path("name").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found)
                .withFailMessage("Folder '%s' not found in response: %s", name, lastBody)
                .isTrue();
    }

    @And("the folder list does not contain {string}")
    public void folderListDoesNotContain(String name) throws Exception {
        JsonNode data = objectMapper.readTree(lastBody).path("data");
        assertThat(data.isArray()).isTrue();
        for (JsonNode node : data) {
            assertThat(node.path("name").asText())
                    .withFailMessage("Folder '%s' should NOT be in response but was found: %s", name, lastBody)
                    .isNotEqualTo(name);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void createFolderViaApi(String name, String visibility, UUID clinicId) throws Exception {
        Map<String, Object> body = Map.of("name", name, "visibility", visibility);
        mockMvc.perform(
                post("/api/v1/documents/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(TenantAuthSupport.asClinicAdmin(clinicId)))
                .andReturn();
    }

    private void insertClinic(UUID id, String name) {
        jdbcTemplate.update(
                "INSERT INTO clinics (id, name, nif, address, city, active, created_at) " +
                        "VALUES (?, ?, ?, 'Test Street', 'Caracas', true, NOW())",
                id, name, "NIF-" + id.toString().substring(0, 8));
    }

    private void insertFolder(UUID id, UUID clinicId, String name,
                               String type, String visibility, boolean system) {
        jdbcTemplate.update(
                "INSERT INTO document_folders " +
                        "(id, clinic_id, name, path, type, visibility, system, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                id, clinicId, name, "/" + name, type, visibility, system);
    }
}

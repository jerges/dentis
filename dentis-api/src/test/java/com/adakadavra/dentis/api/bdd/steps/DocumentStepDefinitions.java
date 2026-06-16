package com.adakadavra.dentis.api.bdd.steps;

import com.adakadavra.dentis.api.bdd.support.TenantAuthSupport;
import com.adakadavra.dentis.api.security.entity.User;
import com.adakadavra.dentis.api.security.entity.UserRole;
import com.adakadavra.dentis.api.security.entity.UserStaffType;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

public class DocumentStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UUID clinicId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    private UUID folderId;
    private UUID kbFolderId;
    private UUID documentId;
    private int lastStatus;
    private String lastBody;

    @Before
    public void reset() {
        jdbcTemplate.execute("DELETE FROM clinic_documents");
        jdbcTemplate.execute("DELETE FROM document_folders");
        folderId = null;
        kbFolderId = null;
        documentId = null;
        lastStatus = 0;
        lastBody = null;
    }

    // ── Given ────────────────────────────────────────────────────────────────

    @Given("documents api has a clinic with knowledge-base initialized")
    public void hasKnowledgeBaseInitialized() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/api/v1/documents/folders/kb/init")
                        .with(TenantAuthSupport.asClinicAdmin(clinicId)))
                .andReturn();
        kbFolderId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").isMissingNode()
                ? null
                : UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                        .path("id").asText());
    }

    @Given("documents api has a GENERAL folder named {string}")
    public void hasGeneralFolder(String name) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/api/v1/documents/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("parentId", "", "name", name, "zone", "GENERAL")))
                        .with(TenantAuthSupport.asClinicAdmin(clinicId)))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        folderId = UUID.fromString(objectMapper.readTree(body).path("id").asText());
    }

    @Given("documents api has a document {string} in that folder")
    public void hasDocumentInFolder(String fileName) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "folderId", folderId.toString(),
                                "fileName", fileName,
                                "contentType", "application/pdf",
                                "s3Key", "clinics/" + clinicId + "/general/" + UUID.randomUUID() + "-" + fileName,
                                "fileSize", 102400,
                                "description", ""
                        )))
                        .with(TenantAuthSupport.asClinicAdmin(clinicId)))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        documentId = UUID.fromString(objectMapper.readTree(body).path("id").asText());
    }

    // ── When ─────────────────────────────────────────────────────────────────

    @When("documents api initializes the knowledge-base root as ADMIN")
    public void initKnowledgeBase() throws Exception {
        execute(post("/api/v1/documents/folders/kb/init")
                .with(TenantAuthSupport.asClinicAdmin(clinicId)));
    }

    @When("documents api creates a GENERAL folder named {string}")
    public void createGeneralFolder(String name) throws Exception {
        execute(post("/api/v1/documents/folders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("parentId", "", "name", name, "zone", "GENERAL", "visibility", "PUBLIC")))
                .with(TenantAuthSupport.asClinicAdmin(clinicId)));
    }

    @When("documents api creates a PRIVATE GENERAL folder named {string}")
    public void createPrivateGeneralFolder(String name) throws Exception {
        execute(post("/api/v1/documents/folders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("parentId", "", "name", name, "zone", "GENERAL", "visibility", "PRIVATE")))
                .with(TenantAuthSupport.asClinicAdmin(clinicId)));
    }

    @When("documents api creates a KNOWLEDGE_BASE folder named {string}")
    public void createKbFolder(String name) throws Exception {
        execute(post("/api/v1/documents/folders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("parentId", "", "name", name, "zone", "KNOWLEDGE_BASE", "visibility", "PUBLIC")))
                .with(TenantAuthSupport.asClinicAdmin(clinicId)));
    }

    @When("documents api user tries to create a KNOWLEDGE_BASE folder named {string}")
    public void userTriesToCreateKbFolder(String name) throws Exception {
        execute(post("/api/v1/documents/folders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("parentId", "", "name", name, "zone", "KNOWLEDGE_BASE")))
                .with(asClinicUser(clinicId)));
    }

    @When("documents api lists root folders as ADMIN")
    public void listRootFolders() throws Exception {
        execute(get("/api/v1/documents/folders")
                .with(TenantAuthSupport.asClinicAdmin(clinicId)));
    }

    @When("documents api deletes that folder as ADMIN")
    public void deleteThatFolder() throws Exception {
        execute(delete("/api/v1/documents/folders/" + folderId)
                .with(TenantAuthSupport.asClinicAdmin(clinicId)));
    }

    @When("documents api deletes the system knowledge-base folder as ADMIN")
    public void deleteSystemKbFolder() throws Exception {
        execute(delete("/api/v1/documents/folders/" + kbFolderId)
                .with(TenantAuthSupport.asClinicAdmin(clinicId)));
    }

    @When("documents api registers a document {string} in that folder")
    public void registerDocument(String fileName) throws Exception {
        execute(post("/api/v1/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "folderId", folderId.toString(),
                        "fileName", fileName,
                        "contentType", "application/pdf",
                        "s3Key", "clinics/" + clinicId + "/general/" + UUID.randomUUID() + "-" + fileName,
                        "fileSize", 51200,
                        "description", "",
                        "visibility", "PUBLIC"
                )))
                .with(TenantAuthSupport.asClinicAdmin(clinicId)));
    }

    @When("documents api registers a PRIVATE document {string} in that folder")
    public void registerPrivateDocument(String fileName) throws Exception {
        execute(post("/api/v1/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "folderId", folderId.toString(),
                        "fileName", fileName,
                        "contentType", "application/pdf",
                        "s3Key", "clinics/" + clinicId + "/general/" + UUID.randomUUID() + "-" + fileName,
                        "fileSize", 51200,
                        "description", "",
                        "visibility", "PRIVATE"
                )))
                .with(TenantAuthSupport.asClinicAdmin(clinicId)));
    }

    @When("documents api lists documents in that folder")
    public void listDocumentsInFolder() throws Exception {
        execute(get("/api/v1/documents")
                .param("folderId", folderId.toString())
                .with(TenantAuthSupport.asClinicAdmin(clinicId)));
    }

    @When("documents api searches for {string}")
    public void searchDocuments(String query) throws Exception {
        execute(get("/api/v1/documents/search")
                .param("q", query)
                .with(TenantAuthSupport.asClinicAdmin(clinicId)));
    }

    // ── Then / And ────────────────────────────────────────────────────────────

    @Then("documents response status should be {int}")
    public void responseStatusShouldBe(int expected) {
        assertThat(lastStatus)
                .withFailMessage("Expected status %d but got %d. Body: %s", expected, lastStatus, lastBody)
                .isEqualTo(expected);
    }

    @And("documents response contains a folder named {string}")
    public void responseFolderNameIs(String expected) throws Exception {
        assertThat(objectMapper.readTree(lastBody).path("name").asText()).isEqualTo(expected);
    }

    @And("documents response folder list is not empty")
    public void responseFolderListNotEmpty() throws Exception {
        JsonNode node = objectMapper.readTree(lastBody);
        assertThat(node.isArray()).isTrue();
        assertThat(node.size()).isGreaterThan(0);
    }

    @And("documents document response has fileName {string}")
    public void documentResponseFileName(String expected) throws Exception {
        assertThat(objectMapper.readTree(lastBody).path("fileName").asText()).isEqualTo(expected);
    }

    @And("documents response document list is not empty")
    public void responseDocumentListNotEmpty() throws Exception {
        JsonNode node = objectMapper.readTree(lastBody);
        assertThat(node.isArray()).isTrue();
        assertThat(node.size()).isGreaterThan(0);
    }

    @And("documents response folder visibility is {string}")
    public void responseFolderVisibilityIs(String expected) throws Exception {
        assertThat(objectMapper.readTree(lastBody).path("visibility").asText()).isEqualTo(expected);
    }

    @And("documents document visibility is {string}")
    public void documentVisibilityIs(String expected) throws Exception {
        assertThat(objectMapper.readTree(lastBody).path("visibility").asText()).isEqualTo(expected);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void execute(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
            throws Exception {
        MvcResult result = mockMvc.perform(builder).andReturn();
        lastStatus = result.getResponse().getStatus();
        lastBody = result.getResponse().getContentAsString();
    }

    private static RequestPostProcessor asClinicUser(UUID clinicId) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("clinic-user-" + clinicId.toString().substring(0, 8))
                .email("user@bdd.test")
                .password("n/a")
                .role(UserRole.USER)
                .staffType(UserStaffType.ADMINISTRATIVE)
                .clinicId(clinicId)
                .fullName("BDD USER")
                .active(true)
                .build();
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}

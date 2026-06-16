package com.adakadavra.dentis.api;

import com.adakadavra.dentis.api.controller.IaController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the full Spring application context loads without errors.
 * This test catches wiring bugs (missing beans, bad @Configuration, etc.)
 * that unit tests miss because they never start the ApplicationContext.
 * <p>
 * Run with: mvn test -Dtest=ApplicationContextSmokeTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"dev", "test"})
class ApplicationContextSmokeTest {

    // Bedrock clients require AWS credentials at call-time, not build-time,
    // but mocking them keeps the test self-contained and credential-free.
    @MockitoBean
    BedrockRuntimeClient bedrockRuntimeClient;

    @MockitoBean
    BedrockRuntimeAsyncClient bedrockRuntimeAsyncClient;

    private static final List<String> MIGRATION_SCRIPTS = List.of(
            "db/changelog/changes/001-baseline.sql",
            "db/changelog/changes/002-clinical-enhancements.sql",
            "db/changelog/changes/004-odontogram-root-and-tooth-dx.sql",
            "db/changelog/changes/005-clinical-attachments.sql",
            "db/changelog/changes/006-ia-vector.sql",
            "db/changelog/changes/007-ia-spring-ai-vector.sql",
            "db/changelog/changes/008-documents.sql"
    );

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("dentis_smoke")
                    .withUsername("dentis")
                    .withPassword("dentis");

    static {
        POSTGRES.start();
        initSchema();
    }

    @DynamicPropertySource
    static void registerDataSource(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static void initSchema() {
        try (final Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            for (final String script : MIGRATION_SCRIPTS) {
                ScriptUtils.executeSqlScript(conn, new ClassPathResource(script));
            }
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to initialize smoke test schema", e);
        }
    }

    @Autowired
    ApplicationContext context;

    @Autowired
    IaController iaController;

    @Test
    @DisplayName("shouldBeApplicationContextLoadedWithoutErrors")
    void shouldBeApplicationContextLoadedWithoutErrors() {
        assertThat(this.context).isNotNull();
    }

    @Test
    @DisplayName("shouldBeIaControllerPresentInContext")
    void shouldBeIaControllerPresentInContext() {
        // Este test habría fallado con el bug de ObjectMapper no disponible como bean.
        // Verifica explícitamente que IaController es creado por Spring sin errores.
        assertThat(this.iaController).isNotNull();
    }
}

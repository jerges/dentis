package com.adakadavra.dentis.api.bdd.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Configures the Spring Boot test context for all Cucumber scenarios.
 * Starts a single PostgreSQL Testcontainer for the entire test run and
 * sets datasource properties via system properties before Spring reads them.
 */
@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles({"dev", "test"})
public class CucumberSpringConfig {

    private static final List<String> MIGRATION_SCRIPTS = List.of(
            "db/changelog/changes/001-baseline.sql",
            "db/changelog/changes/002-clinical-enhancements.sql",
            "db/changelog/changes/004-odontogram-root-and-tooth-dx.sql",
            "db/changelog/changes/005-clinical-attachments.sql",
            "db/changelog/changes/006-ia-vector.sql",
            "db/changelog/changes/007-ia-spring-ai-vector.sql",
            "db/changelog/changes/008-patient-document-type.sql",
            "db/changelog/changes/009-documents.sql"
    );

    // pgvector/pgvector includes the vector extension required by migration 006
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("dentis_test")
                    .withUsername("dentis")
                    .withPassword("dentis");

    static {
        POSTGRES.start();
        initializeSchema();
    }

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static void initializeSchema() {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword())) {
            for (String script : MIGRATION_SCRIPTS) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource(script));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initialize the Cucumber test database", ex);
        }
    }

    @TestConfiguration
    static class MockMvcTestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        MockMvc mockMvc(WebApplicationContext context) {
            return MockMvcBuilders.webAppContextSetup(context)
                    .apply(springSecurity())
                    .build();
        }
    }
}

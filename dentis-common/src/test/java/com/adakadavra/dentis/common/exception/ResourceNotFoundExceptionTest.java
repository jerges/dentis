package com.adakadavra.dentis.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResourceNotFoundException")
class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("should be NOT_FOUND with RESOURCE_NOT_FOUND code")
    void shouldBeNotFoundWithResourceNotFoundCode() {
        UUID id = UUID.randomUUID();
        ResourceNotFoundException ex = new ResourceNotFoundException("Patient", id);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("should include resource name and id in message")
    void shouldIncludeResourceNameAndIdInMessage() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ResourceNotFoundException ex = new ResourceNotFoundException("Appointment", id);

        assertThat(ex.getMessage()).contains("Appointment");
        assertThat(ex.getMessage()).contains("11111111-1111-1111-1111-111111111111");
    }

    @Test
    @DisplayName("should work with non-UUID identifiers")
    void shouldWorkWithNonUuidIdentifiers() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Tariff", "T-001");

        assertThat(ex.getMessage()).contains("Tariff");
        assertThat(ex.getMessage()).contains("T-001");
    }

    @Test
    @DisplayName("should be a DentisException")
    void shouldBeADentisException() {
        assertThat(new ResourceNotFoundException("X", UUID.randomUUID())).isInstanceOf(DentisException.class);
    }
}

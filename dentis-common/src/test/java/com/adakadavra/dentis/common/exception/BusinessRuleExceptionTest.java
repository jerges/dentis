package com.adakadavra.dentis.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessRuleException")
class BusinessRuleExceptionTest {

    @Test
    @DisplayName("should be UNPROCESSABLE_ENTITY with default error code when created with message only")
    void shouldBeUnprocessableEntityWithDefaultCode() {
        BusinessRuleException ex = new BusinessRuleException("rule violated");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.getErrorCode()).isEqualTo("BUSINESS_RULE_VIOLATION");
        assertThat(ex.getMessage()).isEqualTo("rule violated");
    }

    @Test
    @DisplayName("should use custom error code when provided")
    void shouldUseCustomErrorCode() {
        BusinessRuleException ex = new BusinessRuleException("conflict", "SCHEDULING_CONFLICT");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.getErrorCode()).isEqualTo("SCHEDULING_CONFLICT");
        assertThat(ex.getMessage()).isEqualTo("conflict");
    }

    @Test
    @DisplayName("should be a DentisException")
    void shouldBeADentisException() {
        assertThat(new BusinessRuleException("test")).isInstanceOf(DentisException.class);
    }
}

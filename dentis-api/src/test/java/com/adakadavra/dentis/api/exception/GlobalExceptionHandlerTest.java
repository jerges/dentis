package com.adakadavra.dentis.api.exception;

import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import com.adakadavra.dentis.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("handleDentisException")
    class HandleDentisException {

        @Test
        @DisplayName("should return 422 with business rule code for BusinessRuleException")
        void shouldReturn422ForBusinessRuleException() {
            BusinessRuleException ex = new BusinessRuleException("budget not approved", "BUDGET_NOT_APPROVED");

            ResponseEntity<ApiResponse<Void>> response = handler.handleDentisException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("BUDGET_NOT_APPROVED");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("budget not approved");
        }

        @Test
        @DisplayName("should return 404 with RESOURCE_NOT_FOUND code for ResourceNotFoundException")
        void shouldReturn404ForResourceNotFoundException() {
            UUID id = UUID.randomUUID();
            ResourceNotFoundException ex = new ResourceNotFoundException("Patient", id);

            ResponseEntity<ApiResponse<Void>> response = handler.handleDentisException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getError().getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("handleValidationException")
    class HandleValidationException {

        @Test
        @DisplayName("should return 400 with field errors for validation failures")
        void shouldReturn400WithFieldErrors() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "patientId", "must not be null"));
            bindingResult.addError(new FieldError("request", "startDateTime", "must be a future date"));

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getError().getCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getBody().getError().getFieldErrors()).hasSize(2);
            assertThat(response.getBody().getError().getFieldErrors())
                    .extracting("field")
                    .containsExactlyInAnyOrder("patientId", "startDateTime");
        }

        @Test
        @DisplayName("should return 400 even when no field errors are present")
        void shouldReturn400WhenNoFieldErrors() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getError().getFieldErrors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("handleAccessDeniedException")
    class HandleAccessDeniedException {

        @Test
        @DisplayName("should return 403 with ACCESS_DENIED code")
        void shouldReturn403() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleAccessDeniedException(new AccessDeniedException("denied"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().getError().getCode()).isEqualTo("ACCESS_DENIED");
        }
    }

    @Nested
    @DisplayName("handleHttpMessageNotReadableException")
    class HandleHttpMessageNotReadable {

        @Test
        @DisplayName("should return 400 with INVALID_REQUEST_BODY code")
        void shouldReturn400() {
            HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleHttpMessageNotReadableException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getError().getCode()).isEqualTo("INVALID_REQUEST_BODY");
        }
    }

    @Nested
    @DisplayName("handleIllegalArgument")
    class HandleIllegalArgument {

        @Test
        @DisplayName("should return 400 for IllegalArgumentException")
        void shouldReturn400ForIllegalArgumentException() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleIllegalArgument(new IllegalArgumentException("bad argument"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getError().getCode()).isEqualTo("INVALID_ARGUMENT");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("bad argument");
        }

        @Test
        @DisplayName("should return 400 for IllegalStateException")
        void shouldReturn400ForIllegalStateException() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleIllegalArgument(new IllegalStateException("bad state"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("handleNoSuchElement")
    class HandleNoSuchElement {

        @Test
        @DisplayName("should return 404 with NOT_FOUND code")
        void shouldReturn404() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleNoSuchElement(new NoSuchElementException("not found"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getError().getCode()).isEqualTo("NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("handleDataIntegrity")
    class HandleDataIntegrity {

        @Test
        @DisplayName("should return 409 with DATA_CONFLICT code")
        void shouldReturn409() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleDataIntegrity(new DataIntegrityViolationException("duplicate key"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().getError().getCode()).isEqualTo("DATA_CONFLICT");
        }
    }

    @Nested
    @DisplayName("handleTypeMismatch")
    class HandleTypeMismatch {

        @Test
        @DisplayName("should return 400 with TYPE_MISMATCH code and parameter name")
        void shouldReturn400WithParameterName() {
            MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
            when(ex.getName()).thenReturn("clinicId");
            when(ex.getMessage()).thenReturn("Failed to convert");

            ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatch(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getError().getCode()).isEqualTo("TYPE_MISMATCH");
            assertThat(response.getBody().getError().getMessage()).contains("clinicId");
        }
    }

    @Nested
    @DisplayName("handleMissingParam")
    class HandleMissingParam {

        @Test
        @DisplayName("should return 400 with MISSING_PARAMETER code and parameter name")
        void shouldReturn400WithMissingParameterName() {
            MissingServletRequestParameterException ex =
                    new MissingServletRequestParameterException("dentistId", "UUID");

            ResponseEntity<ApiResponse<Void>> response = handler.handleMissingParam(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getError().getCode()).isEqualTo("MISSING_PARAMETER");
            assertThat(response.getBody().getError().getMessage()).contains("dentistId");
        }
    }

    @Nested
    @DisplayName("handleResponseStatusException")
    class HandleResponseStatusException {

        @Test
        @DisplayName("should return the status from the exception")
        void shouldReturnStatusFromException() {
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "resource gone");

            ResponseEntity<ApiResponse<Void>> response = handler.handleResponseStatusException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getError().getMessage()).isEqualTo("resource gone");
        }
    }

    @Nested
    @DisplayName("handleGenericException")
    class HandleGenericException {

        @Test
        @DisplayName("should return 500 with INTERNAL_ERROR code for unexpected exceptions")
        void shouldReturn500ForUnexpectedException() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleGenericException(new RuntimeException("something exploded"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
        }
    }
}

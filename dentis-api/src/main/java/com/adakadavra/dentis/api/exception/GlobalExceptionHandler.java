package com.adakadavra.dentis.api.exception;

import com.adakadavra.dentis.common.exception.DentisException;
import com.adakadavra.dentis.common.response.ApiError;
import com.adakadavra.dentis.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DentisException.class)
    public ResponseEntity<ApiResponse<Void>> handleDentisException(DentisException ex) {
        log.warn("Business exception: [{}] {}", ex.getErrorCode(), ex.getMessage());
        ApiError error = ApiError.builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getAllErrors().stream()
                .filter(e -> e instanceof FieldError)
                .map(e -> (FieldError) e)
                .map(e -> ApiError.FieldError.builder()
                        .field(e.getField())
                        .message(e.getDefaultMessage())
                        .build())
                .toList();

        ApiError error = ApiError.builder()
                .code("VALIDATION_ERROR")
                .message("Request validation failed")
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        ApiError error = ApiError.builder().code("ACCESS_DENIED").message("Access denied").build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        ApiError error = ApiError.builder().code("INTERNAL_ERROR").message("An unexpected error occurred").build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error));
    }
}

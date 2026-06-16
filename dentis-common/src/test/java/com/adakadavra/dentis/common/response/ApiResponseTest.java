package com.adakadavra.dentis.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse")
class ApiResponseTest {

    @Nested
    @DisplayName("ok(data)")
    class OkWithData {

        @Test
        @DisplayName("should be successful with data when created with ok(data)")
        void shouldBeSuccessfulWithData() {
            ApiResponse<String> response = ApiResponse.ok("payload");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo("payload");
            assertThat(response.getError()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("should allow null data")
        void shouldAllowNullData() {
            ApiResponse<String> response = ApiResponse.ok(null);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNull();
        }
    }

    @Nested
    @DisplayName("ok(data, message)")
    class OkWithDataAndMessage {

        @Test
        @DisplayName("should carry message when created with ok(data, message)")
        void shouldCarryMessage() {
            ApiResponse<String> response = ApiResponse.ok("payload", "Operation successful");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo("payload");
            assertThat(response.getMessage()).isEqualTo("Operation successful");
            assertThat(response.getError()).isNull();
        }
    }

    @Nested
    @DisplayName("error(apiError)")
    class ErrorResponse {

        @Test
        @DisplayName("should be unsuccessful with error when created with error()")
        void shouldBeUnsuccessfulWithError() {
            ApiError error = ApiError.builder()
                    .code("SOME_ERROR")
                    .message("Something went wrong")
                    .build();

            ApiResponse<Void> response = ApiResponse.error(error);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getError()).isEqualTo(error);
            assertThat(response.getError().getCode()).isEqualTo("SOME_ERROR");
            assertThat(response.getData()).isNull();
            assertThat(response.getMessage()).isNull();
        }

        @Test
        @DisplayName("should have timestamp even for error responses")
        void shouldHaveTimestampEvenForErrors() {
            ApiError error = ApiError.builder().code("ERR").message("msg").build();

            ApiResponse<Void> response = ApiResponse.error(error);

            assertThat(response.getTimestamp()).isNotNull();
        }
    }
}

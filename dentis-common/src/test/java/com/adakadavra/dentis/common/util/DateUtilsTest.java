package com.adakadavra.dentis.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DateUtils")
class DateUtilsTest {

    @Nested
    @DisplayName("calculateAge")
    class CalculateAge {

        @Test
        @DisplayName("should be 0 when born today")
        void shouldBeZeroWhenBornToday() {
            assertThat(DateUtils.calculateAge(LocalDate.now())).isEqualTo(0);
        }

        @Test
        @DisplayName("should be correct age for a known birthdate")
        void shouldBeCorrectAgeForKnownBirthdate() {
            LocalDate birthDate = LocalDate.now().minusYears(30);
            assertThat(DateUtils.calculateAge(birthDate)).isEqualTo(30);
        }

        @Test
        @DisplayName("should be one year less when birthday has not yet passed this year")
        void shouldBeOneLessWhenBirthdayNotYetPassed() {
            LocalDate birthDate = LocalDate.now().plusDays(1).minusYears(18);
            assertThat(DateUtils.calculateAge(birthDate)).isEqualTo(17);
        }

        @Test
        @DisplayName("should count exact year when birthday is today")
        void shouldCountExactYearWhenBirthdayIsToday() {
            LocalDate birthDate = LocalDate.now().minusYears(18);
            assertThat(DateUtils.calculateAge(birthDate)).isEqualTo(18);
        }
    }

    @Nested
    @DisplayName("isAdult")
    class IsAdult {

        @Test
        @DisplayName("should be true when age is exactly 18")
        void shouldBeTrueWhenAgeIsExactly18() {
            LocalDate birthDate = LocalDate.now().minusYears(18);
            assertThat(DateUtils.isAdult(birthDate)).isTrue();
        }

        @Test
        @DisplayName("should be true when age is over 18")
        void shouldBeTrueWhenAgeIsOver18() {
            LocalDate birthDate = LocalDate.now().minusYears(25);
            assertThat(DateUtils.isAdult(birthDate)).isTrue();
        }

        @Test
        @DisplayName("should be false when age is 17")
        void shouldBeFalseWhenAgeIs17() {
            LocalDate birthDate = LocalDate.now().minusYears(17);
            assertThat(DateUtils.isAdult(birthDate)).isFalse();
        }

        @Test
        @DisplayName("should be false when birthday has not yet passed this year at age 18")
        void shouldBeFalseWhenBirthdayNotPassedAtAge18() {
            LocalDate birthDate = LocalDate.now().plusDays(1).minusYears(18);
            assertThat(DateUtils.isAdult(birthDate)).isFalse();
        }

        @Test
        @DisplayName("should be false when born today")
        void shouldBeFalseWhenBornToday() {
            assertThat(DateUtils.isAdult(LocalDate.now())).isFalse();
        }
    }
}

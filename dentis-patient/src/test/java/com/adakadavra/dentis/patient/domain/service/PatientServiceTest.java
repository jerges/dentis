package com.adakadavra.dentis.patient.domain.service;

import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import com.adakadavra.dentis.patient.application.dto.ContactInfoDto;
import com.adakadavra.dentis.patient.application.dto.CreatePatientRequest;
import com.adakadavra.dentis.patient.application.dto.PatientResponse;
import com.adakadavra.dentis.patient.application.mapper.PatientMapper;
import com.adakadavra.dentis.patient.domain.model.ContactInfo;
import com.adakadavra.dentis.patient.domain.model.Gender;
import com.adakadavra.dentis.patient.domain.model.Patient;
import com.adakadavra.dentis.patient.domain.model.Sex;
import com.adakadavra.dentis.patient.domain.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatientService")
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientMapper patientMapper;

    @InjectMocks
    private PatientService patientService;

    private Patient samplePatient;
    private PatientResponse sampleResponse;
    private CreatePatientRequest createRequest;

    @BeforeEach
    void setUp() {
        UUID patientId = UUID.randomUUID();

        samplePatient = Patient.builder()
                .id(patientId)
                .firstName("Maria")
                .lastName("Gonzalez")
                .idDocument("V-12345678")
                .birthDate(LocalDate.of(1990, 5, 15))
                .sex(Sex.FEMALE)
                .gender(Gender.FEMALE)
                .active(true)
                .contactInfo(ContactInfo.builder().email("maria@example.com").phoneNumber("0414-1234567").build())
                .build();

        sampleResponse = PatientResponse.builder()
                .id(patientId)
                .firstName("Maria")
                .lastName("Gonzalez")
                .fullName("Maria Gonzalez")
                .idDocument("V-12345678")
                .active(true)
                .build();

        createRequest = CreatePatientRequest.builder()
                .firstName("Maria")
                .lastName("Gonzalez")
                .idDocument("V-12345678")
                .birthDate(LocalDate.of(1990, 5, 15))
                .sex(Sex.FEMALE)
                .gender(Gender.FEMALE)
                .contactInfo(ContactInfoDto.builder().email("maria@example.com").build())
                .build();
    }

    @Nested
    @DisplayName("createPatient")
    class CreatePatient {

        @Test
        @DisplayName("should be created successfully when ID document is unique")
        void shouldBeCreatedSuccessfullyWhenIdDocumentIsUnique() {
            // Arrange
            when(patientRepository.existsByIdDocument("V-12345678")).thenReturn(false);
            when(patientMapper.toDomain(createRequest)).thenReturn(samplePatient);
            when(patientRepository.save(samplePatient)).thenReturn(samplePatient);
            when(patientMapper.toResponse(samplePatient)).thenReturn(sampleResponse);

            // Act
            PatientResponse result = patientService.createPatient(createRequest, null);

            // Assert
            assertThat(result).isEqualTo(sampleResponse);
            verify(patientRepository).save(samplePatient);
        }

        @Test
        @DisplayName("should be rejected when ID document already exists")
        void shouldBeRejectedWhenIdDocumentAlreadyExists() {
            // Arrange
            when(patientRepository.existsByIdDocument("V-12345678")).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> patientService.createPatient(createRequest, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("V-12345678");

            verify(patientRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should be returned when patient exists")
        void shouldBeReturnedWhenPatientExists() {
            // Arrange
            UUID id = samplePatient.getId();
            when(patientRepository.findById(id)).thenReturn(Optional.of(samplePatient));
            when(patientMapper.toResponse(samplePatient)).thenReturn(sampleResponse);

            // Act
            PatientResponse result = patientService.findById(id);

            // Assert
            assertThat(result).isEqualTo(sampleResponse);
        }

        @Test
        @DisplayName("should be rejected when patient does not exist")
        void shouldBeRejectedWhenPatientDoesNotExist() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(patientRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> patientService.findById(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should be mapped when listing patients")
        void shouldBeMappedWhenListingPatients() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Patient> patientPage = new PageImpl<>(List.of(samplePatient), pageable, 1);
            when(patientRepository.findAll(pageable)).thenReturn(patientPage);
            when(patientMapper.toResponse(samplePatient)).thenReturn(sampleResponse);

            // Act
            Page<PatientResponse> result = patientService.findAll(pageable);

            // Assert
            assertThat(result.getContent()).containsExactly(sampleResponse);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("searchByName")
    class SearchByName {

        @Test
        @DisplayName("should be mapped when searching patients by name")
        void shouldBeMappedWhenSearchingPatientsByName() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Patient> patientPage = new PageImpl<>(List.of(samplePatient), pageable, 1);
            when(patientRepository.searchByName("Maria", pageable)).thenReturn(patientPage);
            when(patientMapper.toResponse(samplePatient)).thenReturn(sampleResponse);

            // Act
            Page<PatientResponse> result = patientService.searchByName("Maria", pageable);

            // Assert
            assertThat(result.getContent()).containsExactly(sampleResponse);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("deactivatePatient")
    class DeactivatePatient {

        @Test
        @DisplayName("should be deactivated when patient exists")
        void shouldBeDeactivatedWhenPatientExists() {
            // Arrange
            UUID id = samplePatient.getId();
            when(patientRepository.findById(id)).thenReturn(Optional.of(samplePatient));

            // Act
            patientService.deactivatePatient(id);

            // Assert
            verify(patientRepository).deactivate(id);
        }

        @Test
        @DisplayName("should be rejected when deactivating a non-existent patient")
        void shouldBeRejectedWhenDeactivatingANonExistentPatient() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(patientRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> patientService.deactivatePatient(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(patientRepository, never()).deactivate(any());
        }
    }
}

package com.adakadavra.dentis.patient.domain.service;

import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import com.adakadavra.dentis.patient.application.dto.*;
import com.adakadavra.dentis.patient.application.mapper.PatientMapper;
import com.adakadavra.dentis.patient.domain.model.*;
import com.adakadavra.dentis.patient.domain.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        @DisplayName("should create patient successfully when ID document is unique")
        void shouldCreatePatientSuccessfully() {
            when(patientRepository.existsByIdDocument("V-12345678")).thenReturn(false);
            when(patientMapper.toDomain(createRequest)).thenReturn(samplePatient);
            when(patientRepository.save(samplePatient)).thenReturn(samplePatient);
            when(patientMapper.toResponse(samplePatient)).thenReturn(sampleResponse);

            PatientResponse result = patientService.createPatient(createRequest);

            assertThat(result).isEqualTo(sampleResponse);
            verify(patientRepository).save(samplePatient);
        }

        @Test
        @DisplayName("should throw BusinessRuleException when ID document already exists")
        void shouldThrowWhenIdDocumentDuplicated() {
            when(patientRepository.existsByIdDocument("V-12345678")).thenReturn(true);

            assertThatThrownBy(() -> patientService.createPatient(createRequest))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("V-12345678");

            verify(patientRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return patient when found")
        void shouldReturnPatientWhenFound() {
            UUID id = samplePatient.getId();
            when(patientRepository.findById(id)).thenReturn(Optional.of(samplePatient));
            when(patientMapper.toResponse(samplePatient)).thenReturn(sampleResponse);

            PatientResponse result = patientService.findById(id);

            assertThat(result).isEqualTo(sampleResponse);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when patient does not exist")
        void shouldThrowWhenPatientNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(patientRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.findById(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deactivatePatient")
    class DeactivatePatient {

        @Test
        @DisplayName("should deactivate patient when found")
        void shouldDeactivatePatient() {
            UUID id = samplePatient.getId();
            when(patientRepository.findById(id)).thenReturn(Optional.of(samplePatient));

            patientService.deactivatePatient(id);

            verify(patientRepository).deactivate(id);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when patient does not exist")
        void shouldThrowWhenDeactivatingNonExistentPatient() {
            UUID unknownId = UUID.randomUUID();
            when(patientRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.deactivatePatient(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(patientRepository, never()).deactivate(any());
        }
    }
}

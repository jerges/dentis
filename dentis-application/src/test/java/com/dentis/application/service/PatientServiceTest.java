package com.dentis.application.service;

import com.dentis.application.dto.request.CreatePatientRequest;
import com.dentis.application.dto.request.UpdatePatientRequest;
import com.dentis.application.dto.response.PatientResponse;
import com.dentis.application.mapper.PatientMapper;
import com.dentis.application.service.impl.PatientServiceImpl;
import com.dentis.common.exception.BusinessException;
import com.dentis.common.exception.ResourceNotFoundException;
import com.dentis.domain.entity.Patient;
import com.dentis.domain.enums.Gender;
import com.dentis.domain.repository.PatientRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatientService")
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientMapper patientMapper;

    @InjectMocks
    private PatientServiceImpl patientService;

    private Patient samplePatient;
    private PatientResponse sampleResponse;
    private CreatePatientRequest createRequest;

    @BeforeEach
    void setUp() {
        samplePatient = Patient.builder()
                .firstName("Carlos")
                .lastName("Gonzalez")
                .documentId("V-12345678")
                .birthDate(LocalDate.of(1990, 5, 15))
                .gender(Gender.MALE)
                .email("carlos@example.com")
                .active(true)
                .build();

        sampleResponse = PatientResponse.builder()
                .id("patient-id-1")
                .firstName("Carlos")
                .lastName("Gonzalez")
                .fullName("Carlos Gonzalez")
                .documentId("V-12345678")
                .email("carlos@example.com")
                .active(true)
                .build();

        createRequest = CreatePatientRequest.builder()
                .firstName("Carlos")
                .lastName("Gonzalez")
                .documentId("V-12345678")
                .birthDate(LocalDate.of(1990, 5, 15))
                .gender(Gender.MALE)
                .email("carlos@example.com")
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("should create patient when document ID is unique")
        void shouldCreatePatientWhenDocumentIdIsUnique() {
            when(patientRepository.existsByDocumentId(anyString())).thenReturn(false);
            when(patientMapper.toEntity(createRequest)).thenReturn(samplePatient);
            when(patientRepository.save(samplePatient)).thenReturn(samplePatient);
            when(patientMapper.toResponse(samplePatient)).thenReturn(sampleResponse);

            PatientResponse result = patientService.create(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getDocumentId()).isEqualTo("V-12345678");
            verify(patientRepository).save(samplePatient);
        }

        @Test
        @DisplayName("should throw BusinessException when document ID already exists")
        void shouldThrowWhenDocumentIdAlreadyExists() {
            when(patientRepository.existsByDocumentId("V-12345678")).thenReturn(true);

            assertThatThrownBy(() -> patientService.create(createRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("V-12345678");

            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create patient without document ID")
        void shouldCreatePatientWithoutDocumentId() {
            createRequest.setDocumentId(null);
            when(patientMapper.toEntity(createRequest)).thenReturn(samplePatient);
            when(patientRepository.save(samplePatient)).thenReturn(samplePatient);
            when(patientMapper.toResponse(samplePatient)).thenReturn(sampleResponse);

            PatientResponse result = patientService.create(createRequest);

            assertThat(result).isNotNull();
            verify(patientRepository, never()).existsByDocumentId(any());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("should return patient when found")
        void shouldReturnPatientWhenFound() {
            when(patientRepository.findById("patient-id-1")).thenReturn(Optional.of(samplePatient));
            when(patientMapper.toResponse(samplePatient)).thenReturn(sampleResponse);

            PatientResponse result = patientService.findById("patient-id-1");

            assertThat(result).isNotNull();
            assertThat(result.getFullName()).isEqualTo("Carlos Gonzalez");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(patientRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.findById("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        @Test
        @DisplayName("should deactivate patient")
        void shouldDeactivatePatient() {
            when(patientRepository.findById("patient-id-1")).thenReturn(Optional.of(samplePatient));
            when(patientRepository.save(samplePatient)).thenReturn(samplePatient);

            patientService.deactivate("patient-id-1");

            assertThat(samplePatient.isActive()).isFalse();
            verify(patientRepository).save(samplePatient);
        }

        @Test
        @DisplayName("should throw when patient not found")
        void shouldThrowWhenPatientNotFound() {
            when(patientRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.deactivate("bad-id"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

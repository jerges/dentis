package com.adakadavra.dentis.clinical.domain.service;

import com.adakadavra.dentis.clinical.domain.model.*;
import com.adakadavra.dentis.clinical.domain.repository.ClinicalRecordRepository;
import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClinicalRecordService")
class ClinicalRecordServiceTest {

    @Mock private ClinicalRecordRepository clinicalRecordRepository;

    @InjectMocks
    private ClinicalRecordService clinicalRecordService;

    private UUID patientId;
    private ClinicalRecord existingRecord;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        existingRecord = ClinicalRecord.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .dentitionType(DentitionType.PERMANENT)
                .odontogram(new ArrayList<>())
                .evolutions(new ArrayList<>())
                .diagnoses(new ArrayList<>())
                .treatmentPlans(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("getOrCreateForPatient")
    class GetOrCreate {

        @Test
        @DisplayName("should return existing record when found")
        void shouldReturnExistingRecord() {
            when(clinicalRecordRepository.findByPatientId(patientId)).thenReturn(Optional.of(existingRecord));

            ClinicalRecord result = clinicalRecordService.getOrCreateForPatient(patientId);

            assertThat(result).isEqualTo(existingRecord);
            verify(clinicalRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create new record when not found")
        void shouldCreateNewRecordWhenNotFound() {
            when(clinicalRecordRepository.findByPatientId(patientId)).thenReturn(Optional.empty());
            when(clinicalRecordRepository.existsByPatientId(patientId)).thenReturn(false);
            when(clinicalRecordRepository.save(any())).thenReturn(existingRecord);

            ClinicalRecord result = clinicalRecordService.getOrCreateForPatient(patientId);

            assertThat(result).isNotNull();
            verify(clinicalRecordRepository).save(any());
        }
    }

    @Nested
    @DisplayName("createForPatient")
    class CreateForPatient {

        @Test
        @DisplayName("should throw when clinical record already exists")
        void shouldThrowWhenDuplicateRecord() {
            when(clinicalRecordRepository.existsByPatientId(patientId)).thenReturn(true);

            assertThatThrownBy(() -> clinicalRecordService.createForPatient(patientId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("addEvolution")
    class AddEvolution {

        @Test
        @DisplayName("should append evolution to existing list")
        void shouldAppendEvolution() {
            when(clinicalRecordRepository.findByPatientId(patientId)).thenReturn(Optional.of(existingRecord));
            when(clinicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ClinicalEvolution evolution = ClinicalEvolution.builder()
                    .dentistId(UUID.randomUUID())
                    .description("Revisión post-operatoria")
                    .build();

            ClinicalRecord result = clinicalRecordService.addEvolution(patientId, evolution);

            assertThat(result.getEvolutions()).hasSize(1);
            assertThat(result.getEvolutions().get(0).getDescription()).isEqualTo("Revisión post-operatoria");
        }

        @Test
        @DisplayName("should throw when patient record does not exist")
        void shouldThrowWhenPatientRecordNotFound() {
            when(clinicalRecordRepository.findByPatientId(patientId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clinicalRecordService.addEvolution(patientId,
                    ClinicalEvolution.builder().dentistId(UUID.randomUUID()).description("X").build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateOdontogram")
    class UpdateOdontogram {

        @Test
        @DisplayName("should replace odontogram teeth list")
        void shouldReplaceOdontogramTeeth() {
            when(clinicalRecordRepository.findByPatientId(patientId)).thenReturn(Optional.of(existingRecord));
            when(clinicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<OdontogramTooth> teeth = List.of(
                    OdontogramTooth.builder().toothNumber(11).condition(ToothCondition.HEALTHY).build(),
                    OdontogramTooth.builder().toothNumber(21).condition(ToothCondition.CARIES).build()
            );

            ClinicalRecord result = clinicalRecordService.updateOdontogram(patientId, teeth);

            assertThat(result.getOdontogram()).hasSize(2);
            assertThat(result.getUpdatedAt()).isNotNull();
        }
    }
}

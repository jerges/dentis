package com.adakadavra.dentis.clinical.infrastructure.persistence.mapper;

import com.adakadavra.dentis.clinical.domain.model.*;
import com.adakadavra.dentis.clinical.infrastructure.persistence.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClinicalRecordEntityMapper")
class ClinicalRecordEntityMapperTest {

    private ClinicalRecordEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ClinicalRecordEntityMapper();
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should return null when record is null")
        void shouldReturnNullWhenRecordIsNull() {
            assertThat(mapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("should map scalar fields from domain to entity")
        void shouldMapScalarFields() {
            UUID id = UUID.randomUUID();
            UUID patientId = UUID.randomUUID();
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
            LocalDateTime updatedAt = LocalDateTime.now();

            ClinicalRecord record = ClinicalRecord.builder()
                    .id(id)
                    .patientId(patientId)
                    .dentitionType(DentitionType.PERMANENT)
                    .odontogram(List.of())
                    .evolutions(List.of())
                    .diagnoses(List.of())
                    .treatmentPlans(List.of())
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();

            ClinicalRecordEntity entity = mapper.toEntity(record);

            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getPatientId()).isEqualTo(patientId);
            assertThat(entity.getDentitionType()).isEqualTo(DentitionType.PERMANENT);
            assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
            assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        }

        @Test
        @DisplayName("should serialize single tooth surface to its name")
        void shouldSerializeSingleToothSurface() {
            OdontogramTooth tooth = OdontogramTooth.builder()
                    .id(UUID.randomUUID())
                    .toothNumber(11)
                    .condition(ToothCondition.CARIES)
                    .affectedSurfaces(Set.of(ToothSurface.BUCCAL))
                    .surfaceConditions(Map.of())
                    .rootFindings(Set.of())
                    .build();

            ClinicalRecord record = buildRecordWithTooth(tooth);
            ClinicalRecordEntity entity = mapper.toEntity(record);

            assertThat(entity.getOdontogram()).hasSize(1);
            assertThat(entity.getOdontogram().get(0).getAffectedSurfaces()).isEqualTo("BUCCAL");
        }

        @Test
        @DisplayName("should serialize multiple tooth surfaces as sorted comma-separated string")
        void shouldSerializeMultipleSurfacesSorted() {
            OdontogramTooth tooth = OdontogramTooth.builder()
                    .id(UUID.randomUUID())
                    .toothNumber(21)
                    .condition(ToothCondition.RESTORED)
                    .affectedSurfaces(new LinkedHashSet<>(List.of(ToothSurface.MESIAL, ToothSurface.OCCLUSAL, ToothSurface.DISTAL)))
                    .surfaceConditions(Map.of())
                    .rootFindings(Set.of())
                    .build();

            ClinicalRecord record = buildRecordWithTooth(tooth);
            String serialized = mapper.toEntity(record).getOdontogram().get(0).getAffectedSurfaces();

            assertThat(serialized).isEqualTo("DISTAL,MESIAL,OCCLUSAL");
        }

        @Test
        @DisplayName("should serialize null surfaces as null")
        void shouldSerializeNullSurfacesAsNull() {
            OdontogramTooth tooth = OdontogramTooth.builder()
                    .id(UUID.randomUUID())
                    .toothNumber(11)
                    .condition(ToothCondition.HEALTHY)
                    .affectedSurfaces(null)
                    .surfaceConditions(null)
                    .rootFindings(null)
                    .build();

            ClinicalRecord record = buildRecordWithTooth(tooth);
            OdontogramToothEntity toothEntity = mapper.toEntity(record).getOdontogram().get(0);

            assertThat(toothEntity.getAffectedSurfaces()).isNull();
            assertThat(toothEntity.getSurfaceConditions()).isNull();
            assertThat(toothEntity.getRootFindings()).isNull();
        }

        @Test
        @DisplayName("should serialize surface conditions as sorted KEY:VALUE pairs")
        void shouldSerializeSurfaceConditionsSorted() {
            Map<ToothSurface, ToothCondition> conditions = new LinkedHashMap<>();
            conditions.put(ToothSurface.OCCLUSAL, ToothCondition.CARIES);
            conditions.put(ToothSurface.BUCCAL, ToothCondition.RESTORED);

            OdontogramTooth tooth = OdontogramTooth.builder()
                    .id(UUID.randomUUID())
                    .toothNumber(16)
                    .condition(ToothCondition.CARIES)
                    .affectedSurfaces(Set.of())
                    .surfaceConditions(conditions)
                    .rootFindings(Set.of())
                    .build();

            ClinicalRecord record = buildRecordWithTooth(tooth);
            String serialized = mapper.toEntity(record).getOdontogram().get(0).getSurfaceConditions();

            assertThat(serialized).isEqualTo("BUCCAL:RESTORED,OCCLUSAL:CARIES");
        }

        @Test
        @DisplayName("should serialize root findings as sorted comma-separated string")
        void shouldSerializeRootFindingsSorted() {
            OdontogramTooth tooth = OdontogramTooth.builder()
                    .id(UUID.randomUUID())
                    .toothNumber(11)
                    .condition(ToothCondition.ROOT_CANAL)
                    .affectedSurfaces(Set.of())
                    .surfaceConditions(Map.of())
                    .rootFindings(new LinkedHashSet<>(List.of(RootFinding.PERIAPICAL_LESION, RootFinding.ENDODONTIC_TREATMENT)))
                    .build();

            ClinicalRecord record = buildRecordWithTooth(tooth);
            String serialized = mapper.toEntity(record).getOdontogram().get(0).getRootFindings();

            assertThat(serialized).isEqualTo("ENDODONTIC_TREATMENT,PERIAPICAL_LESION");
        }

        @Test
        @DisplayName("should map clinical evolution fields")
        void shouldMapEvolutionFields() {
            UUID dentistId = UUID.randomUUID();
            LocalDateTime recordedAt = LocalDateTime.now();

            ClinicalEvolution evolution = ClinicalEvolution.builder()
                    .id(UUID.randomUUID())
                    .clinicalRecordId(UUID.randomUUID())
                    .dentistId(dentistId)
                    .description("Revisión")
                    .findings("Sin hallazgos")
                    .treatment("Profilaxis")
                    .recordedAt(recordedAt)
                    .build();

            ClinicalRecord record = ClinicalRecord.builder()
                    .id(UUID.randomUUID())
                    .patientId(UUID.randomUUID())
                    .dentitionType(DentitionType.PERMANENT)
                    .odontogram(List.of())
                    .evolutions(List.of(evolution))
                    .diagnoses(List.of())
                    .treatmentPlans(List.of())
                    .createdAt(LocalDateTime.now())
                    .build();

            ClinicalRecordEntity entity = mapper.toEntity(record);

            assertThat(entity.getEvolutions()).hasSize(1);
            ClinicalEvolutionEntity evolutionEntity = entity.getEvolutions().get(0);
            assertThat(evolutionEntity.getDentistId()).isEqualTo(dentistId);
            assertThat(evolutionEntity.getDescription()).isEqualTo("Revisión");
            assertThat(evolutionEntity.getFindings()).isEqualTo("Sin hallazgos");
            assertThat(evolutionEntity.getTreatment()).isEqualTo("Profilaxis");
            assertThat(evolutionEntity.getRecordedAt()).isEqualTo(recordedAt);
            assertThat(evolutionEntity.getClinicalRecord()).isSameAs(entity);
        }

        @Test
        @DisplayName("should map diagnosis fields")
        void shouldMapDiagnosisFields() {
            UUID dentistId = UUID.randomUUID();
            LocalDate diagnosedAt = LocalDate.now();

            Diagnosis diagnosis = Diagnosis.builder()
                    .id(UUID.randomUUID())
                    .clinicalRecordId(UUID.randomUUID())
                    .code("K02.1")
                    .description("Caries de dentina")
                    .diagnosedAt(diagnosedAt)
                    .dentistId(dentistId)
                    .toothNumber(36)
                    .build();

            ClinicalRecord record = ClinicalRecord.builder()
                    .id(UUID.randomUUID())
                    .patientId(UUID.randomUUID())
                    .dentitionType(DentitionType.PERMANENT)
                    .odontogram(List.of())
                    .evolutions(List.of())
                    .diagnoses(List.of(diagnosis))
                    .treatmentPlans(List.of())
                    .createdAt(LocalDateTime.now())
                    .build();

            ClinicalRecordEntity entity = mapper.toEntity(record);

            assertThat(entity.getDiagnoses()).hasSize(1);
            DiagnosisEntity diagnosisEntity = entity.getDiagnoses().get(0);
            assertThat(diagnosisEntity.getCode()).isEqualTo("K02.1");
            assertThat(diagnosisEntity.getDescription()).isEqualTo("Caries de dentina");
            assertThat(diagnosisEntity.getDiagnosedAt()).isEqualTo(diagnosedAt);
            assertThat(diagnosisEntity.getDentistId()).isEqualTo(dentistId);
            assertThat(diagnosisEntity.getToothNumber()).isEqualTo(36);
            assertThat(diagnosisEntity.getClinicalRecord()).isSameAs(entity);
        }

        @Test
        @DisplayName("should map treatment plan with nested procedures")
        void shouldMapTreatmentPlanWithProcedures() {
            UUID dentistId = UUID.randomUUID();
            LocalDate startDate = LocalDate.now();

            TreatmentProcedure procedure = TreatmentProcedure.builder()
                    .id(UUID.randomUUID())
                    .treatmentPlanId(UUID.randomUUID())
                    .description("Obturación")
                    .toothNumber(16)
                    .performed(false)
                    .build();

            TreatmentPlan plan = TreatmentPlan.builder()
                    .id(UUID.randomUUID())
                    .clinicalRecordId(UUID.randomUUID())
                    .dentistId(dentistId)
                    .title("Plan de tratamiento inicial")
                    .description("Tratamiento completo")
                    .status(TreatmentPlanStatus.IN_PROGRESS)
                    .procedures(List.of(procedure))
                    .startDate(startDate)
                    .build();

            ClinicalRecord record = ClinicalRecord.builder()
                    .id(UUID.randomUUID())
                    .patientId(UUID.randomUUID())
                    .dentitionType(DentitionType.PERMANENT)
                    .odontogram(List.of())
                    .evolutions(List.of())
                    .diagnoses(List.of())
                    .treatmentPlans(List.of(plan))
                    .createdAt(LocalDateTime.now())
                    .build();

            ClinicalRecordEntity entity = mapper.toEntity(record);

            assertThat(entity.getTreatmentPlans()).hasSize(1);
            TreatmentPlanEntity planEntity = entity.getTreatmentPlans().get(0);
            assertThat(planEntity.getTitle()).isEqualTo("Plan de tratamiento inicial");
            assertThat(planEntity.getStatus()).isEqualTo(TreatmentPlanStatus.IN_PROGRESS);
            assertThat(planEntity.getStartDate()).isEqualTo(startDate);
            assertThat(planEntity.getClinicalRecord()).isSameAs(entity);
            assertThat(planEntity.getProcedures()).hasSize(1);
            assertThat(planEntity.getProcedures().get(0).getTreatmentPlan()).isSameAs(planEntity);
            assertThat(planEntity.getProcedures().get(0).getDescription()).isEqualTo("Obturación");
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("should return null when entity is null")
        void shouldReturnNullWhenEntityIsNull() {
            assertThat(mapper.toDomain(null)).isNull();
        }

        @Test
        @DisplayName("should deserialize single surface from serialized string")
        void shouldDeserializeSingleSurface() {
            ClinicalRecordEntity recordEntity = buildMinimalRecordEntity();
            OdontogramToothEntity toothEntity = OdontogramToothEntity.builder()
                    .id(UUID.randomUUID())
                    .clinicalRecord(recordEntity)
                    .toothNumber(11)
                    .condition(ToothCondition.CARIES)
                    .affectedSurfaces("BUCCAL")
                    .build();
            recordEntity.getOdontogram().add(toothEntity);

            ClinicalRecord domain = mapper.toDomain(recordEntity);

            assertThat(domain.getOdontogram().get(0).getAffectedSurfaces())
                    .containsExactlyInAnyOrder(ToothSurface.BUCCAL);
        }

        @Test
        @DisplayName("should deserialize multiple surfaces from sorted comma-separated string")
        void shouldDeserializeMultipleSurfaces() {
            ClinicalRecordEntity recordEntity = buildMinimalRecordEntity();
            OdontogramToothEntity toothEntity = OdontogramToothEntity.builder()
                    .id(UUID.randomUUID())
                    .clinicalRecord(recordEntity)
                    .toothNumber(21)
                    .condition(ToothCondition.RESTORED)
                    .affectedSurfaces("DISTAL,MESIAL,OCCLUSAL")
                    .build();
            recordEntity.getOdontogram().add(toothEntity);

            Set<ToothSurface> surfaces = mapper.toDomain(recordEntity).getOdontogram().get(0).getAffectedSurfaces();

            assertThat(surfaces).containsExactlyInAnyOrder(
                    ToothSurface.DISTAL, ToothSurface.MESIAL, ToothSurface.OCCLUSAL);
        }

        @Test
        @DisplayName("should return empty set when affectedSurfaces is null")
        void shouldReturnEmptySetWhenSurfacesNull() {
            ClinicalRecordEntity recordEntity = buildMinimalRecordEntity();
            OdontogramToothEntity toothEntity = OdontogramToothEntity.builder()
                    .id(UUID.randomUUID())
                    .clinicalRecord(recordEntity)
                    .toothNumber(11)
                    .condition(ToothCondition.HEALTHY)
                    .affectedSurfaces(null)
                    .build();
            recordEntity.getOdontogram().add(toothEntity);

            Set<ToothSurface> surfaces = mapper.toDomain(recordEntity).getOdontogram().get(0).getAffectedSurfaces();

            assertThat(surfaces).isEmpty();
        }

        @Test
        @DisplayName("should deserialize surface conditions from KEY:VALUE string")
        void shouldDeserializeSurfaceConditions() {
            ClinicalRecordEntity recordEntity = buildMinimalRecordEntity();
            OdontogramToothEntity toothEntity = OdontogramToothEntity.builder()
                    .id(UUID.randomUUID())
                    .clinicalRecord(recordEntity)
                    .toothNumber(16)
                    .condition(ToothCondition.CARIES)
                    .surfaceConditions("BUCCAL:RESTORED,OCCLUSAL:CARIES")
                    .build();
            recordEntity.getOdontogram().add(toothEntity);

            Map<ToothSurface, ToothCondition> conditions =
                    mapper.toDomain(recordEntity).getOdontogram().get(0).getSurfaceConditions();

            assertThat(conditions)
                    .containsEntry(ToothSurface.BUCCAL, ToothCondition.RESTORED)
                    .containsEntry(ToothSurface.OCCLUSAL, ToothCondition.CARIES);
        }

        @Test
        @DisplayName("should deserialize root findings from comma-separated string")
        void shouldDeserializeRootFindings() {
            ClinicalRecordEntity recordEntity = buildMinimalRecordEntity();
            OdontogramToothEntity toothEntity = OdontogramToothEntity.builder()
                    .id(UUID.randomUUID())
                    .clinicalRecord(recordEntity)
                    .toothNumber(11)
                    .condition(ToothCondition.ROOT_CANAL)
                    .rootFindings("ENDODONTIC_TREATMENT,PERIAPICAL_LESION")
                    .build();
            recordEntity.getOdontogram().add(toothEntity);

            Set<RootFinding> findings = mapper.toDomain(recordEntity).getOdontogram().get(0).getRootFindings();

            assertThat(findings).containsExactlyInAnyOrder(
                    RootFinding.ENDODONTIC_TREATMENT, RootFinding.PERIAPICAL_LESION);
        }

        @Test
        @DisplayName("should map evolution with clinicalRecordId set from parent")
        void shouldMapEvolutionWithClinicalRecordId() {
            ClinicalRecordEntity recordEntity = buildMinimalRecordEntity();
            ClinicalEvolutionEntity evolutionEntity = ClinicalEvolutionEntity.builder()
                    .id(UUID.randomUUID())
                    .clinicalRecord(recordEntity)
                    .dentistId(UUID.randomUUID())
                    .description("Revisión")
                    .recordedAt(LocalDateTime.now())
                    .build();
            recordEntity.getEvolutions().add(evolutionEntity);

            ClinicalRecord domain = mapper.toDomain(recordEntity);

            assertThat(domain.getEvolutions()).hasSize(1);
            assertThat(domain.getEvolutions().get(0).getClinicalRecordId()).isEqualTo(recordEntity.getId());
            assertThat(domain.getEvolutions().get(0).getDescription()).isEqualTo("Revisión");
        }

        @Test
        @DisplayName("should return empty collections when entity collections are null")
        void shouldReturnEmptyCollectionsWhenNull() {
            ClinicalRecordEntity entity = ClinicalRecordEntity.builder()
                    .id(UUID.randomUUID())
                    .patientId(UUID.randomUUID())
                    .dentitionType(DentitionType.PERMANENT)
                    .createdAt(LocalDateTime.now())
                    .odontogram(null)
                    .evolutions(null)
                    .diagnoses(null)
                    .treatmentPlans(null)
                    .build();

            ClinicalRecord domain = mapper.toDomain(entity);

            assertThat(domain.getOdontogram()).isEmpty();
            assertThat(domain.getEvolutions()).isEmpty();
            assertThat(domain.getDiagnoses()).isEmpty();
            assertThat(domain.getTreatmentPlans()).isEmpty();
        }
    }

    private ClinicalRecord buildRecordWithTooth(OdontogramTooth tooth) {
        return ClinicalRecord.builder()
                .id(UUID.randomUUID())
                .patientId(UUID.randomUUID())
                .dentitionType(DentitionType.PERMANENT)
                .odontogram(List.of(tooth))
                .evolutions(List.of())
                .diagnoses(List.of())
                .treatmentPlans(List.of())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ClinicalRecordEntity buildMinimalRecordEntity() {
        return ClinicalRecordEntity.builder()
                .id(UUID.randomUUID())
                .patientId(UUID.randomUUID())
                .dentitionType(DentitionType.PERMANENT)
                .createdAt(LocalDateTime.now())
                .odontogram(new ArrayList<>())
                .evolutions(new ArrayList<>())
                .diagnoses(new ArrayList<>())
                .treatmentPlans(new ArrayList<>())
                .build();
    }
}

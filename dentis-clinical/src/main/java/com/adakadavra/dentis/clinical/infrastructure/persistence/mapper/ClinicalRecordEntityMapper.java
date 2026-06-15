package com.adakadavra.dentis.clinical.infrastructure.persistence.mapper;

import com.adakadavra.dentis.clinical.domain.model.*;
import com.adakadavra.dentis.clinical.infrastructure.persistence.entity.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ClinicalRecordEntityMapper {

    public ClinicalRecordEntity toEntity(ClinicalRecord record) {
        if (record == null) {
            return null;
        }

        ClinicalRecordEntity entity = ClinicalRecordEntity.builder()
                .id(record.getId())
                .patientId(record.getPatientId())
                .dentitionType(record.getDentitionType())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .odontogram(new ArrayList<>())
                .evolutions(new ArrayList<>())
                .diagnoses(new ArrayList<>())
                .treatmentPlans(new ArrayList<>())
                .build();

        if (record.getOdontogram() != null) {
            for (OdontogramTooth tooth : record.getOdontogram()) {
                entity.getOdontogram().add(toToothEntity(tooth, entity));
            }
        }

        if (record.getEvolutions() != null) {
            for (ClinicalEvolution evolution : record.getEvolutions()) {
                entity.getEvolutions().add(toEvolutionEntity(evolution, entity));
            }
        }

        if (record.getDiagnoses() != null) {
            for (Diagnosis diagnosis : record.getDiagnoses()) {
                entity.getDiagnoses().add(toDiagnosisEntity(diagnosis, entity));
            }
        }

        if (record.getTreatmentPlans() != null) {
            for (TreatmentPlan treatmentPlan : record.getTreatmentPlans()) {
                entity.getTreatmentPlans().add(toTreatmentPlanEntity(treatmentPlan, entity));
            }
        }

        return entity;
    }

    public ClinicalRecord toDomain(ClinicalRecordEntity entity) {
        if (entity == null) {
            return null;
        }

        List<OdontogramTooth> odontogram = entity.getOdontogram() == null
                ? List.of()
                : entity.getOdontogram().stream().map(this::toToothDomain).toList();

        List<ClinicalEvolution> evolutions = entity.getEvolutions() == null
                ? List.of()
                : entity.getEvolutions().stream().map(this::toEvolutionDomain).toList();

        List<Diagnosis> diagnoses = entity.getDiagnoses() == null
                ? List.of()
                : entity.getDiagnoses().stream().map(this::toDiagnosisDomain).toList();

        List<TreatmentPlan> treatmentPlans = entity.getTreatmentPlans() == null
                ? List.of()
                : entity.getTreatmentPlans().stream().map(this::toTreatmentPlanDomain).toList();

        return ClinicalRecord.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .dentitionType(entity.getDentitionType())
                .odontogram(odontogram)
                .evolutions(evolutions)
                .diagnoses(diagnoses)
                .treatmentPlans(treatmentPlans)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private OdontogramToothEntity toToothEntity(OdontogramTooth tooth, ClinicalRecordEntity recordEntity) {
        return OdontogramToothEntity.builder()
                .id(tooth.getId())
                .clinicalRecord(recordEntity)
                .toothNumber(tooth.getToothNumber())
                .condition(tooth.getCondition())
                .affectedSurfaces(serializeSurfaces(tooth.getAffectedSurfaces()))
                .surfaceConditions(serializeSurfaceConditions(tooth.getSurfaceConditions()))
                .spaceStatus(tooth.getSpaceStatus())
                .notes(tooth.getNotes())
                .rootFindings(serializeRootFindings(tooth.getRootFindings()))
                .rootNotes(tooth.getRootNotes())
                .build();
    }

    private OdontogramTooth toToothDomain(OdontogramToothEntity entity) {
        return OdontogramTooth.builder()
                .id(entity.getId())
                .toothNumber(entity.getToothNumber())
                .condition(entity.getCondition())
                .affectedSurfaces(deserializeSurfaces(entity.getAffectedSurfaces()))
                .surfaceConditions(deserializeSurfaceConditions(entity.getSurfaceConditions()))
                .spaceStatus(entity.getSpaceStatus())
                .notes(entity.getNotes())
                .rootFindings(deserializeRootFindings(entity.getRootFindings()))
                .rootNotes(entity.getRootNotes())
                .build();
    }

    private ClinicalEvolutionEntity toEvolutionEntity(ClinicalEvolution evolution, ClinicalRecordEntity recordEntity) {
        return ClinicalEvolutionEntity.builder()
                .id(evolution.getId())
                .clinicalRecord(recordEntity)
                .dentistId(evolution.getDentistId())
                .description(evolution.getDescription())
                .findings(evolution.getFindings())
                .treatment(evolution.getTreatment())
                .recordedAt(evolution.getRecordedAt())
                .build();
    }

    private ClinicalEvolution toEvolutionDomain(ClinicalEvolutionEntity entity) {
        return ClinicalEvolution.builder()
                .id(entity.getId())
                .clinicalRecordId(entity.getClinicalRecord().getId())
                .dentistId(entity.getDentistId())
                .description(entity.getDescription())
                .findings(entity.getFindings())
                .treatment(entity.getTreatment())
                .recordedAt(entity.getRecordedAt())
                .build();
    }

    private DiagnosisEntity toDiagnosisEntity(Diagnosis diagnosis, ClinicalRecordEntity recordEntity) {
        return DiagnosisEntity.builder()
                .id(diagnosis.getId())
                .clinicalRecord(recordEntity)
                .code(diagnosis.getCode())
                .description(diagnosis.getDescription())
                .diagnosedAt(diagnosis.getDiagnosedAt())
                .dentistId(diagnosis.getDentistId())
                .toothNumber(diagnosis.getToothNumber())
                .build();
    }

    private Diagnosis toDiagnosisDomain(DiagnosisEntity entity) {
        return Diagnosis.builder()
                .id(entity.getId())
                .clinicalRecordId(entity.getClinicalRecord().getId())
                .code(entity.getCode())
                .description(entity.getDescription())
                .diagnosedAt(entity.getDiagnosedAt())
                .dentistId(entity.getDentistId())
                .toothNumber(entity.getToothNumber())
                .build();
    }

    private TreatmentPlanEntity toTreatmentPlanEntity(TreatmentPlan treatmentPlan, ClinicalRecordEntity recordEntity) {
        TreatmentPlanEntity entity = TreatmentPlanEntity.builder()
                .id(treatmentPlan.getId())
                .clinicalRecord(recordEntity)
                .dentistId(treatmentPlan.getDentistId())
                .title(treatmentPlan.getTitle())
                .description(treatmentPlan.getDescription())
                .status(treatmentPlan.getStatus())
                .budgetId(treatmentPlan.getBudgetId())
                .startDate(treatmentPlan.getStartDate())
                .estimatedEndDate(treatmentPlan.getEstimatedEndDate())
                .procedures(new ArrayList<>())
                .build();

        if (treatmentPlan.getProcedures() != null) {
            for (TreatmentProcedure procedure : treatmentPlan.getProcedures()) {
                entity.getProcedures().add(toTreatmentProcedureEntity(procedure, entity));
            }
        }

        return entity;
    }

    private TreatmentPlan toTreatmentPlanDomain(TreatmentPlanEntity entity) {
        List<TreatmentProcedure> procedures = entity.getProcedures() == null
                ? List.of()
                : entity.getProcedures().stream().map(this::toTreatmentProcedureDomain).toList();

        return TreatmentPlan.builder()
                .id(entity.getId())
                .clinicalRecordId(entity.getClinicalRecord().getId())
                .dentistId(entity.getDentistId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .procedures(procedures)
                .budgetId(entity.getBudgetId())
                .startDate(entity.getStartDate())
                .estimatedEndDate(entity.getEstimatedEndDate())
                .build();
    }

    private TreatmentProcedureEntity toTreatmentProcedureEntity(TreatmentProcedure procedure, TreatmentPlanEntity treatmentPlanEntity) {
        return TreatmentProcedureEntity.builder()
                .id(procedure.getId())
                .treatmentPlan(treatmentPlanEntity)
                .description(procedure.getDescription())
                .toothNumber(procedure.getToothNumber())
                .performed(procedure.isPerformed())
                .performedAt(procedure.getPerformedAt())
                .performedByDentistId(procedure.getPerformedByDentistId())
                .budgetItemId(procedure.getBudgetItemId())
                .build();
    }

    private TreatmentProcedure toTreatmentProcedureDomain(TreatmentProcedureEntity entity) {
        return TreatmentProcedure.builder()
                .id(entity.getId())
                .treatmentPlanId(entity.getTreatmentPlan().getId())
                .description(entity.getDescription())
                .toothNumber(entity.getToothNumber())
                .performed(entity.isPerformed())
                .performedAt(entity.getPerformedAt())
                .performedByDentistId(entity.getPerformedByDentistId())
                .budgetItemId(entity.getBudgetItemId())
                .build();
    }

    private String serializeSurfaces(Set<ToothSurface> surfaces) {
        if (surfaces == null || surfaces.isEmpty()) {
            return null;
        }
        return surfaces.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    private Set<ToothSurface> deserializeSurfaces(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return Collections.emptySet();
        }
        Set<ToothSurface> result = new LinkedHashSet<>();
        for (String token : serialized.split(",")) {
            result.add(ToothSurface.valueOf(token.trim()));
        }
        return result;
    }

    private String serializeSurfaceConditions(Map<ToothSurface, ToothCondition> map) {
        if (map == null || map.isEmpty()) return null;
        return map.entrySet().stream()
                .map(e -> e.getKey().name() + ":" + e.getValue().name())
                .sorted()
                .collect(Collectors.joining(","));
    }

    private Map<ToothSurface, ToothCondition> deserializeSurfaceConditions(String serialized) {
        if (serialized == null || serialized.isBlank()) return Collections.emptyMap();
        Map<ToothSurface, ToothCondition> result = new LinkedHashMap<>();
        for (String token : serialized.split(",")) {
            String[] parts = token.trim().split(":");
            if (parts.length == 2) {
                result.put(ToothSurface.valueOf(parts[0]), ToothCondition.valueOf(parts[1]));
            }
        }
        return result;
    }

    private String serializeRootFindings(Set<RootFinding> findings) {
        if (findings == null || findings.isEmpty()) return null;
        return findings.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    private Set<RootFinding> deserializeRootFindings(String serialized) {
        if (serialized == null || serialized.isBlank()) return Collections.emptySet();
        Set<RootFinding> result = new LinkedHashSet<>();
        for (String token : serialized.split(",")) {
            result.add(RootFinding.valueOf(token.trim()));
        }
        return result;
    }
}


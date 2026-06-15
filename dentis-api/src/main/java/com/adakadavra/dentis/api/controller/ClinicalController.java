package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.api.config.S3StorageService;
import com.adakadavra.dentis.api.security.service.TenantSecurityService;
import com.adakadavra.dentis.clinical.domain.model.*;
import com.adakadavra.dentis.common.response.ApiError;
import com.adakadavra.dentis.clinical.domain.service.ClinicalAttachmentService;
import com.adakadavra.dentis.clinical.domain.service.ClinicalRecordService;
import com.adakadavra.dentis.ia.domain.service.IngestionService;
import com.adakadavra.dentis.common.response.ApiResponse;
import com.adakadavra.dentis.patient.application.usecase.PatientUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinical-records")
@RequiredArgsConstructor
@Tag(name = "Clinical Records", description = "Clinical history, odontogram, evolutions and treatment plans")
public class ClinicalController {

    private final ClinicalRecordService clinicalRecordService;
    private final ClinicalAttachmentService attachmentService;
    private final S3StorageService s3;
    private final PatientUseCase patientUseCase;
    private final TenantSecurityService tenantSecurity;
    private final IngestionService ingestionService;

    private void requirePatientAccess(UUID patientId) {
        tenantSecurity.currentClinicId()
                .ifPresent(clinicId -> patientUseCase.validatePatientInClinic(patientId, clinicId));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get or create clinical record for a patient")
    public ResponseEntity<ApiResponse<ClinicalRecord>> getOrCreate(@PathVariable UUID patientId) {
        patientUseCase.findById(patientId);
        requirePatientAccess(patientId);
        return ResponseEntity.ok(ApiResponse.ok(clinicalRecordService.getOrCreateForPatient(patientId)));
    }

    @PutMapping("/patient/{patientId}/odontogram")
    @Operation(summary = "Update odontogram teeth for a patient")
    public ResponseEntity<ApiResponse<ClinicalRecord>> updateOdontogram(
            @PathVariable UUID patientId,
            @Valid @RequestBody UpdateOdontogramRequest request) {
        requirePatientAccess(patientId);
        List<OdontogramTooth> teeth = request.teeth().stream()
                .map(t -> OdontogramTooth.builder()
                        .id(t.id())
                        .toothNumber(t.toothNumber())
                        .condition(t.condition())
                        .affectedSurfaces(t.affectedSurfaces() != null ? t.affectedSurfaces() : Set.of())
                        .surfaceConditions(t.surfaceConditions() != null ? t.surfaceConditions() : Map.of())
                        .spaceStatus(t.spaceStatus())
                        .notes(t.notes())
                        .rootFindings(t.rootFindings() != null ? t.rootFindings() : Set.of())
                        .rootNotes(t.rootNotes())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(clinicalRecordService.updateOdontogram(patientId, teeth)));
    }

    @PatchMapping("/patient/{patientId}/dentition-type")
    @Operation(summary = "Update dentition type (PERMANENT / PRIMARY / MIXED)")
    public ResponseEntity<ApiResponse<ClinicalRecord>> updateDentitionType(
            @PathVariable UUID patientId,
            @RequestParam DentitionType dentitionType) {
        requirePatientAccess(patientId);
        return ResponseEntity.ok(ApiResponse.ok(clinicalRecordService.updateDentitionType(patientId, dentitionType)));
    }

    @PostMapping("/patient/{patientId}/evolutions")
    @Operation(summary = "Add a clinical evolution entry")
    public ResponseEntity<ApiResponse<ClinicalRecord>> addEvolution(
            @PathVariable UUID patientId,
            @Valid @RequestBody AddEvolutionRequest request) {
        requirePatientAccess(patientId);
        ClinicalEvolution evolution = ClinicalEvolution.builder()
                .dentistId(request.dentistId())
                .description(request.description())
                .findings(request.findings())
                .treatment(request.treatment())
                .recordedAt(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(clinicalRecordService.addEvolution(patientId, evolution), "Evolution added"));
    }

    @PostMapping("/patient/{patientId}/diagnoses")
    @Operation(summary = "Add a diagnosis to the clinical record")
    public ResponseEntity<ApiResponse<ClinicalRecord>> addDiagnosis(
            @PathVariable UUID patientId,
            @Valid @RequestBody AddDiagnosisRequest request) {
        requirePatientAccess(patientId);
        Diagnosis diagnosis = Diagnosis.builder()
                .code(request.code())
                .description(request.description())
                .diagnosedAt(request.diagnosedAt() != null ? request.diagnosedAt() : LocalDate.now())
                .dentistId(request.dentistId())
                .toothNumber(request.toothNumber())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(clinicalRecordService.addDiagnosis(patientId, diagnosis), "Diagnosis added"));
    }

    @PostMapping("/patient/{patientId}/treatment-plans")
    @Operation(summary = "Add a treatment plan to the clinical record")
    public ResponseEntity<ApiResponse<ClinicalRecord>> addTreatmentPlan(
            @PathVariable UUID patientId,
            @Valid @RequestBody AddTreatmentPlanRequest request) {
        requirePatientAccess(patientId);
        TreatmentPlan plan = TreatmentPlan.builder()
                .dentistId(request.dentistId())
                .title(request.title())
                .description(request.description())
                .status(TreatmentPlanStatus.PROPOSED)
                .startDate(request.startDate())
                .estimatedEndDate(request.estimatedEndDate())
                .procedures(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(clinicalRecordService.addTreatmentPlan(patientId, plan), "Treatment plan added"));
    }

    // ---- Attachments ----

    @PostMapping("/patient/{patientId}/attachments/presign")
    @Operation(summary = "Request a presigned S3 URL to upload a clinical attachment")
    public ResponseEntity<ApiResponse<PresignedUploadResponse>> presignUpload(
            @PathVariable UUID patientId,
            @Valid @RequestBody PresignAttachmentRequest request) {
        requirePatientAccess(patientId);
        UUID clinicId = tenantSecurity.currentClinicId()
                .orElseThrow(() -> new IllegalStateException("No clinic context"));
        if (!s3.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(
                            ApiError.builder().code("S3_NOT_CONFIGURED").message("S3 not configured").build()));
        }
        String key = s3.buildKey(clinicId, patientId, request.fileName());
        String uploadUrl = s3.presignedUploadUrl(key, request.contentType());
        return ResponseEntity.ok(ApiResponse.ok(new PresignedUploadResponse(key, uploadUrl)));
    }

    @PostMapping("/patient/{patientId}/attachments")
    @Operation(summary = "Register an attachment after successful S3 upload")
    public ResponseEntity<ApiResponse<ClinicalAttachment>> registerAttachment(
            @PathVariable UUID patientId,
            @Valid @RequestBody RegisterAttachmentRequest request) {
        requirePatientAccess(patientId);
        UUID clinicId = tenantSecurity.currentClinicId()
                .orElseThrow(() -> new IllegalStateException("No clinic context"));
        ClinicalAttachment attachment = attachmentService.register(
                patientId, clinicId, request.s3Key(), request.fileName(),
                request.contentType(), request.fileSize(), request.description(),
                request.toothNumber(), request.uploadedByDentistId());
        if (s3.isConfigured()) {
            ingestionService.ingestAsync(
                    attachment.getId(), clinicId, attachment.getS3Key(),
                    attachment.getContentType(), s3::getObjectBytes);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(attachment, "Attachment registered"));
    }

    @GetMapping("/patient/{patientId}/attachments")
    @Operation(summary = "List clinical attachments for a patient")
    public ResponseEntity<ApiResponse<List<AttachmentView>>> listAttachments(
            @PathVariable UUID patientId,
            @RequestParam(required = false) Integer toothNumber) {
        requirePatientAccess(patientId);
        List<ClinicalAttachment> attachments = toothNumber != null
                ? attachmentService.listByTooth(patientId, toothNumber)
                : attachmentService.listByRecord(patientId);
        List<AttachmentView> views = attachments.stream()
                .map(a -> new AttachmentView(
                        a.getId(), a.getFileName(), a.getContentType(),
                        a.getFileSize(), a.getDescription(), a.getToothNumber(),
                        a.getUploadedAt(),
                        s3.isConfigured() ? s3.presignedDownloadUrl(a.getS3Key()) : null))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(views));
    }

    @DeleteMapping("/patient/{patientId}/attachments/{attachmentId}")
    @Operation(summary = "Delete a clinical attachment")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable UUID patientId,
            @PathVariable UUID attachmentId) {
        requirePatientAccess(patientId);
        String key = attachmentService.deleteAndGetKey(attachmentId, patientId);
        if (s3.isConfigured()) {
            s3.delete(key);
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "Attachment deleted"));
    }

    // ---- Inner request records ----

    public record ToothUpdateRequest(
            UUID id,
            @NotNull int toothNumber,
            @NotNull ToothCondition condition,
            Set<ToothSurface> affectedSurfaces,
            Map<ToothSurface, ToothCondition> surfaceConditions,
            SpaceStatus spaceStatus,
            String notes,
            Set<RootFinding> rootFindings,
            String rootNotes) {}

    public record UpdateOdontogramRequest(@NotNull List<ToothUpdateRequest> teeth) {}

    public record AddEvolutionRequest(
            @NotNull UUID dentistId,
            @NotBlank @Size(max = 2000) String description,
            @Size(max = 2000) String findings,
            @Size(max = 2000) String treatment) {}

    public record AddDiagnosisRequest(
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 500) String description,
            LocalDate diagnosedAt,
            UUID dentistId,
            Integer toothNumber) {}

    public record AddTreatmentPlanRequest(
            @NotNull UUID dentistId,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            LocalDate startDate,
            LocalDate estimatedEndDate) {}

    public record PresignAttachmentRequest(
            @NotBlank @Size(max = 255) String fileName,
            @NotBlank @Size(max = 100) String contentType) {}

    public record PresignedUploadResponse(String s3Key, String uploadUrl) {}

    public record RegisterAttachmentRequest(
            @NotBlank String s3Key,
            @NotBlank @Size(max = 255) String fileName,
            @NotBlank @Size(max = 100) String contentType,
            Long fileSize,
            @Size(max = 500) String description,
            Integer toothNumber,
            UUID uploadedByDentistId) {}

    public record AttachmentView(
            UUID id,
            String fileName,
            String contentType,
            Long fileSize,
            String description,
            Integer toothNumber,
            LocalDateTime uploadedAt,
            String downloadUrl) {}
}

package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.api.config.S3StorageService;
import com.adakadavra.dentis.api.security.entity.User;
import com.adakadavra.dentis.api.security.entity.UserRole;
import com.adakadavra.dentis.api.security.entity.UserStaffType;
import com.adakadavra.dentis.api.security.service.TenantSecurityService;
import com.adakadavra.dentis.clinical.domain.model.ClinicalAttachment;
import com.adakadavra.dentis.clinical.domain.service.ClinicalAttachmentService;
import com.adakadavra.dentis.common.response.ApiError;
import com.adakadavra.dentis.common.response.ApiResponse;
import com.adakadavra.dentis.ia.domain.model.ChatMessage;
import com.adakadavra.dentis.ia.domain.model.ChatSession;
import com.adakadavra.dentis.ia.agent.AgentPort;
import com.adakadavra.dentis.ia.domain.service.IaChatService;
import com.adakadavra.dentis.ia.domain.service.IngestionService;
import com.adakadavra.dentis.ia.infrastructure.config.IaProperties;
import com.adakadavra.dentis.ia.infrastructure.persistence.repository.ChatSessionJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor
@Tag(name = "Asistente IA", description = "Asistente odontológico experto (solo dentistas y super admin)")
public class IaController {

    private final IaChatService chatService;
    private final IngestionService ingestionService;
    private final ClinicalAttachmentService attachmentService;
    private final S3StorageService s3;
    private final TenantSecurityService tenantSecurity;
    private final IaProperties props;
    private final ChatSessionJpaRepository sessionJpaRepo;
    private final MeterRegistry meterRegistry;

    private static final Logger log = LoggerFactory.getLogger(IaController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ─── Guard helper ────────────────────────────────────────────────────────

    private static final String IA_GUARD =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN') or 'DENTIST' == principal?.staffType?.name()";

    // ─── Resolve caller identity ──────────────────────────────────────────────

    private UUID dentistId(User user) {
        return user.getId();
    }

    private UUID resolveClinicId(User user, UUID requestedClinicId) {
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return requestedClinicId; // may be null → retrieval skips RAG
        }
        return tenantSecurity.currentClinicId()
                .orElseThrow(() -> new IllegalStateException("No clinic context"));
    }

    // ─── Chat sessions ────────────────────────────────────────────────────────

    @PostMapping("/chat/sessions")
    @PreAuthorize(IA_GUARD)
    @Operation(summary = "Create a new chat session")
    public ResponseEntity<ApiResponse<ChatSession>> createSession(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateSessionRequest req) {
        if (!props.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(ApiError.builder().code("IA_DISABLED").message("El módulo de IA está deshabilitado").build()));
        }
        UUID clinicId = resolveClinicId(user, req.clinicId());
        ChatSession session = chatService.createSession(dentistId(user), clinicId, req.title());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(session, "Sesión creada"));
    }

    @GetMapping("/chat/sessions")
    @PreAuthorize(IA_GUARD)
    @Operation(summary = "List chat sessions for the current user")
    public ResponseEntity<ApiResponse<List<ChatSession>>> listSessions(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getSessions(dentistId(user))));
    }

    @GetMapping("/chat/sessions/{id}/messages")
    @PreAuthorize(IA_GUARD)
    @Operation(summary = "Get messages for a session")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getMessages(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getMessages(id, dentistId(user))));
    }

    @PostMapping(value = "/chat/sessions/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize(IA_GUARD)
    @Operation(summary = "Send a message and stream the assistant's response as SSE tokens")
    public SseEmitter streamMessage(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest req) {

        if (!props.isEnabled()) {
            SseEmitter emitter = new SseEmitter(0L);
            sseSend(emitter, "{\"err\":\"El módulo de IA está deshabilitado\"}");
            emitter.complete();
            return emitter;
        }

        String clinicTag = tenantSecurity.currentClinicId().map(UUID::toString).orElse("global");
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min max

        Thread.ofVirtual().name("sse-ia-" + id).start(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                ChatMessage reply = chatService.streamMessage(id, dentistId(user), req.content(),
                        event -> {
                            if (event instanceof AgentPort.AgentEvent.Token(String text)) {
                                sseSend(emitter, toJson(java.util.Map.of("t", text)));
                            } else if (event instanceof AgentPort.AgentEvent.ToolEvent toolEvent) {
                                sseSend(emitter, toJson(java.util.Map.of(
                                        "tool",   toolEvent.toolName(),
                                        "status", toolEvent.status(),
                                        "label",  toolEvent.label()
                                )));
                            }
                        });

                sample.stop(Timer.builder("dentis.ia.request").tag("clinic_id", clinicTag).register(meterRegistry));

                if (reply.getInputTokens() > 0)
                    Counter.builder("dentis.ia.tokens").tag("type", "input").tag("clinic_id", clinicTag)
                            .register(meterRegistry).increment(reply.getInputTokens());
                if (reply.getOutputTokens() > 0) {
                    Counter.builder("dentis.ia.tokens").tag("type", "output").tag("clinic_id", clinicTag)
                            .register(meterRegistry).increment(reply.getOutputTokens());
                    IaProperties.ModelPricing pricing = props.resolvePricing(props.getGenerationModelId());
                    Counter.builder("dentis.ia.cost.usd").tag("clinic_id", clinicTag)
                            .register(meterRegistry).increment(pricing.rawCost(reply.getInputTokens(), reply.getOutputTokens()));
                }

                IaProperties.ModelPricing pricing = props.resolvePricing(props.getGenerationModelId());
                sseSend(emitter, toJson(java.util.Map.of(
                        "done", true,
                        "usage", java.util.Map.of(
                                "input_tokens",   reply.getInputTokens(),
                                "output_tokens",  reply.getOutputTokens(),
                                "cost_usd",       pricing.rawCost(reply.getInputTokens(), reply.getOutputTokens()),
                                "billed_cost_usd", pricing.billedCost(reply.getInputTokens(), reply.getOutputTokens())
                        )
                )));
            } catch (Exception e) {
                log.error("[sse-ia] error session={}: {}", id, e.getMessage());
                sseSend(emitter, toJson(java.util.Map.of("err", e.getMessage() != null ? e.getMessage() : "Error interno")));
            } finally {
                try { emitter.complete(); } catch (Exception ignored) {}
            }
        });

        return emitter;
    }

    private void sseSend(SseEmitter emitter, String json) {
        try {
            emitter.send(SseEmitter.event().data(json));
        } catch (IOException e) {
            log.debug("[sse-ia] client disconnected");
        }
    }

    private String toJson(Object obj) {
        try { return MAPPER.writeValueAsString(obj); } catch (Exception e) { return "{}"; }
    }

    @DeleteMapping("/chat/sessions/{id}")
    @PreAuthorize(IA_GUARD)
    @Operation(summary = "Delete a chat session")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        chatService.deleteSession(id, dentistId(user));
        return ResponseEntity.ok(ApiResponse.ok(null, "Sesión eliminada"));
    }

    // ─── Indexing ─────────────────────────────────────────────────────────────

    @PostMapping("/index/reindex")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Re-index all clinical attachments for the current clinic")
    public ResponseEntity<ApiResponse<String>> reindex(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) UUID clinicId) {
        UUID targetClinic = resolveClinicId(user, clinicId);
        if (targetClinic == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiError.builder().code("MISSING_CLINIC").message("Se requiere clinicId").build()));
        }
        List<ClinicalAttachment> attachments = attachmentService.listByClinic(targetClinic);
        List<IngestionService.AttachmentRef> refs = attachments.stream()
                .map(a -> new IngestionService.AttachmentRef(a.getId(), a.getS3Key(), a.getContentType()))
                .toList();
        ingestionService.reindexClinic(targetClinic, refs, s3::getObjectBytes);
        return ResponseEntity.accepted().body(
                ApiResponse.ok("Re-indexación iniciada para " + refs.size() + " adjuntos"));
    }

    // ─── Stats ────────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "IA usage statistics (SUPER_ADMIN: all clinics, ADMIN: own clinic)")
    public ResponseEntity<ApiResponse<IaStatsResponse>> getStats(@AuthenticationPrincipal User user) {
        boolean isSuperAdmin = user.getRole() == UserRole.SUPER_ADMIN;

        List<IaUserStats> rows;
        long totalSessions;

        IaProperties.ModelPricing rowPricing = props.resolvePricing(props.getGenerationModelId());

        if (isSuperAdmin) {
            rows = sessionJpaRepo.getUsageAll().stream()
                    .map(r -> {
                        long in = toLong(r[3]), out = toLong(r[4]);
                        return new IaUserStats(
                                (String) r[0],
                                (String) r[1],
                                toLong(r[2]),
                                in,
                                out,
                                rowPricing.billedCost(in, out));
                    })
                    .toList();
            totalSessions = sessionJpaRepo.count();
        } else {
            UUID clinicId = tenantSecurity.currentClinicId()
                    .orElseThrow(() -> new IllegalStateException("No clinic context"));
            rows = sessionJpaRepo.getUsageByClinic(clinicId).stream()
                    .map(r -> {
                        long msg = toLong(r[1]), in = toLong(r[2]), out = toLong(r[3]);
                        return new IaUserStats(
                                (String) r[0],
                                null,
                                msg,
                                in,
                                out,
                                rowPricing.billedCost(in, out));
                    })
                    .toList();
            totalSessions = sessionJpaRepo.countByClinicId(clinicId);
        }

        long totalMessages     = rows.stream().mapToLong(IaUserStats::messages).sum();
        long totalInputTokens  = rows.stream().mapToLong(IaUserStats::inputTokens).sum();
        long totalOutputTokens = rows.stream().mapToLong(IaUserStats::outputTokens).sum();

        IaProperties.ModelPricing pricing = props.resolvePricing(props.getGenerationModelId());
        double totalRawCostUsd    = pricing.rawCost(totalInputTokens, totalOutputTokens);
        double totalBilledCostUsd = pricing.billedCost(totalInputTokens, totalOutputTokens);

        return ResponseEntity.ok(ApiResponse.ok(
                new IaStatsResponse(totalSessions, totalMessages, totalInputTokens, totalOutputTokens,
                        totalRawCostUsd, totalBilledCostUsd, rows)));
    }

    private static long toLong(Object val) {
        return val == null ? 0L : ((Number) val).longValue();
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    public record CreateSessionRequest(String title, UUID clinicId) {}
    public record SendMessageRequest(@NotBlank String content) {}
    public record IaUserStats(String username, String clinicName, long messages, long inputTokens, long outputTokens, double billedCostUsd) {}
    public record IaStatsResponse(long totalSessions, long totalMessages, long totalInputTokens, long totalOutputTokens,
                                  double totalRawCostUsd, double totalBilledCostUsd, List<IaUserStats> rows) {}
}

package com.dentis.api.controller;

import com.dentis.application.dto.request.CreateFeeItemRequest;
import com.dentis.application.dto.response.FeeItemResponse;
import com.dentis.application.service.FeeItemService;
import com.dentis.common.response.ApiResponse;
import com.dentis.common.response.PageResponse;
import com.dentis.domain.enums.FeeCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fee-items")
@RequiredArgsConstructor
@Tag(name = "Fee Items", description = "Dental procedure tariffs / aranceles")
public class FeeItemController {

    private final FeeItemService feeItemService;

    @PostMapping
    @Operation(summary = "Create a fee item")
    public ResponseEntity<ApiResponse<FeeItemResponse>> create(
            @Valid @RequestBody CreateFeeItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(feeItemService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a fee item")
    public ResponseEntity<ApiResponse<FeeItemResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody CreateFeeItemRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(feeItemService.update(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fee item by ID")
    public ResponseEntity<ApiResponse<FeeItemResponse>> findById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(feeItemService.findById(id)));
    }

    @GetMapping
    @Operation(summary = "List all active fee items")
    public ResponseEntity<ApiResponse<List<FeeItemResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(feeItemService.findAllActive()));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "List fee items by category")
    public ResponseEntity<ApiResponse<PageResponse<FeeItemResponse>>> findByCategory(
            @PathVariable FeeCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                feeItemService.findByCategory(category, PageRequest.of(page, size))));
    }

    @GetMapping("/search")
    @Operation(summary = "Search fee items by name or code")
    public ResponseEntity<ApiResponse<PageResponse<FeeItemResponse>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                feeItemService.search(q, PageRequest.of(page, size))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate fee item")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String id) {
        feeItemService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Fee item deactivated"));
    }
}

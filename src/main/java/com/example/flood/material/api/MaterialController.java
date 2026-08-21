package com.example.flood.material.api;

import com.example.flood.common.idempotency.IdempotencyExecutor;
import com.example.flood.common.idempotency.IdempotentOperation;
import com.example.flood.common.idempotency.IdempotentResult;
import com.example.flood.common.idempotency.OperationResult;
import com.example.flood.material.application.DirectMaterialCalculationCommand;
import com.example.flood.material.application.MaterialDemandCalculationService;
import com.example.flood.material.application.RegionDataMaterialCalculationCommand;
import com.example.flood.material.domain.InventoryItem;
import com.example.flood.material.domain.PopulationSnapshot;
import com.example.flood.security.application.ApiPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("/openapi/v1/material-demand-calculations")
public class MaterialController {
    private final MaterialDemandCalculationService service;
    private final IdempotencyExecutor idempotency;
    public MaterialController(MaterialDemandCalculationService service,
        IdempotencyExecutor idempotency) {
        this.service = service; this.idempotency = idempotency;
    }

    @PostMapping
    ResponseEntity<MaterialCalculationResponse> calculateDirect(
        @Valid @RequestBody DirectMaterialCalculationRequest request,
        @RequestHeader("Idempotency-Key") String key,
        @AuthenticationPrincipal ApiPrincipal principal) {
        return execute("POST:/openapi/v1/material-demand-calculations", key, request, principal,
            () -> service.calculateDirect(new DirectMaterialCalculationCommand(request.region(),
                request.situationLevel(), request.supplyDurationHours(), request.reserveRatio(),
                population(request.population()), inventory(request.currentInventory())), principal));
    }

    @PostMapping("/from-region-data")
    ResponseEntity<MaterialCalculationResponse> calculateFromRegionData(
        @Valid @RequestBody RegionDataMaterialCalculationRequest request,
        @RequestHeader("Idempotency-Key") String key,
        @AuthenticationPrincipal ApiPrincipal principal) {
        return execute("POST:/openapi/v1/material-demand-calculations/from-region-data",
            key, request, principal, () -> service.calculateFromRegionData(
                new RegionDataMaterialCalculationCommand(request.region(),
                    request.assessmentTimeRange().startTime().toInstant(),
                    request.assessmentTimeRange().endTime().toInstant(),
                    request.supplyDurationHours(), inventory(request.currentInventory())), principal));
    }

    private ResponseEntity<MaterialCalculationResponse> execute(String operationCode, String key,
        Object request, ApiPrincipal principal,
        java.util.function.Supplier<MaterialCalculationResponse> calculation) {
        IdempotentResult<MaterialCalculationResponse> result = idempotency.execute(principal,
            new IdempotentOperation(operationCode, Map.of(), Map.of()), key, request,
            MaterialCalculationResponse.class, () -> {
                MaterialCalculationResponse body = calculation.get();
                return new OperationResult<>(201, body, "MATERIAL", body.calculationId());
            });
        return ResponseEntity.status(result.httpStatus()).body(result.body());
    }

    private static PopulationSnapshot population(PopulationRequest request) {
        return new PopulationSnapshot(request.affectedPopulation(), request.trappedPopulation(),
            request.evacuatedPopulation(), request.vulnerablePopulation());
    }
    private static List<InventoryItem> inventory(List<InventoryRequest> requests) {
        return requests.stream().map(item -> new InventoryItem(
            item.materialCode(), item.unit(), item.quantity())).toList();
    }
}

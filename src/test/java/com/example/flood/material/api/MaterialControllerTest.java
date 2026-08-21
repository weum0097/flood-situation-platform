package com.example.flood.material.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.flood.common.idempotency.IdempotencyExecutor;
import com.example.flood.common.idempotency.IdempotentResult;
import com.example.flood.common.idempotency.OperationResult;
import com.example.flood.material.application.MaterialDemandCalculationService;
import com.example.flood.region.application.RegionSelector;
import com.example.flood.security.application.ApiPrincipal;
import com.example.flood.situation.domain.SituationLevel;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class MaterialControllerTest {
    @Test @SuppressWarnings("unchecked")
    void bothEndpointsReturnCreatedResponses() {
        MaterialDemandCalculationService service = mock(MaterialDemandCalculationService.class);
        IdempotencyExecutor idempotency = mock(IdempotencyExecutor.class);
        doAnswer(invocation -> {
            Supplier<OperationResult<MaterialCalculationResponse>> supplier = invocation.getArgument(5);
            var result = supplier.get();
            return new IdempotentResult<>(result.httpStatus(), result.body(), false);
        }).when(idempotency).execute(any(), any(), any(), any(), any(), any());
        MaterialController controller = new MaterialController(service, idempotency);
        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T04:00:00Z");
        ApiPrincipal principal = new ApiPrincipal(1, 2, "client", Set.of("material:calculate"));
        MaterialCalculationResponse response = new MaterialCalculationResponse("MDC_1", "DIRECT",
            null, new RegionSelector("320111", "浦口区"), SituationLevel.HIGH,
            new BigDecimal("24.000"), 1, List.of(), "v1", List.of(), now);
        when(service.calculateDirect(any(), any())).thenReturn(response);
        when(service.calculateFromRegionData(any(), any())).thenReturn(response);

        DirectMaterialCalculationRequest direct = new DirectMaterialCalculationRequest(
            new RegionSelector("320111", null), SituationLevel.HIGH, new BigDecimal("24"), null,
            new PopulationRequest(10, 2, 3, 1), List.of());
        RegionDataMaterialCalculationRequest database = new RegionDataMaterialCalculationRequest(
            new RegionSelector("320111", null),
            new RegionDataMaterialCalculationRequest.AssessmentTimeRange(now.minusDays(1), now),
            new BigDecimal("24"), List.of());

        assertThat(controller.calculateDirect(direct, "key-1", principal)
            .getStatusCode().value()).isEqualTo(201);
        assertThat(controller.calculateFromRegionData(database, "key-2", principal)
            .getStatusCode().value()).isEqualTo(201);
    }
}

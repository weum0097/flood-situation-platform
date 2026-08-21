package com.example.flood.situation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.flood.common.api.PageResponse;
import com.example.flood.common.idempotency.IdempotencyExecutor;
import com.example.flood.common.idempotency.IdempotentResult;
import com.example.flood.common.idempotency.OperationResult;
import com.example.flood.event.api.EventObservationRequest;
import com.example.flood.event.api.HazardRequest;
import com.example.flood.event.api.ImpactRequest;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import com.example.flood.region.application.RegionSelector;
import com.example.flood.security.application.ApiPrincipal;
import com.example.flood.situation.application.RegionSituationAssessmentService;
import com.example.flood.situation.domain.SituationLevel;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class SituationControllerTest {
    @Test @SuppressWarnings("unchecked")
    void postReturns201AndGetReturnsSavedAssessments() {
        RegionSituationAssessmentService service = mock(RegionSituationAssessmentService.class);
        IdempotencyExecutor idempotency = mock(IdempotencyExecutor.class);
        doAnswer(invocation -> {
            Supplier<OperationResult<SituationAssessmentResponse>> supplier = invocation.getArgument(5);
            OperationResult<SituationAssessmentResponse> result = supplier.get();
            return new IdempotentResult<>(result.httpStatus(), result.body(), false);
        }).when(idempotency).execute(any(), any(), any(), any(), any(), any());
        SituationController controller = new SituationController(service, idempotency);
        OffsetDateTime time = OffsetDateTime.parse("2026-08-21T12:00:00+08:00");
        ApiPrincipal principal = new ApiPrincipal(1, 2, "client", Set.of("situation:calculate"));
        SituationAssessmentResponse result = new SituationAssessmentResponse("RSA_1",
            new RegionSelector("320111", "浦口区"), time, SituationLevel.HIGH, 1,
            new SituationAssessmentResponse.AggregateImpact(10, 2, 3, 1), List.of(),
            new SituationAssessmentResponse.ImportSummary(1, 0, 1, 0), "v1", List.of(), time);
        when(service.assess(any(), any())).thenReturn(result);
        when(service.search(any())).thenReturn(new PageResponse<>(List.of(
            SituationAssessmentSummaryResponse.from(result)), 0, 20, 1, 1));
        EventObservationRequest observation = new EventObservationRequest("OBS_EXT", time,
            new HazardRequest(BigDecimal.ONE, null, null, null),
            new ImpactRequest(10, 2, 3, 1, 0, 0, 0, 0, 0, 0, 0, 0));
        SituationAssessmentRequest request = new SituationAssessmentRequest(
            new RegionSelector("320111", null), time, List.of(new SituationEventImportRequest(
                "EXT_1", "source", EventType.RIVER_FLOOD, "Flood", time.minusHours(1),
                null, EventStatus.ONGOING, observation)));

        assertThat(controller.assess(request, "key", principal).getStatusCode().value()).isEqualTo(201);
        assertThat(controller.search("320111", null, time.minusDays(1), time.plusDays(1),
            null, 0, 20).getBody().items()).hasSize(1);
    }
}

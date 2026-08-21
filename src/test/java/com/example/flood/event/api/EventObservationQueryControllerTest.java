package com.example.flood.event.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.flood.common.api.PageResponse;
import com.example.flood.common.idempotency.IdempotencyExecutor;
import com.example.flood.common.idempotency.IdempotentResult;
import com.example.flood.common.idempotency.OperationResult;
import com.example.flood.event.application.EventApplicationService;
import com.example.flood.event.application.EventQueryService;
import com.example.flood.security.application.ApiPrincipal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class EventObservationQueryControllerTest {
    @Test @SuppressWarnings("unchecked")
    void appendReturns201AndQueryReturnsPage() {
        EventApplicationService eventService = mock(EventApplicationService.class);
        EventQueryService queryService = mock(EventQueryService.class);
        IdempotencyExecutor idempotency = mock(IdempotencyExecutor.class);
        doAnswer(invocation -> {
            Supplier<OperationResult<ObservationResponse>> supplier = invocation.getArgument(5);
            var result = supplier.get();
            return new IdempotentResult<>(result.httpStatus(), result.body(), false);
        }).when(idempotency).execute(any(), any(), any(), any(), any(), any());
        EventController controller = new EventController(eventService, queryService, idempotency);
        ApiPrincipal principal = new ApiPrincipal(1, 2, "client", Set.of());
        OffsetDateTime time = OffsetDateTime.parse("2026-08-21T01:00:00Z");
        EventObservationRequest request = new EventObservationRequest("external", time,
            new HazardRequest(null, null, null, null),
            new ImpactRequest(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        when(eventService.appendObservation(any(), any(), any())).thenReturn(
            new ObservationResponse("OBS_1", time, request.hazard(), request.impact()));
        when(queryService.search(any())).thenReturn(
            new PageResponse<>(List.of(new EventSummaryResponse(
                "EVT_1", null, null, null, null, null, time, null, null, null)), 0, 20, 1, 1));

        assertThat(controller.appendObservation("EVT_1", request, "key", principal)
            .getStatusCode().value()).isEqualTo(201);
        assertThat(controller.search("320111", null, time.minusHours(1), time.plusHours(1),
            null, 0, 20).getBody().items()).hasSize(1);
    }
}

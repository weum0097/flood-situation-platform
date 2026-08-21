package com.example.flood.event.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.flood.common.idempotency.IdempotencyExecutor;
import com.example.flood.common.idempotency.IdempotentResult;
import com.example.flood.common.idempotency.OperationResult;
import com.example.flood.event.application.EventApplicationService;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.event.domain.EventType;
import com.example.flood.region.application.RegionSelector;
import com.example.flood.security.application.ApiPrincipal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventCreateUpdateControllerTest {
    private EventApplicationService service;
    private EventController controller;
    private final ApiPrincipal principal = new ApiPrincipal(1, 2, "client", Set.of("event:write"));
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-21T00:00:00Z");

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = mock(EventApplicationService.class);
        IdempotencyExecutor idempotency = mock(IdempotencyExecutor.class);
        doAnswer(invocation -> {
            Supplier<OperationResult<EventResponse>> supplier = invocation.getArgument(5);
            OperationResult<EventResponse> operation = supplier.get();
            return new IdempotentResult<>(operation.httpStatus(), operation.body(), false);
        }).when(idempotency).execute(any(), any(), any(), any(), any(), any());
        controller = new EventController(service, idempotency);
    }

    @Test
    void createReturnsStored201Status() {
        EventResponse body = response();
        when(service.create(any(), any())).thenReturn(body);
        CreateEventRequest request = new CreateEventRequest(
            "external", "source", new RegionSelector("320111", null), EventType.RIVER_FLOOD,
            "Flood", now, null, EventStatus.ONGOING, null);

        var response = controller.create(request, "key", principal);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().eventId()).isEqualTo("EVT_1");
    }

    @Test
    void updateReturnsStored200Status() {
        when(service.update(any(), any(), any())).thenReturn(response());
        UpdateEventRequest request = new UpdateEventRequest(
            EventType.RIVER_FLOOD, "Flood", now, null, EventStatus.ONGOING);

        assertThat(controller.update("EVT_1", request, "key", principal).getStatusCode().value())
            .isEqualTo(200);
    }

    private EventResponse response() {
        return new EventResponse("EVT_1", "external", "source",
            new RegionSelector("320111", "浦口区"), EventType.RIVER_FLOOD, "Flood",
            now, null, EventStatus.ONGOING, null, now, now);
    }
}

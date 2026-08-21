package com.example.flood.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.event.api.EventSummaryResponse;
import com.example.flood.event.domain.EventStatus;
import com.example.flood.region.application.RegionResolver;
import com.example.flood.region.application.RegionSelector;
import com.example.flood.region.domain.RegionStatus;
import com.example.flood.region.domain.ResolvedRegion;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventQueryServiceTest {
    private EventQueryDataSource dataSource;
    private EventQueryService service;
    private final ResolvedRegion region = new ResolvedRegion(
        3, "320111", "浦口区", 2L, "DISTRICT", RegionStatus.ACTIVE);

    @BeforeEach void setUp() {
        RegionResolver resolver = mock(RegionResolver.class);
        when(resolver.resolve(new RegionSelector("320111", null))).thenReturn(region);
        dataSource = mock(EventQueryDataSource.class);
        service = new EventQueryService(resolver, dataSource);
    }

    @Test void returnsStableDataSourceOrderAndPageMetadata() {
        EventQuery query = query(Instant.parse("2026-08-21T00:00:00Z"),
            Instant.parse("2026-08-22T00:00:00Z"), EventStatus.ONGOING, 0, 2);
        List<EventSummaryResponse> ordered = List.of(
            summary("EVT_new", "2026-08-21T10:00:00Z"),
            summary("EVT_old", "2026-08-21T09:00:00Z"));
        when(dataSource.count(region.id(), query)).thenReturn(3L);
        when(dataSource.findPage(region.id(), query)).thenReturn(ordered);

        var result = service.search(query);

        assertThat(result.items()).extracting(EventSummaryResponse::eventId)
            .containsExactly("EVT_new", "EVT_old");
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test void rejectsInvalidRangeAndPagination() {
        Instant time = Instant.parse("2026-08-21T00:00:00Z");
        assertCode(() -> service.search(query(time, time, null, 0, 20)), ErrorCode.INVALID_TIME_RANGE);
        assertCode(() -> service.search(query(time, time.plusSeconds(1), null, -1, 20)), ErrorCode.VALIDATION_ERROR);
        assertCode(() -> service.search(query(time, time.plusSeconds(1), null, 0, 101)), ErrorCode.VALIDATION_ERROR);
    }

    private EventQuery query(Instant start, Instant end, EventStatus status, int page, int size) {
        return new EventQuery(new RegionSelector("320111", null), start, end, status, page, size);
    }
    private EventSummaryResponse summary(String eventId, String startTime) {
        return new EventSummaryResponse(eventId, null, null, null, null, null,
            OffsetDateTime.parse(startTime), null, null, null);
    }
    private static void assertCode(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(code));
    }
}

package com.example.flood.event.application;

import com.example.flood.event.api.EventSummaryResponse;
import java.util.List;

public interface EventQueryDataSource {
    long count(long regionId, EventQuery query);
    List<EventSummaryResponse> findPage(long regionId, EventQuery query);
}

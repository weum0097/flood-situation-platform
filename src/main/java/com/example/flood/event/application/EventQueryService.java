package com.example.flood.event.application;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.common.api.PageResponse;
import com.example.flood.event.api.EventSummaryResponse;
import com.example.flood.region.application.RegionResolver;
import com.example.flood.region.domain.ResolvedRegion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class EventQueryService {
    private final RegionResolver regionResolver;
    private final EventQueryDataSource dataSource;

    public EventQueryService(RegionResolver regionResolver, EventQueryDataSource dataSource) {
        this.regionResolver = regionResolver;
        this.dataSource = dataSource;
    }

    public PageResponse<EventSummaryResponse> search(EventQuery query) {
        if (query.startTime() == null || query.endTime() == null
            || !query.startTime().isBefore(query.endTime())) {
            throw new ApiException(ErrorCode.INVALID_TIME_RANGE, "startTime must be before endTime");
        }
        if (query.page() < 0 || query.size() < 1 || query.size() > 100) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                "page must be non-negative and size must be between 1 and 100");
        }
        ResolvedRegion region = regionResolver.resolve(query.region());
        long total = dataSource.count(region.id(), query);
        int pages = total == 0 ? 0 : (int) ((total + query.size() - 1) / query.size());
        return new PageResponse<>(dataSource.findPage(region.id(), query),
            query.page(), query.size(), total, pages);
    }
}

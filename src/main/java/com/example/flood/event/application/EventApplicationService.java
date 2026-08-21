package com.example.flood.event.application;

import com.example.flood.event.api.EventResponse;
import com.example.flood.security.application.ApiPrincipal;

public interface EventApplicationService {
    EventResponse create(CreateEventCommand command, ApiPrincipal principal);
    EventResponse update(String eventId, UpdateEventCommand command, ApiPrincipal principal);
}

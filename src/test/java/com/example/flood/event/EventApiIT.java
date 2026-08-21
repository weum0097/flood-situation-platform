package com.example.flood.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.support.MySqlIntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventApiIT extends MySqlIntegrationTestBase {
    @Autowired ObjectMapper objectMapper;

    @Test
    void createsAndQueriesEventWithObservation() throws Exception {
        String body = """
            {"externalEventId":"IT-EVENT-1","sourceSystem":"it",
             "region":{"regionId":"320111"},"eventType":"RIVER_FLOOD",
             "eventName":"Integration flood","startTime":"2026-08-21T00:00:00Z",
             "status":"ONGOING","initialObservation":{"externalObservationId":"IT-OBS-1",
             "observedAt":"2026-08-21T01:00:00Z","impact":{"affectedPopulation":10,
             "trappedPopulation":1,"evacuatedPopulation":2,"vulnerablePopulation":3,
             "injuredPopulation":0,"missingPopulation":0,"deathPopulation":0,
             "damagedHouseholds":0,"collapsedHouses":0,"roadInterruptions":0,
             "criticalFacilitiesAffected":0,"powerOutageHouseholds":0}}}
            """;
        var created = rest.postForEntity("/openapi/v1/disaster-events",
            json(body, "it-event-create"), String.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        JsonNode response = objectMapper.readTree(created.getBody());
        assertThat(response.get("eventId").asText()).startsWith("EVT_");

        var query = rest.exchange("/openapi/v1/disaster-events?regionId=320111"
            + "&startTime=2026-08-21T00:00:00Z&endTime=2026-08-22T00:00:00Z",
            org.springframework.http.HttpMethod.GET, json(null, null), String.class);
        assertThat(query.getStatusCode().value()).isEqualTo(200);
        assertThat(query.getBody()).contains("IT-EVENT-1", "IT-OBS-1");
    }
}

package com.example.flood.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.support.MySqlIntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventApiIT extends MySqlIntegrationTestBase {
    @Autowired ObjectMapper objectMapper;

    @Test
    void createsAndQueriesEventWithObservation() throws Exception {
        String body = """
            {"externalEventId":"IT-EVENT-1","sourceSystem":"it",
             "region":{"regionId":"320111"},"eventType":"RIVER_FLOOD",
             "eventName":"Integration flood","startTime":"2026-08-21T08:00:00+08:00",
             "status":"ONGOING","initialObservation":{"externalObservationId":"IT-OBS-1",
             "observedAt":"2026-08-21T09:00:00+08:00","impact":{"affectedPopulation":10,
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
        assertThat(OffsetDateTime.parse(response.get("startTime").asText()).getOffset())
            .isEqualTo(ZoneOffset.ofHours(8));
        assertThat(OffsetDateTime.parse(response.get("createdAt").asText()).getOffset())
            .isEqualTo(ZoneOffset.ofHours(8));

        var query = rest.exchange("/openapi/v1/disaster-events?regionId=320111"
                + "&startTime={startTime}&endTime={endTime}",
            org.springframework.http.HttpMethod.GET, json(null, null), String.class,
            "2026-08-21T08:00:00+08:00", "2026-08-22T08:00:00+08:00");
        assertThat(query.getStatusCode().value()).isEqualTo(200);
        assertThat(query.getBody()).contains("IT-EVENT-1", "\"observationId\":\"OBS_");
        JsonNode item = objectMapper.readTree(query.getBody()).at("/items/0");
        assertThat(OffsetDateTime.parse(item.get("startTime").asText()).getOffset())
            .isEqualTo(ZoneOffset.ofHours(8));
        assertThat(OffsetDateTime.parse(item.at("/latestObservation/observedAt").asText()).getOffset())
            .isEqualTo(ZoneOffset.ofHours(8));
    }

    @Test
    void rejectsUtcEventAndQueryTimesBecauseRequestsMustUseBeijingOffset() throws Exception {
        String body = """
            {"externalEventId":"IT-UTC-REJECT","sourceSystem":"it",
             "region":{"regionId":"320111"},"eventType":"RIVER_FLOOD",
             "eventName":"UTC should fail","startTime":"2026-08-21T00:00:00Z",
             "status":"ONGOING"}
            """;

        var created = rest.postForEntity("/openapi/v1/disaster-events",
            json(body, "it-utc-reject-create"), String.class);
        assertThat(created.getStatusCode().value()).isEqualTo(422);
        assertThat(created.getBody()).contains("VALIDATION_ERROR", "+08:00");

        var query = rest.exchange("/openapi/v1/disaster-events?regionId=320111"
                + "&startTime=2026-08-21T00:00:00Z&endTime=2026-08-22T00:00:00Z",
            org.springframework.http.HttpMethod.GET, json(null, null), String.class);
        assertThat(query.getStatusCode().value()).isEqualTo(422);
        assertThat(query.getBody()).contains("VALIDATION_ERROR", "+08:00");
    }
}

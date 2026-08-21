package com.example.flood.situation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;

class SituationApiIT extends MySqlIntegrationTestBase {
    @Test
    void importsEventPersistsAssessmentAndAudit() {
        String body = """
            {"region":{"regionId":"320111"},"assessmentTime":"2026-08-21T04:00:00Z",
             "events":[{"externalEventId":"IT-ASSESS-EVENT","sourceSystem":"it-assess",
             "eventType":"RIVER_FLOOD","eventName":"Assessment flood",
             "startTime":"2026-08-21T00:00:00Z","status":"ONGOING",
             "observation":{"externalObservationId":"IT-ASSESS-OBS",
             "observedAt":"2026-08-21T04:00:00Z","hazard":{"maxWaterDepthM":1.5},
             "impact":{"affectedPopulation":100,"trappedPopulation":10,
             "evacuatedPopulation":20,"vulnerablePopulation":5,"injuredPopulation":0,
             "missingPopulation":0,"deathPopulation":0,"damagedHouseholds":0,
             "collapsedHouses":0,"roadInterruptions":0,"criticalFacilitiesAffected":0,
             "powerOutageHouseholds":0}}}]}
            """;
        var response = rest.postForEntity("/openapi/v1/region-situation-assessments",
            json(body, "it-assessment"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).contains("\"situationLevel\":\"HIGH\"");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM region_situation_assessment",
            Integer.class)).isPositive();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM api_audit_log WHERE request_path=?",
            Integer.class, "/openapi/v1/region-situation-assessments")).isPositive();
    }
}

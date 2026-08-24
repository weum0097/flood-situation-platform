package com.example.flood.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;

class AssessmentRollbackIT extends MySqlIntegrationTestBase {
    @Test
    void failedAssessmentRollsBackImportedEventAndObservation() {
        String externalEventId = "IT-ROLLBACK-EVENT";
        String key = "it-assessment-rollback";
        String body = """
            {"region":{"regionId":"320111"},"assessmentTime":"2010-08-21T12:00:00+08:00",
             "events":[{"externalEventId":"IT-ROLLBACK-EVENT","sourceSystem":"it-rollback",
             "eventType":"RIVER_FLOOD","eventName":"Must roll back",
             "startTime":"2010-08-21T08:00:00+08:00","status":"ONGOING",
             "observation":{"externalObservationId":"IT-ROLLBACK-OBS",
             "observedAt":"2010-08-21T12:00:00+08:00","hazard":{"maxWaterDepthM":0.5},
             "impact":{"affectedPopulation":10,"trappedPopulation":1,
             "evacuatedPopulation":2,"vulnerablePopulation":1,"injuredPopulation":0,
             "missingPopulation":0,"deathPopulation":0,"damagedHouseholds":0,
             "collapsedHouses":0,"roadInterruptions":0,"criticalFacilitiesAffected":0,
             "powerOutageHouseholds":0}}}]}
            """;

        var response = rest.postForEntity("/openapi/v1/region-situation-assessments",
            json(body, key), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).contains("NO_ACTIVE_RULE_SET");
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM disaster_event
            WHERE source_system = 'it-rollback' AND external_event_id = ?
            """, Integer.class, externalEventId)).isZero();
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM disaster_event_observation o
            JOIN disaster_event e ON e.id = o.event_id
            WHERE e.source_system = 'it-rollback' AND e.external_event_id = ?
            """, Integer.class, externalEventId)).isZero();
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM idempotency_record
            WHERE operation_code = 'POST:/openapi/v1/region-situation-assessments'
              AND idempotency_key = ?
            """, Integer.class, key)).isZero();
    }
}

package com.example.flood.situation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.support.MySqlIntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SituationApiIT extends MySqlIntegrationTestBase {
    @Autowired ObjectMapper objectMapper;

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

    @Test
    void savedAssessmentSnapshotDoesNotChangeWithLaterEventAndRuleChanges() throws Exception {
        String body = """
            {"region":{"regionId":"320111"},"assessmentTime":"2026-08-21T10:00:00Z",
             "events":[{"externalEventId":"IT-HISTORY-EVENT","sourceSystem":"it-history",
             "eventType":"RIVER_FLOOD","eventName":"Historical name",
             "startTime":"2026-08-21T00:00:00Z","status":"ONGOING",
             "observation":{"externalObservationId":"IT-HISTORY-OBS",
             "observedAt":"2026-08-21T10:00:00Z","hazard":{"maxWaterDepthM":1.5},
             "impact":{"affectedPopulation":120,"trappedPopulation":12,
             "evacuatedPopulation":24,"vulnerablePopulation":6}}}]}
            """;
        var created = rest.postForEntity("/openapi/v1/region-situation-assessments",
            json(body, "it-history-assessment"), String.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        JsonNode response = objectMapper.readTree(created.getBody());
        String assessmentId = response.get("assessmentId").asText();
        String eventId = response.at("/eventResults/0/eventId").asText();
        String snapshotBefore = jdbc.queryForObject("""
            SELECT CAST(input_snapshot AS CHAR) FROM region_situation_assessment
            WHERE public_id = ?
            """, String.class, assessmentId);
        Long ruleSetId = jdbc.queryForObject("""
            SELECT rule_set_id FROM region_situation_assessment WHERE public_id = ?
            """, Long.class, assessmentId);
        Long ruleId = jdbc.queryForObject(
            "SELECT MIN(id) FROM situation_rule WHERE rule_set_id = ?", Long.class, ruleSetId);
        Integer originalPriority = jdbc.queryForObject(
            "SELECT priority FROM situation_rule WHERE id = ?", Integer.class, ruleId);

        String update = """
            {"eventType":"RIVER_FLOOD","eventName":"Changed after assessment",
             "startTime":"2026-08-21T00:00:00Z","status":"ONGOING"}
            """;
        var updated = rest.exchange("/openapi/v1/disaster-events/" + eventId,
            org.springframework.http.HttpMethod.PUT,
            json(update, "it-history-event-update"), String.class);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        try {
            jdbc.update("UPDATE situation_rule SET priority = priority + 1 WHERE id = ?", ruleId);
            String snapshotAfter = jdbc.queryForObject("""
                SELECT CAST(input_snapshot AS CHAR) FROM region_situation_assessment
                WHERE public_id = ?
                """, String.class, assessmentId);
            assertThat(snapshotAfter).isEqualTo(snapshotBefore);
            assertThat(jdbc.queryForObject("""
                SELECT rule_version FROM region_situation_assessment WHERE public_id = ?
                """, String.class, assessmentId)).isEqualTo(response.get("ruleVersion").asText());
        } finally {
            jdbc.update("UPDATE situation_rule SET priority = ? WHERE id = ?",
                originalPriority, ruleId);
        }
    }
}

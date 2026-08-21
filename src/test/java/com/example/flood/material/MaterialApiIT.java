package com.example.flood.material;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.support.MySqlIntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MaterialApiIT extends MySqlIntegrationTestBase {
    @Autowired ObjectMapper objectMapper;

    @Test
    void calculatesDirectDemandUsingSeededBusinessExamples() {
        String body = """
            {"region":{"regionId":"320111"},"situationLevel":"HIGH",
             "supplyDurationHours":24,"population":{"affectedPopulation":100,
             "trappedPopulation":10,"evacuatedPopulation":20,"vulnerablePopulation":5},
             "currentInventory":[]}
            """;
        var response = rest.postForEntity("/openapi/v1/material-demand-calculations",
            json(body, "it-material-direct"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).contains("material-standard-v1.0", "DRINKING_WATER");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM material_demand_item",
            Integer.class)).isPositive();
    }

    @Test
    void regionDataCalculationUsesLatestAssessmentInsideRequestedRange() throws Exception {
        String earlierId = createAssessment("IT-MAT-EARLY", "2026-08-21T06:00:00Z",
            "it-material-assessment-early", 40);
        String latestId = createAssessment("IT-MAT-LATEST", "2026-08-21T08:00:00Z",
            "it-material-assessment-latest", 90);
        String body = """
            {"region":{"regionId":"320111"},"assessmentTimeRange":{
             "startTime":"2026-08-21T05:00:00Z","endTime":"2026-08-21T09:00:00Z"},
             "supplyDurationHours":24,"currentInventory":[]}
            """;

        var response = rest.postForEntity(
            "/openapi/v1/material-demand-calculations/from-region-data",
            json(body, "it-material-from-latest"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).contains("\"sourceType\":\"DATABASE\"");
        assertThat(response.getBody()).contains("\"assessmentId\":\"" + latestId + "\"");
        assertThat(response.getBody()).doesNotContain("\"assessmentId\":\"" + earlierId + "\"");
    }

    private String createAssessment(String externalId, String assessmentTime,
        String idempotencyKey, long affectedPopulation) throws Exception {
        String body = """
            {"region":{"regionId":"320111"},"assessmentTime":"%s",
             "events":[{"externalEventId":"%s","sourceSystem":"it-material-latest",
             "eventType":"RIVER_FLOOD","eventName":"Material source assessment",
             "startTime":"2026-08-21T00:00:00Z","status":"ONGOING",
             "observation":{"externalObservationId":"%s-OBS","observedAt":"%s",
             "hazard":{"maxWaterDepthM":0.5},"impact":{"affectedPopulation":%d,
             "trappedPopulation":1,"evacuatedPopulation":2,"vulnerablePopulation":1}}}]}
            """.formatted(assessmentTime, externalId, externalId, assessmentTime,
                affectedPopulation);
        var response = rest.postForEntity("/openapi/v1/region-situation-assessments",
            json(body, idempotencyKey), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return objectMapper.readTree(response.getBody()).get("assessmentId").asText();
    }
}

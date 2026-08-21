package com.example.flood.material;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;

class MaterialApiIT extends MySqlIntegrationTestBase {
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
}

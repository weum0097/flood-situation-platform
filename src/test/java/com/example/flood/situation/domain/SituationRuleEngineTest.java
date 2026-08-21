package com.example.flood.situation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.event.domain.EventType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SituationRuleEngineTest {
    private final SituationRuleEngine engine = new SituationRuleEngine();

    @Test
    void gteThresholdsAreInclusiveAndNullDoesNotMatch() {
        var rule = rule("depth", ComparisonDirection.GTE, "0.5", "1.5", 10, null);
        assertThat(evaluate("0.4999", rule).level()).isEqualTo(SituationLevel.LOW);
        assertThat(evaluate("0.5", rule).level()).isEqualTo(SituationLevel.MEDIUM);
        assertThat(evaluate("1.5", rule).level()).isEqualTo(SituationLevel.HIGH);
        assertThat(engine.evaluateEvent(new EventSituationInput(EventType.RIVER_FLOOD, Map.of()), List.of(rule)).level())
            .isEqualTo(SituationLevel.LOW);
    }

    @Test
    void lteThresholdsAreInclusive() {
        var rule = rule("clearance", ComparisonDirection.LTE, "2", "1", 10, null);
        assertThat(evaluate("2.1", rule).level()).isEqualTo(SituationLevel.LOW);
        assertThat(evaluate("2", rule).level()).isEqualTo(SituationLevel.MEDIUM);
        assertThat(evaluate("1", rule).level()).isEqualTo(SituationLevel.HIGH);
    }

    @Test
    void eventSpecificRulesAndPriorityOrderingAreDeterministic() {
        var global = rule("z-global", ComparisonDirection.GTE, "0.5", null, 1, null);
        var specific = rule("a-specific", ComparisonDirection.GTE, "0.5", null, 5, EventType.RIVER_FLOOD);
        EventSituationResult result = engine.evaluateEvent(input("1"), List.of(global, specific));
        assertThat(result.matchedRuleCodes()).containsExactly("a-specific", "z-global");
        assertThat(engine.evaluateEvent(
            new EventSituationInput(EventType.OTHER, Map.of(MetricCode.MAX_WATER_DEPTH_M, BigDecimal.ONE)),
            List.of(specific)).level()).isEqualTo(SituationLevel.LOW);
    }

    @Test
    void aggregateUsesHighestLevelAndMediumEscalation() {
        var low = new EventSituationResult(SituationLevel.LOW, List.of());
        var medium = new EventSituationResult(SituationLevel.MEDIUM, List.of("m"));
        var high = new EventSituationResult(SituationLevel.HIGH, List.of("h"));
        assertThat(engine.aggregate(List.of(), 2).level()).isEqualTo(SituationLevel.LOW);
        assertThat(engine.aggregate(List.of(low, medium), 2).level()).isEqualTo(SituationLevel.MEDIUM);
        assertThat(engine.aggregate(List.of(medium, medium), 2).level()).isEqualTo(SituationLevel.HIGH);
        assertThat(engine.aggregate(List.of(low, high), 99).level()).isEqualTo(SituationLevel.HIGH);
    }

    private EventSituationResult evaluate(String value, SituationRuleDefinition rule) {
        return engine.evaluateEvent(input(value), List.of(rule));
    }
    private EventSituationInput input(String depth) {
        return new EventSituationInput(EventType.RIVER_FLOOD,
            Map.of(MetricCode.MAX_WATER_DEPTH_M, new BigDecimal(depth)));
    }
    private SituationRuleDefinition rule(String code, ComparisonDirection direction,
        String medium, String high, int priority, EventType type) {
        return new SituationRuleDefinition(code, type, MetricCode.MAX_WATER_DEPTH_M, direction,
            medium == null ? null : new BigDecimal(medium),
            high == null ? null : new BigDecimal(high), priority, true);
    }
}

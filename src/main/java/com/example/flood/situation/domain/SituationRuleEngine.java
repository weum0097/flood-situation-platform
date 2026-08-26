package com.example.flood.situation.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SituationRuleEngine {
    public EventSituationResult evaluateEvent(EventSituationInput input,
        List<SituationRuleDefinition> rules) {
        List<Match> matches = new ArrayList<>();
        for (SituationRuleDefinition rule : rules) {
            if (!rule.enabled() || (rule.eventType() != null && rule.eventType() != input.eventType())) continue;
            BigDecimal value = input.metrics().get(rule.metricCode());
            if (value == null) continue;
            SituationLevel level = matches(value, rule.highThreshold(), rule.comparisonDirection())
                ? SituationLevel.HIGH
                : matches(value, rule.mediumThreshold(), rule.comparisonDirection())
                    ? SituationLevel.MEDIUM : SituationLevel.LOW;
            if (level != SituationLevel.LOW) matches.add(new Match(rule.ruleCode(), rule.priority(), level));
        }
        matches.sort(Comparator.comparingInt(Match::priority).reversed().thenComparing(Match::code));
        SituationLevel level = matches.stream().map(Match::level)
            .max(Comparator.comparingInt(Enum::ordinal)).orElse(SituationLevel.LOW);
        return new EventSituationResult(level, matches.stream().map(Match::code).toList());
    }

    public RegionSituationResult aggregate(List<EventSituationResult> events, Integer mediumToHighCount) {
        SituationLevel level = events.stream().map(EventSituationResult::level)
            .max(Comparator.comparingInt(Enum::ordinal)).orElse(SituationLevel.LOW);
        long mediumCount = events.stream().filter(event -> event.level() == SituationLevel.MEDIUM).count();
        if (level != SituationLevel.HIGH && mediumToHighCount != null && mediumCount >= mediumToHighCount)
            level = SituationLevel.HIGH;
        return new RegionSituationResult(level, events);
    }

    private static boolean matches(BigDecimal value, BigDecimal threshold,
        ComparisonDirection direction) {
        if (threshold == null) return false;
        int comparison = value.compareTo(threshold);
        return direction == ComparisonDirection.GTE ? comparison >= 0 : comparison <= 0;
    }

    private record Match(String code, int priority, SituationLevel level) {}
}

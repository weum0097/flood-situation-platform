package com.example.flood.situation.application;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.event.domain.EventType;
import com.example.flood.situation.domain.ComparisonDirection;
import com.example.flood.situation.domain.MetricCode;
import com.example.flood.situation.domain.SituationRuleDefinition;
import com.example.flood.situation.infrastructure.SituationRuleMapper;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class ActiveSituationRuleProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveSituationRuleProvider.class);
    private final SituationRuleMapper mapper;

    public ActiveSituationRuleProvider(SituationRuleMapper mapper) {
        this.mapper = mapper;
    }

    public ActiveSituationRuleSet findActive(Instant assessmentTime) {
        var sets = mapper.findActiveRuleSets(assessmentTime);
        if (sets.isEmpty()) {
            throw new ApiException(ErrorCode.NO_ACTIVE_RULE_SET,
                "No situation rule set is active at assessmentTime");
        }
        if (sets.size() != 1) {
            LOGGER.error("Multiple active situation rule sets found at {}: {}",
                assessmentTime, sets.stream().map(row -> row.version()).toList());
            throw new ApiException(ErrorCode.INTERNAL_ERROR,
                "Multiple active situation rule sets overlap");
        }
        var set = sets.getFirst();
        try {
            List<SituationRuleDefinition> rules = mapper.findRules(set.id()).stream()
                .map(row -> new SituationRuleDefinition(row.ruleCode(),
                    row.eventType() == null ? null : EventType.valueOf(row.eventType()),
                    MetricCode.valueOf(row.metricCode()),
                    ComparisonDirection.valueOf(row.comparisonDirection()),
                    row.mediumThreshold(), row.highThreshold(), row.priority(), row.enabled()))
                .toList();
            return new ActiveSituationRuleSet(set.id(), set.version(),
                set.mediumToHighCount(), rules);
        } catch (IllegalArgumentException invalidConfiguration) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR,
                "Situation rule set contains an unsupported value");
        }
    }
}

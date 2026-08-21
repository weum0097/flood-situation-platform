package com.example.flood.situation.domain;

import java.util.List;

public record EventSituationResult(SituationLevel level, List<String> matchedRuleCodes) {
    public EventSituationResult { matchedRuleCodes = List.copyOf(matchedRuleCodes); }
}

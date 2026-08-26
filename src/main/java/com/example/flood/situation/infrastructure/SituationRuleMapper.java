package com.example.flood.situation.infrastructure;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SituationRuleMapper {
    List<SituationRuleSetRow> findActiveRuleSets(@Param("assessmentTime") Instant assessmentTime);
    List<SituationRuleRow> findRules(@Param("ruleSetId") long ruleSetId);
}

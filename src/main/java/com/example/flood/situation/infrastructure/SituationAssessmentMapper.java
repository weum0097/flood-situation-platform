package com.example.flood.situation.infrastructure;

import com.example.flood.situation.application.SituationAssessmentQuery;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SituationAssessmentMapper {
    int insert(SituationAssessmentRow row);
    int insertEvent(SituationAssessmentEventRow row);
    long countForQuery(@Param("regionId") long regionId,
        @Param("query") SituationAssessmentQuery query);
    List<SituationAssessmentSummaryRow> findPageForQuery(@Param("regionId") long regionId,
        @Param("query") SituationAssessmentQuery query);
}

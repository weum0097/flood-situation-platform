package com.example.flood.situation.infrastructure;

import com.example.flood.situation.application.SituationAssessmentQuery;
import com.example.flood.situation.application.SavedSituationSnapshot;
import com.example.flood.situation.application.SituationSnapshotLookup;
import com.example.flood.situation.domain.SituationLevel;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SituationAssessmentMapper extends SituationSnapshotLookup {
    int insert(SituationAssessmentRow row);
    int insertEvent(SituationAssessmentEventRow row);
    long countForQuery(@Param("regionId") long regionId,
        @Param("query") SituationAssessmentQuery query);
    List<SituationAssessmentSummaryRow> findPageForQuery(@Param("regionId") long regionId,
        @Param("query") SituationAssessmentQuery query);

    Optional<SavedSituationSnapshotRow> findLatestRow(@Param("regionId") long regionId,
        @Param("start") Instant start, @Param("end") Instant end);

    @Override
    default Optional<SavedSituationSnapshot> findLatest(long regionDatabaseId,
        Instant startInclusive, Instant endExclusive) {
        return findLatestRow(regionDatabaseId, startInclusive, endExclusive).map(row ->
            new SavedSituationSnapshot(row.databaseId(), row.publicId(),
                SituationLevel.valueOf(row.level()), row.affectedPopulation(),
                row.trappedPopulation(), row.evacuatedPopulation(), row.vulnerablePopulation(),
                row.assessmentTime()));
    }
}

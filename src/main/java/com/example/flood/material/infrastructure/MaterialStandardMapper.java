package com.example.flood.material.infrastructure;

import com.example.flood.situation.domain.SituationLevel;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MaterialStandardMapper {
    List<MaterialStandardSetRow> findActiveSets(@Param("instant") Instant instant);
    List<MaterialStandardRow> findDefinitions(@Param("setId") long setId,
        @Param("level") SituationLevel level, @Param("scopeIds") List<Long> scopeIds);
}

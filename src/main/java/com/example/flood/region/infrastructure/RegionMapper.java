package com.example.flood.region.infrastructure;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RegionMapper {

    @Select("""
        SELECT id, region_code, region_name, parent_id, region_level, status
        FROM region
        WHERE status = 'ACTIVE' AND region_code = #{regionCode}
        LIMIT 1
        """)
    Optional<RegionRow> findActiveByCode(@Param("regionCode") String regionCode);

    @Select("""
        SELECT id, region_code, region_name, parent_id, region_level, status
        FROM region
        WHERE status = 'ACTIVE' AND region_name = #{regionName}
        ORDER BY id
        """)
    List<RegionRow> findActiveByName(@Param("regionName") String regionName);

    @Select("""
        SELECT id, region_code, region_name, parent_id, region_level, status
        FROM region
        WHERE status = 'ACTIVE' AND id = #{id}
        LIMIT 1
        """)
    Optional<RegionRow> findActiveById(@Param("id") long id);
}

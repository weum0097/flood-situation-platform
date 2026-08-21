package com.example.flood.event.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import com.example.flood.event.application.EventQuery;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DisasterEventMapper {
    long countForQuery(@Param("regionId") long regionId, @Param("query") EventQuery query);

    List<EventQueryRow> findPageForQuery(
        @Param("regionId") long regionId, @Param("query") EventQuery query);

    @Select("SELECT COUNT(*) FROM disaster_event WHERE source_system=#{source} AND external_event_id=#{external}")
    int countByExternal(@Param("source") String source, @Param("external") String external);

    @Insert("""
        INSERT INTO disaster_event (public_id, external_event_id, source_system, region_id,
          event_type, event_name, start_time, end_time, status, created_by_client_id)
        VALUES (#{publicId}, #{externalId}, #{source}, #{regionId}, #{eventType},
          #{eventName}, #{start}, #{end}, #{status}, #{clientId})
        """)
    int insertEvent(@Param("publicId") String publicId, @Param("externalId") String externalId,
        @Param("source") String source, @Param("regionId") long regionId,
        @Param("eventType") String eventType, @Param("eventName") String eventName,
        @Param("start") Instant start, @Param("end") Instant end,
        @Param("status") String status, @Param("clientId") long clientId);

    @Select("""
        SELECT e.id, e.public_id, e.external_event_id, e.source_system, e.region_id,
          r.region_code, r.region_name, e.event_type, e.event_name, e.start_time,
          e.end_time, e.status, e.created_by_client_id, e.created_at, e.updated_at
        FROM disaster_event e JOIN region r ON r.id=e.region_id
        WHERE e.public_id=#{publicId} FOR UPDATE
        """)
    Optional<DisasterEventRow> findByPublicIdForUpdate(@Param("publicId") String publicId);

    @Select("""
        SELECT e.id, e.public_id, e.external_event_id, e.source_system, e.region_id,
          r.region_code, r.region_name, e.event_type, e.event_name, e.start_time,
          e.end_time, e.status, e.created_by_client_id, e.created_at, e.updated_at
        FROM disaster_event e JOIN region r ON r.id=e.region_id
        WHERE e.public_id=#{publicId}
        """)
    Optional<DisasterEventRow> findByPublicId(@Param("publicId") String publicId);

    @Update("""
        UPDATE disaster_event SET event_type=#{eventType}, event_name=#{eventName},
          start_time=#{start}, end_time=#{end}, status=#{status}
        WHERE id=#{id}
        """)
    int updateMutable(@Param("id") long id, @Param("eventType") String eventType,
        @Param("eventName") String eventName, @Param("start") Instant start,
        @Param("end") Instant end, @Param("status") String status);
}

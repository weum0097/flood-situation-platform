package com.example.flood.common.idempotency;

import java.time.Instant;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IdempotencyRecordMapper {
    @Insert("""
        INSERT INTO idempotency_record
          (client_id, operation_code, idempotency_key, request_hash, resource_type, expires_at)
        VALUES (#{clientId}, #{operationCode}, #{key}, #{hash},
          CASE
            WHEN #{operationCode} LIKE '%observations%' THEN 'OBSERVATION'
            WHEN #{operationCode} LIKE '%region-situation%' THEN 'ASSESSMENT'
            WHEN #{operationCode} LIKE '%material-demand%' THEN 'MATERIAL'
            ELSE 'EVENT'
          END, #{expiresAt})
        """)
    int insertPending(@Param("clientId") long clientId,
        @Param("operationCode") String operationCode, @Param("key") String key,
        @Param("hash") byte[] hash, @Param("expiresAt") Instant expiresAt);

    @Select("""
        SELECT id, client_id, operation_code, idempotency_key, request_hash,
               response_status, CAST(response_body AS CHAR) response_body,
               resource_type, resource_public_id
        FROM idempotency_record
        WHERE client_id = #{clientId} AND operation_code = #{operationCode}
          AND idempotency_key = #{key}
        LIMIT 1
        """)
    Optional<IdempotencyRecordRow> find(@Param("clientId") long clientId,
        @Param("operationCode") String operationCode, @Param("key") String key);

    @Update("""
        UPDATE idempotency_record SET response_status = #{status},
          response_body = CAST(#{body} AS JSON), resource_type = #{resourceType},
          resource_public_id = #{resourcePublicId}
        WHERE client_id = #{clientId} AND operation_code = #{operationCode}
          AND idempotency_key = #{key} AND response_status IS NULL
        """)
    int complete(@Param("clientId") long clientId, @Param("operationCode") String operationCode,
        @Param("key") String key, @Param("status") int status, @Param("body") String body,
        @Param("resourceType") String resourceType,
        @Param("resourcePublicId") String resourcePublicId);

    @Update("""
        DELETE FROM idempotency_record
        WHERE response_status IS NOT NULL AND expires_at < #{now}
        ORDER BY id LIMIT #{limit}
        """)
    int deleteExpired(@Param("now") Instant now, @Param("limit") int limit);
}

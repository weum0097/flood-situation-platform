package com.example.flood.audit.infrastructure;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiAuditLogMapper {
    @Insert("""
        INSERT INTO api_audit_log
          (request_id, client_id, api_key_id, http_method, request_path,
           response_status, error_code, duration_ms, remote_ip, request_hash)
        VALUES (#{requestId}, #{clientId}, #{apiKeyId}, #{httpMethod}, #{requestPath},
          #{responseStatus}, #{errorCode}, #{durationMs}, #{remoteIp}, #{requestHash})
        """)
    int insert(ApiAuditLogRow row);
}

SELECT
    request_id,
    http_method,
    request_path,
    response_status,
    error_code,
    duration_ms,
    remote_ip,
    created_at
FROM api_audit_log
ORDER BY id DESC
LIMIT 50;
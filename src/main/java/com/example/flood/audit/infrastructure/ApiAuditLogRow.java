package com.example.flood.audit.infrastructure;

public record ApiAuditLogRow(String requestId, Long clientId, Long apiKeyId,
    String httpMethod, String requestPath, int responseStatus, String errorCode,
    long durationMs, String remoteIp, byte[] requestHash) {}

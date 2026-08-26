package com.example.flood.audit.application;

public record ApiAuditRecord(String requestId, Long clientId, Long apiKeyId,
    String httpMethod, String requestPath, int responseStatus, String errorCode,
    long durationMs, String remoteIp, byte[] requestHash) {}

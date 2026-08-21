package com.example.flood.security.infrastructure;

public record ApiClientRow(long id, String clientCode, String status, int rateLimitPerMinute) {}

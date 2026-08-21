package com.example.flood.audit.application;

import com.example.flood.audit.infrastructure.ApiAuditLogMapper;
import com.example.flood.audit.infrastructure.ApiAuditLogRow;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class ApiAuditService {
    private final ApiAuditLogMapper mapper;
    public ApiAuditService(ApiAuditLogMapper mapper) { this.mapper = mapper; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ApiAuditRecord record) {
        mapper.insert(new ApiAuditLogRow(record.requestId(), record.clientId(), record.apiKeyId(),
            record.httpMethod(), record.requestPath(), record.responseStatus(), record.errorCode(),
            record.durationMs(), record.remoteIp(), record.requestHash()));
    }
}

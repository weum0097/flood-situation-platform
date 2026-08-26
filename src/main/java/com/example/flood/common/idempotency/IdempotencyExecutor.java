package com.example.flood.common.idempotency;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ClientIdentity;
import com.example.flood.common.api.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class IdempotencyExecutor {
    private final IdempotencyRecordMapper mapper;
    private final CanonicalRequestHasher hasher;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final IdempotencyProperties properties;

    public IdempotencyExecutor(IdempotencyRecordMapper mapper, CanonicalRequestHasher hasher,
        ObjectMapper objectMapper, TransactionTemplate transactionTemplate, Clock clock,
        IdempotencyProperties properties) {
        this.mapper = mapper;
        this.hasher = hasher;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.properties = properties;
    }

    public <T> IdempotentResult<T> execute(ClientIdentity principal, IdempotentOperation operation,
        String key, Object fingerprintInput, Class<T> responseType,
        Supplier<OperationResult<T>> operationSupplier) {
        validateKey(key);
        byte[] requestHash = hasher.hash(operation, fingerprintInput);
        return transactionTemplate.execute(status -> executeInTransaction(
            principal, operation, key, requestHash, responseType, operationSupplier));
    }

    private <T> IdempotentResult<T> executeInTransaction(ClientIdentity principal,
        IdempotentOperation operation, String key, byte[] requestHash, Class<T> responseType,
        Supplier<OperationResult<T>> supplier) {
        try {
            mapper.insertPending(principal.clientId(), operation.operationCode(), key,
                requestHash, clock.instant().plus(properties.ttl()));
        } catch (DuplicateKeyException duplicate) {
            IdempotencyRecordRow existing = mapper.find(
                principal.clientId(), operation.operationCode(), key).orElseThrow(() -> duplicate);
            if (!MessageDigest.isEqual(requestHash, existing.requestHash())) {
                throw new ApiException(ErrorCode.IDEMPOTENCY_CONFLICT,
                    "Idempotency key was already used with another request");
            }
            if (existing.responseStatus() == null || existing.responseBody() == null) {
                throw new ApiException(ErrorCode.IDEMPOTENCY_CONFLICT,
                    "A request with this idempotency key is still in progress");
            }
            try {
                T body = objectMapper.readValue(existing.responseBody(), responseType);
                return new IdempotentResult<>(existing.responseStatus(), body, true);
            } catch (Exception exception) {
                throw new IllegalStateException("Stored idempotency response is unreadable", exception);
            }
        }

        OperationResult<T> result = supplier.get();
        try {
            String responseBody = objectMapper.writeValueAsString(result.body());
            mapper.complete(principal.clientId(), operation.operationCode(), key,
                result.httpStatus(), responseBody, result.resourceType(), result.resourcePublicId());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not store idempotency response", exception);
        }
        return new IdempotentResult<>(result.httpStatus(), result.body(), false);
    }

    private static void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 128) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                "Idempotency-Key must contain between 1 and 128 characters");
        }
    }
}

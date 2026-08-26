package com.example.flood.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.security.application.ApiPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class IdempotencyExecutorTest {

    private IdempotencyRecordMapper mapper;
    private IdempotencyExecutor executor;
    private final ApiPrincipal principal = new ApiPrincipal(10, 20, "client", Set.of());

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mapper = mock(IdempotencyRecordMapper.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
            ((TransactionCallback<Object>) invocation.getArgument(0))
                .doInTransaction(mock(TransactionStatus.class)));
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        executor = new IdempotencyExecutor(
            mapper, new CanonicalRequestHasher(objectMapper), objectMapper,
            transactionTemplate, Clock.fixed(Instant.parse("2026-08-21T00:00:00Z"), ZoneOffset.UTC),
            new IdempotencyProperties(Duration.ofHours(24)));
    }

    @Test
    void executesFirstRequestAndReturnsOriginalStatus() {
        when(mapper.insertPending(anyLong(), anyString(), anyString(), any(), any())).thenReturn(1);

        IdempotentResult<TestBody> result = executor.execute(
            principal, operation("POST:/events"), "same-key", Map.of("name", "a"),
            TestBody.class, () -> new OperationResult<>(201, new TestBody("EVT_1"), "EVENT", "EVT_1"));

        assertThat(result.httpStatus()).isEqualTo(201);
        assertThat(result.body().id()).isEqualTo("EVT_1");
        assertThat(result.replayed()).isFalse();
    }

    @Test
    void replaysCompletedRequestWithoutCallingSupplier() throws Exception {
        byte[] hash = new CanonicalRequestHasher(new ObjectMapper())
            .hash(operation("POST:/events"), Map.of("name", "a"));
        when(mapper.insertPending(anyLong(), anyString(), anyString(), any(), any()))
            .thenThrow(new DuplicateKeyException("duplicate"));
        when(mapper.find(10, "POST:/events", "same-key")).thenReturn(Optional.of(
            new IdempotencyRecordRow(1, 10, "POST:/events", "same-key", hash,
                201, "{\"id\":\"EVT_1\"}", "EVENT", "EVT_1")));
        AtomicInteger calls = new AtomicInteger();

        IdempotentResult<TestBody> result = executor.execute(
            principal, operation("POST:/events"), "same-key", Map.of("name", "a"),
            TestBody.class, () -> { calls.incrementAndGet(); return null; });

        assertThat(result.replayed()).isTrue();
        assertThat(result.body().id()).isEqualTo("EVT_1");
        assertThat(calls).hasValue(0);
    }

    @Test
    void rejectsDifferentBodyButAllowsSameKeyForAnotherOperation() {
        when(mapper.insertPending(anyLong(), anyString(), anyString(), any(), any()))
            .thenThrow(new DuplicateKeyException("duplicate"));
        byte[] original = new CanonicalRequestHasher(new ObjectMapper())
            .hash(operation("POST:/events"), Map.of("name", "a"));
        when(mapper.find(10, "POST:/events", "same-key")).thenReturn(Optional.of(
            new IdempotencyRecordRow(1, 10, "POST:/events", "same-key", original,
                201, "{\"id\":\"EVT_1\"}", "EVENT", "EVT_1")));

        assertThatThrownBy(() -> executor.execute(
                principal, operation("POST:/events"), "same-key", Map.of("name", "b"),
                TestBody.class, () -> null))
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));

        when(mapper.insertPending(eq(10L), eq("PUT:/events/{id}"), eq("same-key"), any(), any()))
            .thenReturn(1);
        IdempotentResult<TestBody> other = executor.execute(
            principal, operation("PUT:/events/{id}"), "same-key", Map.of("name", "b"),
            TestBody.class, () -> new OperationResult<>(200, new TestBody("EVT_1"), "EVENT", "EVT_1"));
        assertThat(other.replayed()).isFalse();
    }

    private static IdempotentOperation operation(String code) {
        return new IdempotentOperation(code, Map.of(), Map.of());
    }

    record TestBody(String id) {}
}

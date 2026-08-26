package com.example.flood.common.api;

import java.util.Optional;

public final class RequestContextHolder {

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    private RequestContextHolder() {}

    public static void set(RequestContext context) {
        CONTEXT.set(context);
    }

    public static Optional<RequestContext> current() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static RequestContext requireCurrent() {
        return current().orElseThrow(() -> new IllegalStateException("No request context is active"));
    }

    public static void clear() {
        CONTEXT.remove();
    }
}

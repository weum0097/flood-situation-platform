package com.example.flood.security.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiKeyHasherTest {

    private final ApiKeyHasher hasher = new ApiKeyHasher("unit-test-pepper");

    @Test
    void createsSha256LengthDigestAndUsesConstantTimeVerification() {
        byte[] digest = hasher.hash("flood_live_abcdefgh.abcdefghijklmnopqrstuvwxyz012345");

        assertThat(digest).hasSize(32);
        assertThat(hasher.matches("flood_live_abcdefgh.abcdefghijklmnopqrstuvwxyz012345", digest))
            .isTrue();
        assertThat(hasher.matches("flood_live_abcdefgh.abcdefghijklmnopqrstuvwxyz012346", digest))
            .isFalse();
    }
}

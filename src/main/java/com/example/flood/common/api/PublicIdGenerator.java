package com.example.flood.common.api;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;

public class PublicIdGenerator {

    private static final char[] BASE32 = "0123456789abcdefghjkmnpqrstvwxyz".toCharArray();
    private final Clock clock;
    private final SecureRandom random;

    public PublicIdGenerator(Clock clock) {
        this(clock, new SecureRandom());
    }

    PublicIdGenerator(Clock clock, SecureRandom random) {
        this.clock = Objects.requireNonNull(clock);
        this.random = Objects.requireNonNull(random);
    }

    public String next(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        char[] value = new char[26];
        long timestamp = clock.millis();
        for (int index = 9; index >= 0; index--) {
            value[index] = BASE32[(int) (timestamp & 31)];
            timestamp >>>= 5;
        }
        synchronized (random) {
            for (int index = 10; index < value.length; index++) {
                value[index] = BASE32[random.nextInt(32)];
            }
        }
        return prefix + new String(value);
    }
}

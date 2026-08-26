package com.example.flood.security.application;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class ApiKeyHasher {

    private final byte[] pepper;

    public ApiKeyHasher(String pepper) {
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalArgumentException("API key pepper must not be blank");
        }
        this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] hash(String rawKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            return mac.doFinal(rawKey.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    public boolean matches(String rawKey, byte[] expectedDigest) {
        return expectedDigest != null && MessageDigest.isEqual(hash(rawKey), expectedDigest);
    }
}

package com.contented.contented.contentlet;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generates time-ordered UUIDv7 values so new ids sort by creation time and append to the right
 * edge of the primary-key B-tree (cheaper inserts, scan locality for the index-rebuild job).
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID generate() {
        long timestamp = System.currentTimeMillis() & 0xFFFFFFFFFFFFL;        // 48-bit unix millis
        long randomA = RANDOM.nextInt(0x1000);                               // 12 random bits
        long mostSignificantBits = (timestamp << 16) | (0x7L << 12) | randomA; // ...| version 7
        long leastSignificantBits = (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L; // variant 0b10 + 62 random bits
        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}

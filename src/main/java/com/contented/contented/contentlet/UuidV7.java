package com.contented.contented.contentlet;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;

import java.util.UUID;

/**
 * Generates time-ordered UUIDv7 values (via java-uuid-generator) so new ids sort by creation time
 * and append to the right edge of the primary-key B-tree: cheaper inserts, scan locality for the
 * index-rebuild job.
 */
public final class UuidV7 {

    private static final NoArgGenerator GENERATOR = Generators.timeBasedEpochGenerator();

    private UuidV7() {
    }

    public static UUID generate() {
        return GENERATOR.generate();
    }
}

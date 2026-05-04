package dev.iadev.application.product;

public record CreateCapabilitiesResult(
        int capabilityCount,
        int skippedCount,
        boolean rnfInheritanceWritten) {
}

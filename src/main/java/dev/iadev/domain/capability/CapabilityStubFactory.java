package dev.iadev.domain.capability;

import java.util.List;

public final class CapabilityStubFactory {

    private static final String C1_CAPABILITY_ID = "capability-c1";

    public Capability createC1Stub(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be null or blank");
        }
        return new Capability(C1_CAPABILITY_ID, productId, List.of());
    }
}

package dev.iadev.domain.capability;

import java.util.List;

public final class Capability {

    private final String capabilityId;
    private final String productId;
    private final List<RNFOverride> rnfOverrides;

    public Capability(String capabilityId, String productId, List<RNFOverride> rnfOverrides) {
        this.capabilityId = capabilityId;
        this.productId = productId;
        this.rnfOverrides = List.copyOf(rnfOverrides);
    }

    public String capabilityId() {
        return capabilityId;
    }

    public String productId() {
        return productId;
    }

    public List<RNFOverride> rnfOverrides() {
        return rnfOverrides;
    }
}

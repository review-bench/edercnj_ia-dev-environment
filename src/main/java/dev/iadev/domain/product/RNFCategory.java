package dev.iadev.domain.product;

public enum RNFCategory {
    PERFORMANCE(true),
    SCALABILITY(true),
    RELIABILITY(true),
    SECURITY(true),
    COMPLIANCE(true),
    OBSERVABILITY(true),
    DATA_INTEGRITY(false),
    MAINTAINABILITY(false),
    PORTABILITY(false),
    USABILITY(false);

    private final boolean mandatory;

    RNFCategory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isMandatory() {
        return mandatory;
    }
}

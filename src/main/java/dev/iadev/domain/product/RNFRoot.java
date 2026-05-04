package dev.iadev.domain.product;

public record RNFRoot(
        RNFCategory category,
        String description,
        String verificationMethod,
        boolean mandatory
) {
}

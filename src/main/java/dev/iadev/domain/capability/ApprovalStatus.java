package dev.iadev.domain.capability;

public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static ApprovalStatus fromString(String value) {
        if (value == null || value.isBlank()) return PENDING;
        return switch (value.toUpperCase().trim()) {
            case "APPROVED" -> APPROVED;
            case "REJECTED" -> REJECTED;
            default -> PENDING;
        };
    }
}

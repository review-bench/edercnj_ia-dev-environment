package dev.iadev.domain.capability;

import java.util.Locale;

public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static ApprovalStatus fromString(String value) {
        if (value == null || value.isBlank() || "—".equals(value)) {
            return null;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}

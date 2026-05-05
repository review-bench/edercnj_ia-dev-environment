package dev.iadev.domain.capability;

import dev.iadev.domain.product.RNFCategory;

public record RNFOverride(
        RNFCategory category,
        String originalValue,
        String overrideValue,
        boolean noRelaxed,
        String justification,
        ApprovalStatus approvalStatus,
        String approver
) {
    public static RNFOverride noRelax(RNFCategory category, String originalValue) {
        return new RNFOverride(category, originalValue, null, true, null, null, null);
    }

    public static RNFOverride withOverride(RNFCategory category, String originalValue,
                                           String overrideValue, String justification) {
        return new RNFOverride(category, originalValue, overrideValue, false, justification, null, null);
    }

    public static RNFOverride withApproval(
            RNFCategory category,
            String originalValue,
            String overrideValue,
            String justification,
            ApprovalStatus approvalStatus,
            String approver) {
        return new RNFOverride(category, originalValue, overrideValue, false, justification, approvalStatus, approver);
    }

    public boolean isRelaxed() {
        return !noRelaxed;
    }

    public boolean hasFormalApproval() {
        return approvalStatus == ApprovalStatus.APPROVED && approver != null && !approver.isBlank();
    }
}

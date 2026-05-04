package dev.iadev.domain.capability;

import dev.iadev.domain.product.RNFCategory;

public record ApprovalRequest(
        String requestId,
        RNFCategory category,
        String originalValue,
        String overrideValue,
        String justification,
        String approver,
        ApprovalStatus status,
        String note
) {
    public ApprovalRequest withStatus(ApprovalStatus newStatus, String newNote) {
        return new ApprovalRequest(requestId, category, originalValue, overrideValue,
                justification, approver, newStatus, newNote);
    }
}

package dev.iadev.application.capability;

import dev.iadev.domain.capability.ApprovalRequest;
import dev.iadev.domain.capability.ApprovalStatus;
import dev.iadev.domain.capability.RNFOverride;
import dev.iadev.domain.capability.RNFOverrideApprovalPort;

import java.util.List;
import java.util.UUID;

public final class RNFOverrideApprovalUseCase {

    private final RNFOverrideApprovalPort port;

    public RNFOverrideApprovalUseCase(RNFOverrideApprovalPort port) {
        this.port = port;
    }

    public String requestApproval(RNFOverride override, String approver) {
        ApprovalRequest request = new ApprovalRequest(
                UUID.randomUUID().toString(),
                override.category(),
                override.originalValue(),
                override.overrideValue(),
                override.justification(),
                approver,
                ApprovalStatus.PENDING,
                null
        );
        return port.submitRequest(request);
    }

    public void approve(String requestId) {
        port.updateStatus(requestId, ApprovalStatus.APPROVED, null);
    }

    public void reject(String requestId, String reason) {
        port.updateStatus(requestId, ApprovalStatus.REJECTED, reason);
    }

    public List<ApprovalRequest> listByStatus(ApprovalStatus status) {
        return port.findByStatus(status);
    }
}

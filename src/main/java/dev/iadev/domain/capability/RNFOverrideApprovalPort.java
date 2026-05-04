package dev.iadev.domain.capability;

import dev.iadev.domain.capability.ApprovalRequest;

import java.util.List;

public interface RNFOverrideApprovalPort {

    String submitRequest(ApprovalRequest request);

    void updateStatus(String requestId, ApprovalStatus status, String note);

    List<ApprovalRequest> findByStatus(ApprovalStatus status);
}

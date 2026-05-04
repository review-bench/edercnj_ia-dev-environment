package dev.iadev.adapter.outbound.approval;

import dev.iadev.domain.capability.ApprovalRequest;
import dev.iadev.domain.capability.ApprovalStatus;
import dev.iadev.domain.capability.RNFOverrideApprovalPort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RNFOverrideApprovalAdapter implements RNFOverrideApprovalPort {

    private final List<ApprovalRequest> store = new ArrayList<>();

    @Override
    public String submitRequest(ApprovalRequest request) {
        store.add(request);
        return request.requestId();
    }

    @Override
    public void updateStatus(String requestId, ApprovalStatus status, String note) {
        for (int i = 0; i < store.size(); i++) {
            if (store.get(i).requestId().equals(requestId)) {
                store.set(i, store.get(i).withStatus(status, note));
                return;
            }
        }
    }

    @Override
    public List<ApprovalRequest> findByStatus(ApprovalStatus status) {
        return store.stream().filter(r -> r.status() == status).toList();
    }
}

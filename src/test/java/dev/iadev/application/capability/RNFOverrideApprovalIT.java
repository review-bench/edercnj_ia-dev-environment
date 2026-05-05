package dev.iadev.application.capability;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.adapter.outbound.approval.RNFOverrideApprovalAdapter;
import dev.iadev.domain.capability.ApprovalRequest;
import dev.iadev.domain.capability.ApprovalStatus;
import dev.iadev.domain.capability.RNFOverride;
import dev.iadev.domain.product.RNFCategory;
import java.util.List;
import org.junit.jupiter.api.Test;

class RNFOverrideApprovalIT {

    private final RNFOverrideApprovalAdapter adapter = new RNFOverrideApprovalAdapter();
    private final RNFOverrideApprovalUseCase useCase = new RNFOverrideApprovalUseCase(adapter);

    @Test
    void requestApproval_setsToPending() {
        RNFOverride override =
                RNFOverride.withOverride(
                        RNFCategory.PERFORMANCE, "P99 < 3s", "P99 < 1s", "Stricter SLA needed");

        useCase.requestApproval(override, "approver@example.com");

        List<ApprovalRequest> pending = useCase.listByStatus(ApprovalStatus.PENDING);
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).category()).isEqualTo(RNFCategory.PERFORMANCE);
        assertThat(pending.get(0).approver()).isEqualTo("approver@example.com");
        assertThat(pending.get(0).status()).isEqualTo(ApprovalStatus.PENDING);
    }

    @Test
    void approveOverride_changesStatusToApproved() {
        RNFOverride override =
                RNFOverride.withOverride(
                        RNFCategory.RELIABILITY, "99.9%", "99.5%", "Gateway dependency");
        useCase.requestApproval(override, "cto@example.com");
        List<ApprovalRequest> pending = useCase.listByStatus(ApprovalStatus.PENDING);
        String requestId = pending.get(0).requestId();

        useCase.approve(requestId);

        List<ApprovalRequest> approved = useCase.listByStatus(ApprovalStatus.APPROVED);
        assertThat(approved).hasSize(1);
        assertThat(approved.get(0).status()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void rejectOverride_changesStatusToRejected() {
        RNFOverride override =
                RNFOverride.withOverride(
                        RNFCategory.OBSERVABILITY, "full trace", "sampled 10%", "Cost concern");
        useCase.requestApproval(override, "vp@example.com");
        List<ApprovalRequest> pending = useCase.listByStatus(ApprovalStatus.PENDING);
        String requestId = pending.get(0).requestId();

        useCase.reject(requestId, "Observability cannot be reduced below 50% sampling");

        List<ApprovalRequest> rejected = useCase.listByStatus(ApprovalStatus.REJECTED);
        assertThat(rejected).hasSize(1);
        assertThat(rejected.get(0).status()).isEqualTo(ApprovalStatus.REJECTED);
    }
}

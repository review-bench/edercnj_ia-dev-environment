package dev.iadevkit.audit;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AuditPolicy {

    private final Map<String, List<AuditRule>> rulesByTargetId;

    public AuditPolicy(Map<String, List<AuditRule>> rulesByTargetId) {
        this.rulesByTargetId = Map.copyOf(Objects.requireNonNull(rulesByTargetId));
    }

    public List<AuditRule> rulesFor(AuditTarget target) {
        return rulesByTargetId.getOrDefault(target.id(), List.of());
    }
}

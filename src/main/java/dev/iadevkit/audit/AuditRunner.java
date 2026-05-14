package dev.iadevkit.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AuditRunner {

    public List<AuditViolation> run(List<AuditTarget> targets, AuditPolicy policy) {
        Objects.requireNonNull(targets, "targets must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        List<AuditViolation> violations = new ArrayList<>();
        for (AuditTarget target : targets) {
            for (AuditRule rule : policy.rulesFor(target)) {
                violations.addAll(rule.evaluate(target));
            }
        }
        return List.copyOf(violations);
    }
}

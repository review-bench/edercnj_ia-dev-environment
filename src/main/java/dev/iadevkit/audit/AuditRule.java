package dev.iadevkit.audit;

import java.util.List;

public interface AuditRule {

    String code();

    String description();

    List<AuditViolation> evaluate(AuditTarget target);
}

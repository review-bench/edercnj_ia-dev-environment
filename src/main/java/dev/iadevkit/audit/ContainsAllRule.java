package dev.iadevkit.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ContainsAllRule implements AuditRule {

    private final String code;
    private final String description;
    private final List<String> requiredSignals;

    public ContainsAllRule(String code, String description, List<String> requiredSignals) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.requiredSignals = List.copyOf(Objects.requireNonNull(requiredSignals));
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public List<AuditViolation> evaluate(AuditTarget target) {
        List<String> missingSignals = new ArrayList<>();
        for (String signal : requiredSignals) {
            if (!target.content().contains(signal)) {
                missingSignals.add(signal);
            }
        }
        if (missingSignals.isEmpty()) {
            return List.of();
        }
        return List.of(
                new AuditViolation(
                        target.id(),
                        code,
                        description + " | missing signals: " + String.join(", ", missingSignals)));
    }
}

package dev.iadevkit.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ContainsNoneRule implements AuditRule {

    private final String code;
    private final String description;
    private final List<String> forbiddenSignals;

    public ContainsNoneRule(String code, String description, List<String> forbiddenSignals) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.forbiddenSignals = List.copyOf(Objects.requireNonNull(forbiddenSignals));
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
        List<String> foundSignals = new ArrayList<>();
        for (String signal : forbiddenSignals) {
            if (target.content().contains(signal)) {
                foundSignals.add(signal);
            }
        }
        if (foundSignals.isEmpty()) {
            return List.of();
        }
        return List.of(
                new AuditViolation(
                        target.id(),
                        code,
                        description + " | forbidden signals: " + String.join(", ", foundSignals)));
    }
}

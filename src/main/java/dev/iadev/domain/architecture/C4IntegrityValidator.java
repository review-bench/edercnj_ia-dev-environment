package dev.iadev.domain.architecture;

import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4CodeLevelValidator.ValidationResult;
import java.util.ArrayList;
import java.util.List;

public final class C4IntegrityValidator {

    public enum ViolationType {
        OUTWARD_DEPENDENCY,
        LAYER_CROSSING,
        MISSING_CONTENT
    }

    public enum Severity {
        ERROR,
        WARN
    }

    public record Violation(String message, ViolationType type, Severity severity) {
        public Violation {
            if (message == null || message.isBlank())
                throw new IllegalArgumentException("message must not be blank");
            if (type == null) throw new IllegalArgumentException("type must not be null");
            if (severity == null) throw new IllegalArgumentException("severity must not be null");
        }
    }

    public record IntegrityResult(boolean valid, List<Violation> violations) {
        public static IntegrityResult ok() {
            return new IntegrityResult(true, List.of());
        }
    }

    private final C4CodeLevelValidator codeValidator = new C4CodeLevelValidator();

    public IntegrityResult validate(
            C4Diagram diagram, List<CodeEntry> classes, List<Dependency> deps) {
        if (diagram == null) throw new IllegalArgumentException("diagram must not be null");

        List<Violation> violations = new ArrayList<>();

        if (diagram.level() == C4Diagram.C4Level.CODE) {
            validateCodeLevel(classes, deps, violations);
        } else {
            validateNonCodeLevel(diagram, violations);
        }

        return violations.isEmpty() ? IntegrityResult.ok() : new IntegrityResult(false, violations);
    }

    private void validateCodeLevel(
            List<CodeEntry> classes, List<Dependency> deps, List<Violation> violations) {
        ValidationResult result = codeValidator.validate(classes, deps);
        if (!result.valid()) {
            for (String v : result.violations()) {
                Severity severity = v.contains("outward") ? Severity.ERROR : Severity.WARN;
                ViolationType type =
                        v.contains("outward")
                                ? ViolationType.OUTWARD_DEPENDENCY
                                : ViolationType.LAYER_CROSSING;
                violations.add(new Violation(v, type, severity));
            }
        }
    }

    private void validateNonCodeLevel(C4Diagram diagram, List<Violation> violations) {
        if (diagram.content() == null || diagram.content().isBlank()) {
            violations.add(
                    new Violation(
                            "C4 diagram content must not be blank for level " + diagram.level(),
                            ViolationType.MISSING_CONTENT,
                            Severity.ERROR));
        }
    }
}

package dev.iadev.domain.architecture;

import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4CodeLevelValidator.ValidationResult;
import dev.iadev.domain.architecture.C4IntegrityValidator.Severity;
import dev.iadev.domain.architecture.C4IntegrityValidator.Violation;
import dev.iadev.domain.architecture.C4IntegrityValidator.ViolationType;
import java.util.List;
import java.util.stream.Collectors;

public final class HexagonalArchitectureValidator {

    private final C4CodeLevelValidator codeValidator = new C4CodeLevelValidator();

    public record ArchResult(boolean valid, List<Violation> violations) {
        public static ArchResult ok() {
            return new ArchResult(true, List.of());
        }
    }

    public ArchResult validate(List<CodeEntry> classes, List<Dependency> dependencies) {
        ValidationResult result = codeValidator.validate(classes, dependencies);
        if (result.valid()) {
            return ArchResult.ok();
        }

        List<Violation> violations =
                result.violations().stream()
                        .map(
                                message ->
                                        new Violation(
                                                message,
                                                message.contains("outward")
                                                        ? ViolationType.OUTWARD_DEPENDENCY
                                                        : ViolationType.LAYER_CROSSING,
                                                Severity.ERROR))
                        .collect(Collectors.toList());

        return new ArchResult(false, violations);
    }
}

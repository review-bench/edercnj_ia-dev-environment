package dev.iadev.domain.quality;

import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4IntegrityValidator;
import dev.iadev.domain.architecture.C4IntegrityValidator.IntegrityResult;
import dev.iadev.domain.architecture.C4IntegrityValidator.Violation;

import java.util.ArrayList;
import java.util.List;

public final class PhaseGateC4Validator {

    private final C4IntegrityValidator integrityValidator = new C4IntegrityValidator();

    public record PhaseGateResult(boolean passed, List<Violation> violations) {
        public static PhaseGateResult ok() {
            return new PhaseGateResult(true, List.of());
        }
    }

    public PhaseGateResult validate(List<C4Diagram> diagrams, List<CodeEntry> classes,
            List<Dependency> deps) {
        if (diagrams == null || diagrams.isEmpty()) {
            throw new IllegalArgumentException("diagrams must not be null or empty");
        }
        List<Violation> all = new ArrayList<>();
        for (C4Diagram diagram : diagrams) {
            IntegrityResult result = integrityValidator.validate(diagram, classes, deps);
            if (!result.valid()) {
                all.addAll(result.violations());
            }
        }
        return all.isEmpty() ? PhaseGateResult.ok() : new PhaseGateResult(false, all);
    }
}

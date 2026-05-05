package dev.iadev.application.quality;

import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.quality.PhaseGateC4Validator;
import dev.iadev.domain.quality.PhaseGateC4Validator.PhaseGateResult;
import java.util.List;

public final class ExecuteC4PhaseGateUseCase {

    private final PhaseGateC4Validator validator = new PhaseGateC4Validator();

    public PhaseGateResult execute(
            List<C4Diagram> diagrams, List<CodeEntry> classes, List<Dependency> deps) {
        if (diagrams == null) throw new IllegalArgumentException("diagrams must not be null");
        return validator.validate(diagrams, classes, deps);
    }
}

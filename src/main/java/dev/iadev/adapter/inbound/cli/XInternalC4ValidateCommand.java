package dev.iadev.adapter.inbound.cli;

import dev.iadev.application.architecture.ValidateC4IntegrityUseCase;
import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4IntegrityValidator.IntegrityResult;
import dev.iadev.domain.architecture.C4IntegrityValidator.Violation;
import java.util.List;

public final class XInternalC4ValidateCommand {

    private final ValidateC4IntegrityUseCase useCase = new ValidateC4IntegrityUseCase();

    public IntegrityResult execute(
            C4Diagram diagram, List<CodeEntry> classes, List<Dependency> deps) {
        if (diagram == null) throw new IllegalArgumentException("diagram must not be null");
        return useCase.execute(diagram, classes, deps);
    }

    public String format(IntegrityResult result) {
        if (result.valid()) return "OK: diagram is valid";
        StringBuilder sb = new StringBuilder("VIOLATIONS:\n");
        for (Violation v : result.violations()) {
            sb.append("  [")
                    .append(v.severity())
                    .append("] ")
                    .append(v.type())
                    .append(": ")
                    .append(v.message())
                    .append("\n");
        }
        return sb.toString();
    }
}

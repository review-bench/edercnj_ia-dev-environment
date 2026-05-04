package dev.iadev.application.architecture;

import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4IntegrityValidator;
import dev.iadev.domain.architecture.C4IntegrityValidator.IntegrityResult;

import java.util.List;

public final class ValidateC4IntegrityUseCase {

    private final C4IntegrityValidator validator = new C4IntegrityValidator();

    public IntegrityResult execute(C4Diagram diagram, List<CodeEntry> classes, List<Dependency> deps) {
        if (diagram == null) throw new IllegalArgumentException("diagram must not be null");
        return validator.validate(diagram, classes, deps);
    }
}

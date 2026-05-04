package dev.iadev.domain.architecture;

import dev.iadev.domain.architecture.C4CodeLevelValidator.ClassType;
import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4CodeLevelValidator.LayerType;
import dev.iadev.domain.architecture.C4IntegrityValidator.Severity;
import dev.iadev.domain.architecture.C4IntegrityValidator.ViolationType;
import dev.iadev.domain.architecture.HexagonalArchitectureValidator.ArchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HexagonalArchitectureValidatorTest {

    private final HexagonalArchitectureValidator validator = new HexagonalArchitectureValidator();

    @Test
    void validate_validHexagonal_returnsOk() {
        List<CodeEntry> classes = List.of(
                new CodeEntry("Order", "domain", LayerType.DOMAIN, ClassType.ENTITY));
        ArchResult result = validator.validate(classes, List.of());
        assertTrue(result.valid());
        assertTrue(result.violations().isEmpty());
    }

    @Test
    void validate_outwardDependency_returnsError() {
        List<CodeEntry> classes = List.of(
                new CodeEntry("Domain", "domain", LayerType.DOMAIN, ClassType.ENTITY),
                new CodeEntry("Adapter", "adapter", LayerType.ADAPTER_OUTBOUND, ClassType.RENDERER));
        List<Dependency> deps = List.of(new Dependency("Domain", "Adapter"));
        ArchResult result = validator.validate(classes, deps);
        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.type() == ViolationType.OUTWARD_DEPENDENCY));
        assertTrue(result.violations().stream().anyMatch(v -> v.severity() == Severity.ERROR));
    }

    @Test
    void validate_applicationToAdapterDep_returnsError() {
        List<CodeEntry> classes = List.of(
                new CodeEntry("Domain", "domain", LayerType.DOMAIN, ClassType.ENTITY),
                new CodeEntry("UseCase", "application", LayerType.APPLICATION, ClassType.USE_CASE),
                new CodeEntry("Repo", "outbound", LayerType.ADAPTER_OUTBOUND, ClassType.REPOSITORY));
        List<Dependency> deps = List.of(new Dependency("UseCase", "Repo"));
        ArchResult result = validator.validate(classes, deps);
        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.severity() == Severity.ERROR));
    }

    @Test
    void validate_noDomainClass_returnsError() {
        List<CodeEntry> classes = List.of(
                new CodeEntry("UseCase", "application", LayerType.APPLICATION, ClassType.USE_CASE));
        ArchResult result = validator.validate(classes, List.of());
        assertFalse(result.valid());
    }
}

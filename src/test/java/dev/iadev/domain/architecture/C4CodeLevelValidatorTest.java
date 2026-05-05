package dev.iadev.domain.architecture;

import static org.junit.jupiter.api.Assertions.*;

import dev.iadev.domain.architecture.C4CodeLevelValidator.ClassType;
import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4CodeLevelValidator.LayerType;
import dev.iadev.domain.architecture.C4CodeLevelValidator.ValidationResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class C4CodeLevelValidatorTest {

    private final C4CodeLevelValidator validator = new C4CodeLevelValidator();

    @Test
    void validate_withEmptyClasses_returnsViolation() {
        ValidationResult result = validator.validate(List.of(), null);
        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("empty")));
    }

    @Test
    void validate_withNullClasses_returnsViolation() {
        ValidationResult result = validator.validate(null, null);
        assertFalse(result.valid());
        assertFalse(result.violations().isEmpty());
    }

    @Test
    void validate_withNoDomainClass_returnsViolation() {
        List<CodeEntry> classes =
                List.of(
                        new CodeEntry(
                                "MyCommand",
                                "adapter.inbound.cli",
                                LayerType.ADAPTER_INBOUND,
                                ClassType.COMMAND));
        ValidationResult result = validator.validate(classes, null);
        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("DOMAIN")));
    }

    @Test
    void validate_withValidHexagonalClasses_returnsOk() {
        List<CodeEntry> classes =
                List.of(
                        new CodeEntry(
                                "MyEntity", "domain.model", LayerType.DOMAIN, ClassType.ENTITY),
                        new CodeEntry(
                                "MyUseCase",
                                "application",
                                LayerType.APPLICATION,
                                ClassType.USE_CASE),
                        new CodeEntry(
                                "MyRenderer",
                                "adapter.outbound",
                                LayerType.ADAPTER_OUTBOUND,
                                ClassType.RENDERER));
        List<Dependency> deps =
                List.of(
                        new Dependency("MyUseCase", "MyEntity"),
                        new Dependency("MyRenderer", "MyEntity"));
        ValidationResult result = validator.validate(classes, deps);
        assertTrue(result.valid());
        assertTrue(result.violations().isEmpty());
    }

    @Test
    void validate_withOutwardDependencyDomainToAdapter_returnsViolation() {
        List<CodeEntry> classes =
                List.of(
                        new CodeEntry(
                                "MyEntity", "domain.model", LayerType.DOMAIN, ClassType.ENTITY),
                        new CodeEntry(
                                "MyRenderer",
                                "adapter.outbound",
                                LayerType.ADAPTER_OUTBOUND,
                                ClassType.RENDERER));
        List<Dependency> deps = List.of(new Dependency("MyEntity", "MyRenderer"));
        ValidationResult result = validator.validate(classes, deps);
        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("outward dependency")));
    }

    @Test
    void validate_withApplicationDependingOnAdapter_returnsViolation() {
        List<CodeEntry> classes =
                List.of(
                        new CodeEntry(
                                "MyEntity", "domain.model", LayerType.DOMAIN, ClassType.ENTITY),
                        new CodeEntry(
                                "MyUseCase",
                                "application",
                                LayerType.APPLICATION,
                                ClassType.USE_CASE),
                        new CodeEntry(
                                "MyCommand",
                                "adapter.inbound",
                                LayerType.ADAPTER_INBOUND,
                                ClassType.COMMAND));
        List<Dependency> deps = List.of(new Dependency("MyUseCase", "MyCommand"));
        ValidationResult result = validator.validate(classes, deps);
        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("outward")));
    }

    @Test
    void codeEntry_withBlankClassName_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CodeEntry("", "domain.model", LayerType.DOMAIN, ClassType.ENTITY));
    }
}

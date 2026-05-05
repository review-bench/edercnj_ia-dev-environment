package dev.iadev.domain.architecture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class C4CodeLevelValidator {

    public enum LayerType {
        DOMAIN,
        APPLICATION,
        ADAPTER_INBOUND,
        ADAPTER_OUTBOUND
    }

    public enum ClassType {
        ENTITY,
        VALUE_OBJECT,
        SERVICE,
        PORT,
        USE_CASE,
        COMMAND,
        RENDERER,
        REPOSITORY
    }

    public record CodeEntry(
            String className, String packageName, LayerType layer, ClassType classType) {
        public CodeEntry {
            if (className == null || className.isBlank())
                throw new IllegalArgumentException("className must not be blank");
            if (packageName == null || packageName.isBlank())
                throw new IllegalArgumentException("packageName must not be blank");
            if (layer == null) throw new IllegalArgumentException("layer must not be null");
            if (classType == null) throw new IllegalArgumentException("classType must not be null");
        }
    }

    public record Dependency(String fromClass, String toClass) {
        public Dependency {
            if (fromClass == null || fromClass.isBlank())
                throw new IllegalArgumentException("fromClass must not be blank");
            if (toClass == null || toClass.isBlank())
                throw new IllegalArgumentException("toClass must not be blank");
        }
    }

    public record ValidationResult(boolean valid, List<String> violations) {
        public static ValidationResult ok() {
            return new ValidationResult(true, List.of());
        }
    }

    public ValidationResult validate(List<CodeEntry> classes, List<Dependency> dependencies) {
        List<String> violations = new ArrayList<>();

        if (classes == null || classes.isEmpty()) {
            violations.add("classes must not be empty");
            return new ValidationResult(false, violations);
        }

        boolean hasDomain = classes.stream().anyMatch(c -> c.layer() == LayerType.DOMAIN);
        if (!hasDomain) {
            violations.add("at least one DOMAIN class is required");
        }

        if (dependencies != null && !dependencies.isEmpty()) {
            Map<String, LayerType> layerMap =
                    classes.stream()
                            .collect(
                                    Collectors.toMap(
                                            CodeEntry::className, CodeEntry::layer, (a, b) -> a));

            for (Dependency dep : dependencies) {
                LayerType fromLayer = layerMap.get(dep.fromClass());
                LayerType toLayer = layerMap.get(dep.toClass());
                if (fromLayer == null || toLayer == null) continue;
                if (isOutwardDependency(fromLayer, toLayer)) {
                    violations.add(
                            "outward dependency: "
                                    + dep.fromClass()
                                    + " ("
                                    + fromLayer
                                    + ") -> "
                                    + dep.toClass()
                                    + " ("
                                    + toLayer
                                    + ") violates hexagonal architecture");
                }
            }
        }

        return violations.isEmpty()
                ? ValidationResult.ok()
                : new ValidationResult(false, violations);
    }

    private boolean isOutwardDependency(LayerType from, LayerType to) {
        if (from == LayerType.DOMAIN && to != LayerType.DOMAIN) return true;
        if (from == LayerType.APPLICATION
                && (to == LayerType.ADAPTER_INBOUND || to == LayerType.ADAPTER_OUTBOUND))
            return true;
        return false;
    }
}

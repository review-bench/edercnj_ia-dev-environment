package dev.iadev.domain.model;

import java.util.Map;
import java.util.Set;

/**
 * Policy and meta configuration of a {@link ProjectConfig}.
 *
 * <p>Bundles the five policy / meta fields — compliance type, target platforms, branching model,
 * telemetry flag, and documentation governance — that govern how the generated output is assembled
 * but do not describe the technology stack itself.
 *
 * <p>Extracted by EPIC-0044 (audit finding M-003) to keep the root {@link ProjectConfig} aggregate
 * within the 4-parameter guideline of Rule 03. The fifth field ({@code documentation}) was added by
 * EPIC-0071 (Documentation as DoD, story-0071-0001).
 *
 * @param compliance the compliance type (optional, default "none"); accepted values are documented
 *     on {@link ProjectConfig}
 * @param platforms the target platforms from YAML (optional, empty = all, immutable)
 * @param branchingModel the branching strategy (optional, default {@link BranchingModel#GITFLOW})
 * @param telemetryEnabled whether telemetry hooks are injected (optional, default {@code true});
 *     maps to YAML {@code telemetry.enabled} (story-0040-0004)
 * @param documentation the documentation governance config (optional, default auto-detect); maps to
 *     YAML {@code documentation} block (EPIC-0071, story-0071-0001)
 * @param quality the quality-gate configuration (optional, default all-disabled); maps to YAML
 *     {@code quality} block (EPIC-0072, story-0072-0001)
 * @param dependencyPolicy the dependency policy gate configuration (optional, default disabled);
 *     maps to YAML {@code dependencies.policy} block (EPIC-0074, story-0074-0001)
 */
public record Governance(
        String compliance,
        Set<Platform> platforms,
        BranchingModel branchingModel,
        boolean telemetryEnabled,
        DocumentationConfig documentation,
        QualityConfig quality,
        DependencyPolicyConfig dependencyPolicy) {

    /**
     * Compact constructor enforcing immutability of the {@code platforms} set and applying defaults
     * for the {@code branchingModel}, {@code documentation}, {@code quality}, and {@code
     * dependencyPolicy}.
     */
    public Governance {
        platforms = platforms == null ? Set.of() : Set.copyOf(platforms);
        branchingModel = branchingModel == null ? BranchingModel.GITFLOW : branchingModel;
        documentation = documentation == null ? DocumentationConfig.DEFAULT : documentation;
        quality = quality == null ? QualityConfig.DEFAULT : quality;
        dependencyPolicy =
                dependencyPolicy == null ? DependencyPolicyConfig.DEFAULT : dependencyPolicy;
    }

    /**
     * Creates a Governance record from a YAML-parsed root map.
     *
     * <p>Delegates compliance, platforms, branching-model, telemetry, and documentation parsing to
     * the helpers owned by {@link ProjectConfig} so the validation rules remain in a single place.
     *
     * @param root the root map from YAML deserialization
     * @return a new Governance instance populated with defaults for any missing field
     * @throws ConfigValidationException if the compliance or branching-model value is invalid
     */
    public static Governance fromMap(Map<String, Object> root) {
        return new Governance(
                ProjectConfig.parseCompliance(root),
                ProjectConfig.parsePlatforms(root),
                ProjectConfig.parseBranchingModel(root),
                ProjectConfig.parseTelemetryEnabled(root),
                ProjectConfig.parseDocumentation(root),
                ProjectConfig.parseQuality(root),
                ProjectConfig.parseDependencyPolicy(root));
    }
}

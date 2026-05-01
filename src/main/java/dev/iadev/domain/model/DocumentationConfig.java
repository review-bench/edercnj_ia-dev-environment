package dev.iadev.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Documentation governance configuration for a {@link ProjectConfig}.
 *
 * <p>Captures which documentation targets are maintained in the project and the grace period
 * before a missing doc update is treated as a blocker. Introduced by EPIC-0071 (Documentation
 * as DoD) to support the {@code /x-doc-validate} gate.
 *
 * <p>When {@code targets} is empty the project uses auto-detection (see {@link #autoDetect()}):
 * the {@code x-doc-validate} skill derives the effective target list from the project's interface
 * declarations (REST → OpenAPI, broker → AsyncAPI, etc.) plus always-included targets (README,
 * ADR). Filesystem-dependent targets (skill-docs, system-architecture) are resolved by the skill
 * at runtime.
 *
 * <p>YAML block parsed from the project configuration file:
 *
 * <pre>{@code
 * documentation:
 *   targets:
 *     - readme
 *     - openapi
 *     - adr
 *   freshness-window-hours: 0
 * }</pre>
 *
 * @param targets the explicit list of documentation targets to maintain; empty = auto-detect
 * @param freshnessWindowHours grace period in hours before a missing doc update blocks the PR;
 *     0 = immediate (default)
 */
public record DocumentationConfig(List<String> targets, int freshnessWindowHours) {

    /** Singleton representing the default documentation config (auto-detect, immediate gate). */
    public static final DocumentationConfig DEFAULT = new DocumentationConfig(List.of(), 0);

    /** Compact constructor enforcing immutability of the targets list. */
    public DocumentationConfig {
        targets = targets == null ? List.of() : List.copyOf(targets);
    }

    /**
     * Returns {@code true} when no explicit targets are configured and the {@code x-doc-validate}
     * skill should derive effective targets from the project's interface declarations.
     */
    public boolean autoDetect() {
        return targets.isEmpty();
    }

    /**
     * Creates a {@link DocumentationConfig} from the {@code documentation} sub-map.
     *
     * <p>Both fields are optional: absent {@code targets} or empty list yields auto-detect mode;
     * absent {@code freshness-window-hours} defaults to {@code 0}.
     *
     * @param docMap the {@code documentation} sub-map from the YAML root (may be empty)
     * @return a new DocumentationConfig instance, never null
     */
    public static DocumentationConfig fromMap(Map<String, Object> docMap) {
        List<String> targets = MapHelper.optionalStringList(docMap, "targets");
        int freshnessWindowHours = MapHelper.optionalInt(docMap, "freshness-window-hours", 0);
        return new DocumentationConfig(targets, freshnessWindowHours);
    }
}

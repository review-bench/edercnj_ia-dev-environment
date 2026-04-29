package dev.iadev.application.composition;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Immutable composition plan produced by {@link CapabilityAwareComposer#plan}.
 *
 * <p>Separates the decision (which artifacts to include) from execution (writing to disk).
 * This separation enables dry-run and testing without side effects (RULE-004 determinism).
 */
public record CompositionPlan(
        List<ArtifactEntry> included,
        List<ArtifactEntry> excluded,
        List<String> warnings) {

    public record ArtifactEntry(Path sourcePath, String relativePath, String excludeReason) {

        public ArtifactEntry(Path sourcePath, String relativePath) {
            this(sourcePath, relativePath, null);
        }

        public boolean isExcluded() {
            return excludeReason != null;
        }
    }

    public CompositionPlan {
        Objects.requireNonNull(included);
        Objects.requireNonNull(excluded);
        included = List.copyOf(included);
        excluded = List.copyOf(excluded);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static CompositionPlan empty() {
        return new CompositionPlan(List.of(), List.of(), List.of());
    }

    public int totalArtifacts() {
        return included.size() + excluded.size();
    }

    public String toSummary() {
        return String.format("CompositionPlan{included=%d, excluded=%d, warnings=%d}",
                included.size(), excluded.size(), warnings.size());
    }
}

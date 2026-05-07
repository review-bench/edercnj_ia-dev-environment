package dev.iadev.application.composition;

import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Core capability-aware composition engine — replaces blind copy of targets/ with
 * capability-filtered output.
 *
 * <p>API is split into two deterministic operations:
 *
 * <ul>
 *   <li>{@link #plan}: pure decision-making, no I/O (testable, RULE-004 deterministic)
 *   <li>{@link #execute}: writes the plan to disk (side-effects isolated here)
 * </ul>
 *
 * <p>Pruning modes:
 *
 * <ul>
 *   <li>{@link PruningMode#ADVISORY} — artifacts with unmatched capabilities are kept in {@code
 *       included} but a {@code WARN [rule-pruning-advisory]} warning is emitted.
 *   <li>{@link PruningMode#HARD} — artifacts with unmatched capabilities are moved to {@code
 *       excluded} (default pre-advisory-mode behavior).
 * </ul>
 */
public final class CapabilityAwareComposer {

    /** Controls whether capability mismatches result in exclusion or advisory warnings. */
    public enum PruningMode {
        /** Include all artifacts; emit WARN for capability mismatches. */
        ADVISORY,
        /** Exclude artifacts whose required capabilities are not in the active set. */
        HARD
    }

    private final ArtifactScanner scanner;
    private final CapabilityMatcher matcher;
    private final PruningMode mode;

    /** Creates a composer in {@link PruningMode#HARD} mode (default since story-0078-0016). */
    public CapabilityAwareComposer() {
        this(PruningMode.HARD);
    }

    /** Creates a composer with an explicit pruning mode. */
    public CapabilityAwareComposer(PruningMode mode) {
        this.scanner = new ArtifactScanner();
        this.matcher = new CapabilityMatcher();
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
    }

    public CompositionPlan plan(ResolvedCapabilitySet activeSet, Path targetsRoot) {
        Objects.requireNonNull(activeSet, "activeSet must not be null");
        Objects.requireNonNull(targetsRoot, "targetsRoot must not be null");

        if (!Files.isDirectory(targetsRoot)) {
            return CompositionPlan.empty();
        }

        List<ArtifactScanner.ScannedArtifact> artifacts;
        try {
            artifacts = scanner.scan(targetsRoot);
        } catch (IOException e) {
            return new CompositionPlan(
                    List.of(),
                    List.of(),
                    List.of("error scanning targets root: " + e.getMessage()));
        }

        List<CompositionPlan.ArtifactEntry> included = new ArrayList<>();
        List<CompositionPlan.ArtifactEntry> excluded = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (ArtifactScanner.ScannedArtifact artifact : artifacts) {
            CapabilityMatcher.MatchResult result =
                    matcher.matches(artifact.requiredCapabilities(), activeSet);
            if (result.included()) {
                included.add(
                        new CompositionPlan.ArtifactEntry(
                                artifact.path(), artifact.relativePath()));
            } else if (mode == PruningMode.ADVISORY) {
                included.add(
                        new CompositionPlan.ArtifactEntry(
                                artifact.path(), artifact.relativePath()));
                warnings.add(
                        "WARN [rule-pruning-advisory] would prune '"
                                + artifact.relativePath()
                                + "' ("
                                + result.excludeReason()
                                + ")");
            } else {
                excluded.add(
                        new CompositionPlan.ArtifactEntry(
                                artifact.path(), artifact.relativePath(), result.excludeReason()));
            }
        }
        return new CompositionPlan(included, excluded, warnings);
    }

    public void execute(CompositionPlan plan, Path outputRoot) throws IOException {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(outputRoot, "outputRoot must not be null");

        for (CompositionPlan.ArtifactEntry entry : plan.included()) {
            Path target = outputRoot.resolve(entry.relativePath());
            Files.createDirectories(target.getParent());
            Files.copy(
                    entry.sourcePath(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public PruningMode mode() {
        return mode;
    }
}

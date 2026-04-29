package dev.iadev.application.composition;

import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Core capability-aware composition engine — replaces blind copy of targets/ with capability-filtered output.
 *
 * <p>API is split into two deterministic operations:
 * <ul>
 *   <li>{@link #plan}: pure decision-making, no I/O (testable, RULE-004 deterministic)
 *   <li>{@link #execute}: writes the plan to disk (side-effects isolated here)
 * </ul>
 */
public final class CapabilityAwareComposer {

    private final ArtifactScanner scanner;
    private final CapabilityMatcher matcher;

    public CapabilityAwareComposer() {
        this.scanner = new ArtifactScanner();
        this.matcher = new CapabilityMatcher();
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
            return new CompositionPlan(List.of(), List.of(),
                    List.of("error scanning targets root: " + e.getMessage()));
        }

        List<CompositionPlan.ArtifactEntry> included = new ArrayList<>();
        List<CompositionPlan.ArtifactEntry> excluded = new ArrayList<>();

        for (ArtifactScanner.ScannedArtifact artifact : artifacts) {
            CapabilityMatcher.MatchResult result =
                    matcher.matches(artifact.requiredCapabilities(), activeSet);
            if (result.included()) {
                included.add(new CompositionPlan.ArtifactEntry(
                        artifact.path(), artifact.relativePath()));
            } else {
                excluded.add(new CompositionPlan.ArtifactEntry(
                        artifact.path(), artifact.relativePath(), result.excludeReason()));
            }
        }
        return new CompositionPlan(included, excluded, List.of());
    }

    public void execute(CompositionPlan plan, Path outputRoot) throws IOException {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(outputRoot, "outputRoot must not be null");

        for (CompositionPlan.ArtifactEntry entry : plan.included()) {
            Path target = outputRoot.resolve(entry.relativePath());
            Files.createDirectories(target.getParent());
            Files.copy(entry.sourcePath(), target,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

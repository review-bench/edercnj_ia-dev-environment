package dev.iadev.application.composition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Applies a {@link CompositionPlan} to an output directory — writes included artifacts, omits excluded.
 *
 * <p>Replaces the blind copy of {@code targets/claude/} → {@code .claude/} (RULE-008: single writer).
 * Idempotent: re-running with the same plan produces the same output.
 */
public final class OutputPruner {

    public record PruneResult(int written, int skipped) {}

    public PruneResult execute(CompositionPlan plan, Path outputRoot) throws IOException {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(outputRoot, "outputRoot must not be null");

        if (plan.included().isEmpty()) return new PruneResult(0, plan.excluded().size());

        Files.createDirectories(outputRoot);
        int written = 0;

        for (CompositionPlan.ArtifactEntry entry : plan.included()) {
            Path target = outputRoot.resolve(entry.relativePath());
            Files.createDirectories(target.getParent());
            Files.copy(entry.sourcePath(), target, StandardCopyOption.REPLACE_EXISTING);
            written++;
        }

        return new PruneResult(written, plan.excluded().size());
    }
}

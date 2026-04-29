package dev.iadev.application.assembler;

import dev.iadev.application.capability.CapabilityResolver;
import dev.iadev.application.composition.CapabilityAwareComposer;
import dev.iadev.application.composition.CompositionPlan;
import dev.iadev.application.composition.OutputPruner;
import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.Profile;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Capability-aware composition assembler — integrates {@link CapabilityAwareComposer} into the
 * assembler pipeline (story-0064-0304, RULE-001/RULE-008).
 *
 * <p>Runs after all legacy assemblers. In Phase 7 (story-0064-0701), legacy assemblers will be
 * removed and this assembler will be the sole writer.
 */
public final class CapabilityCompositionAssembler implements Assembler {

    private static final Logger LOG = Logger.getLogger(CapabilityCompositionAssembler.class.getName());

    private final CapabilityAwareComposer composer;
    private final OutputPruner pruner;

    public CapabilityCompositionAssembler() {
        this.composer = new CapabilityAwareComposer();
        this.pruner = new OutputPruner();
    }

    @Override
    public List<String> assemble(ProjectConfig config, TemplateEngine engine, Path outputDir) {
        Path targetsRoot = resolveTargetsRoot(config);
        if (targetsRoot == null || !Files.isDirectory(targetsRoot)) {
            LOG.fine("CapabilityCompositionAssembler: no capability targets root found — skip");
            return List.of();
        }

        ResolvedCapabilitySet active = buildActiveSet(config);
        CompositionPlan plan = composer.plan(active, targetsRoot);
        LOG.fine(() -> "CapabilityCompositionAssembler: " + plan.toSummary());

        if (plan.included().isEmpty()) return List.of();

        try {
            pruner.execute(plan, outputDir);
        } catch (IOException e) {
            LOG.warning("CapabilityCompositionAssembler: error executing plan: " + e.getMessage());
            return List.of();
        }

        return plan.included().stream()
                .map(entry -> outputDir.resolve(entry.relativePath()).toString())
                .toList();
    }

    private Path resolveTargetsRoot(ProjectConfig config) {
        return null;
    }

    private ResolvedCapabilitySet buildActiveSet(ProjectConfig config) {
        return new ResolvedCapabilitySet("default", List.of(), java.util.Map.of(), List.of());
    }
}

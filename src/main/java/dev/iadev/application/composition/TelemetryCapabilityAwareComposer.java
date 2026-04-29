package dev.iadev.application.composition;

import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Telemetry decorator for {@link CapabilityAwareComposer} — emits phase.start/phase.end with
 * composition metrics (story-0064-0507, RULE-004 baseline).
 *
 * <p>Metrics emitted on phase.end:
 *
 * <ul>
 *   <li>{@code tempoComposeMs} — elapsed time for plan() in milliseconds
 *   <li>{@code numArtefatosIncluidos} — count of artifacts included in plan
 *   <li>{@code numArtefatosPruneados} — count of artifacts excluded (pruned)
 *   <li>{@code numFragmentosResolvidos} — count of included artifacts (proxy for fragments)
 * </ul>
 *
 * <p>Fail-open: if telemetry emission fails, composition continues normally. Respects {@code
 * CLAUDE_TELEMETRY_DISABLED=1} (ADR-0005, Rule 13 §helper-contract).
 */
public final class TelemetryCapabilityAwareComposer {

    private static final Logger LOG =
            Logger.getLogger(TelemetryCapabilityAwareComposer.class.getName());
    private static final String ENV_DISABLED = "CLAUDE_TELEMETRY_DISABLED";

    private final CapabilityAwareComposer delegate;

    public TelemetryCapabilityAwareComposer() {
        this.delegate = new CapabilityAwareComposer();
    }

    public TelemetryCapabilityAwareComposer(CapabilityAwareComposer delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    public CompositionPlan plan(ResolvedCapabilitySet activeSet, Path targetsRoot) {
        boolean enabled = !"1".equals(System.getenv(ENV_DISABLED));
        long start = System.currentTimeMillis();

        if (enabled) emitPhaseStart();

        try {
            CompositionPlan result = delegate.plan(activeSet, targetsRoot);
            if (enabled) {
                emitPhaseEnd(
                        "ok",
                        System.currentTimeMillis() - start,
                        result.included().size(),
                        result.excluded().size(),
                        result.included().size());
            }
            return result;
        } catch (RuntimeException e) {
            if (enabled) emitPhaseEnd("failed", System.currentTimeMillis() - start, 0, 0, 0);
            throw e;
        }
    }

    public void execute(CompositionPlan plan, Path outputRoot) throws IOException {
        delegate.execute(plan, outputRoot);
    }

    private void emitPhaseStart() {
        try {
            LOG.fine(
                    "{\"type\":\"phase.start\",\"skill\":\"capability-aware-composer\","
                            + "\"phase\":\"compose\",\"timestamp\":\""
                            + Instant.now()
                            + "\"}");
        } catch (Exception e) {
            // fail-open
        }
    }

    private void emitPhaseEnd(
            String status, long elapsedMs, int included, int pruned, int fragments) {
        try {
            LOG.fine(
                    "{\"type\":\"phase.end\",\"skill\":\"capability-aware-composer\","
                            + "\"phase\":\"compose\","
                            + "\"status\":\""
                            + status
                            + "\","
                            + "\"tempoComposeMs\":"
                            + elapsedMs
                            + ","
                            + "\"numArtefatosIncluidos\":"
                            + included
                            + ","
                            + "\"numArtefatosPruneados\":"
                            + pruned
                            + ","
                            + "\"numFragmentosResolvidos\":"
                            + fragments
                            + "}");
        } catch (Exception e) {
            // fail-open
        }
    }
}

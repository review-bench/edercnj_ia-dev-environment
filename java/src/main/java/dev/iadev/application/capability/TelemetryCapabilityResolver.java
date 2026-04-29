package dev.iadev.application.capability;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.Profile;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Telemetry decorator for {@link CapabilityResolver} — emits phase.start/phase.end events.
 *
 * <p>Fail-open: if telemetry emission fails, resolution continues normally. Respects
 * {@code CLAUDE_TELEMETRY_DISABLED=1} env variable (ADR-0005, RULE-007 fail-open contract).
 */
public final class TelemetryCapabilityResolver {

    private static final Logger LOG = Logger.getLogger(TelemetryCapabilityResolver.class.getName());
    private static final String ENV_DISABLED = "CLAUDE_TELEMETRY_DISABLED";

    private final CapabilityResolver delegate;

    public TelemetryCapabilityResolver() {
        this.delegate = new CapabilityResolver();
    }

    public TelemetryCapabilityResolver(CapabilityResolver delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    public ResolvedCapabilitySet resolve(Profile profile, List<CapabilityDefinition> catalog) {
        return resolve(profile, catalog, Map.of());
    }

    public ResolvedCapabilitySet resolve(
            Profile profile,
            List<CapabilityDefinition> catalog,
            Map<String, String> parameterOverrides) {
        boolean telemetryEnabled = !"1".equals(System.getenv(ENV_DISABLED));
        long start = System.currentTimeMillis();

        if (telemetryEnabled) emitPhaseStart();

        try {
            ResolvedCapabilitySet result = delegate.resolve(profile, catalog, parameterOverrides);
            if (telemetryEnabled) {
                emitPhaseEnd("ok", System.currentTimeMillis() - start,
                        result.capabilities().size(), 1);
            }
            return result;
        } catch (RuntimeException e) {
            if (telemetryEnabled) {
                emitPhaseEnd("failed", System.currentTimeMillis() - start, 0, 0);
            }
            throw e;
        }
    }

    private void emitPhaseStart() {
        try {
            LOG.fine("{\"type\":\"phase.start\",\"skill\":\"x-internal-resolver\",\"phase\":\"resolve\",\"timestamp\":\""
                    + Instant.now() + "\"}");
        } catch (Exception e) {
            // fail-open
        }
    }

    private void emitPhaseEnd(String status, long elapsedMs, int numCapabilities, int numProfiles) {
        try {
            LOG.fine("{\"type\":\"phase.end\",\"skill\":\"x-internal-resolver\",\"phase\":\"resolve\","
                    + "\"status\":\"" + status + "\","
                    + "\"tempoResolveMs\":" + elapsedMs + ","
                    + "\"numCapabilitiesAtivas\":" + numCapabilities + ","
                    + "\"numProfilesExpandidos\":" + numProfiles + "}");
        } catch (Exception e) {
            // fail-open
        }
    }
}

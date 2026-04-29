package dev.iadev.application.composition;

import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.util.List;
import java.util.Objects;

/**
 * Decides whether a scanned artifact should be included given the active capability set.
 *
 * <p>Rules:
 * <ul>
 *   <li>Empty requires-capabilities ({@code []}) → always include (universal artifact)
 *   <li>Non-empty list → include if ANY required capability matches an active capability (glob-aware)
 *   <li>No match → exclude with reason
 * </ul>
 */
final class CapabilityMatcher {

    record MatchResult(boolean included, String excludeReason) {
        static MatchResult include() {
            return new MatchResult(true, null);
        }

        static MatchResult exclude(String reason) {
            return new MatchResult(false, reason);
        }
    }

    MatchResult matches(List<String> required, ResolvedCapabilitySet active) {
        Objects.requireNonNull(required);
        Objects.requireNonNull(active);

        if (required.isEmpty()) return MatchResult.include();

        for (String req : required) {
            if (isActiveCapability(req, active)) return MatchResult.include();
        }
        return MatchResult.exclude("no matching capability in active set for: " + required);
    }

    private boolean isActiveCapability(String required, ResolvedCapabilitySet active) {
        try {
            CapabilityId reqId = CapabilityId.of(required);
            if (reqId.isGlob()) {
                return active.capabilities().stream().anyMatch(reqId::matches);
            }
            return active.contains(reqId);
        } catch (Exception e) {
            return false;
        }
    }
}

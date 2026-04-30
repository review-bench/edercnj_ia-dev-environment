package dev.iadev.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Immutable record carrying the output of {@code /x-story-refine} or {@code /x-epic-refine}.
 *
 * <p>Persisted as {@code refinementVerdict} in {@code execution-state.json}. The {@code
 * verdictHash} field holds the SHA-256 of the {@code ## Refinement Verdict} block in the story or
 * epic markdown, enabling {@code audit-refinement-gate.sh} to detect manual divergence between the
 * state file and the markdown (Rule 29 §verdictHash contract).
 *
 * @param status one of {@code "approved"}, {@code "rejected"}, or {@code "tbd"}
 * @param scope {@code "story"} for story-level refinement; {@code "epic"} for epic-level
 * @param checkedAt ISO-8601 UTC timestamp of the last refinement run (nullable when status=tbd)
 * @param dimensions per-dimension results keyed by persona name; nullable when not yet run
 * @param blockers aggregated blocker descriptions across all personas; empty list when approved
 * @param verdictHash SHA-256 hex of the {@code ## Refinement Verdict} markdown block; null when
 *     not yet written
 */
public record RefinementVerdict(
        String status,
        String scope,
        String checkedAt,
        Map<String, DimensionResult> dimensions,
        List<String> blockers,
        String verdictHash) {

    /** Result produced by a single persona agent for one dimension. */
    public record DimensionResult(boolean checked, String blocker) {}

    /** Status constant: gate passed — all dimensions clear, no blockers. */
    public static final String STATUS_APPROVED = "approved";

    /** Status constant: gate rejected — at least one persona applied a NO-GO. */
    public static final String STATUS_REJECTED = "rejected";

    /** Status constant: not yet refined (pre-EPIC-0069 default or skill not run). */
    public static final String STATUS_TBD = "tbd";

    /** Returns {@code true} when all dimensions passed and no blockers remain. */
    public boolean isApproved() {
        return STATUS_APPROVED.equals(status);
    }

    /** Returns {@code true} when at least one persona applied a hard NO-GO. */
    public boolean isRejected() {
        return STATUS_REJECTED.equals(status);
    }

    /** Returns {@code true} when the verdict is pending (skill not yet run). */
    public boolean isTbd() {
        return status == null || STATUS_TBD.equals(status);
    }

    /**
     * Returns a default TBD instance representing an absent {@code refinementVerdict} field in a
     * pre-EPIC-0069 state file. Consumers use this sentinel to apply the Rule 19 fallback matrix.
     */
    public static RefinementVerdict absent() {
        return new RefinementVerdict(STATUS_TBD, null, null, null, List.of(), null);
    }
}

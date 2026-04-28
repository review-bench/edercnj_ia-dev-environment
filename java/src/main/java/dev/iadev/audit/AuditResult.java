package dev.iadev.audit;

import java.util.List;

/**
 * Result of an {@link Auditor} run, carrying an exit code and violation list.
 *
 * <p>Exit code semantics (matching bash audit contracts from Rule 26):
 *
 * <ul>
 *   <li>0 — {@code OK}: no violations detected
 *   <li>1 — named violation (e.g., {@code MODEL_SELECTION_VIOLATION}): at least one issue
 *   <li>2 — {@code OPERATIONAL_ERROR}: missing dependency or structural problem
 *   <li>3 — {@code BASELINE_CORRUPT}: baseline file malformed
 * </ul>
 *
 * @param exitCode the numeric exit code
 * @param exitName the named constant (e.g., {@code "OK"})
 * @param violations the list of individual violations (empty when exitCode=0)
 */
public record AuditResult(int exitCode, String exitName, List<AuditViolation> violations) {

    static final int OK = 0;
    static final int VIOLATION = 1;
    static final int OPERATIONAL_ERROR = 2;

    /** Convenience factory — no violations. */
    public static AuditResult ok() {
        return new AuditResult(OK, "OK", List.of());
    }

    /** Convenience factory — one or more violations. */
    public static AuditResult violation(String name, List<AuditViolation> violations) {
        return new AuditResult(VIOLATION, name, List.copyOf(violations));
    }

    /** Convenience factory — operational error (missing tool/file). */
    public static AuditResult operationalError(String message) {
        return new AuditResult(OPERATIONAL_ERROR, "OPERATIONAL_ERROR", List.of());
    }
}

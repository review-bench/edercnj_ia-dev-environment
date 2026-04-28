package dev.iadev.audit;

import java.nio.file.Path;

/**
 * Contract for a single governance audit, Java-equivalent of a bash {@code audit-*.sh} script.
 *
 * <p>Each implementation validates exactly one rule and is used by:
 *
 * <ul>
 *   <li>{@code *AuditorTest} — unit tests with known-good/bad fixtures
 *   <li>{@code AuditEquivalenceSmokeIT} — bash↔Java exit-code parity check (RULE-004)
 * </ul>
 *
 * <p>Implementations MUST be stateless; all state comes from the {@link AuditCorpus}.
 */
public interface Auditor {

    /**
     * Runs the audit over the given corpus.
     *
     * @param corpus the file set to audit
     * @return the audit result with exit code, named constant, and violation list
     */
    AuditResult audit(AuditCorpus corpus);

    /**
     * Returns the short name of this audit, matching the bash script name without prefix/suffix.
     * Example: {@code "model-selection"} for {@code audit-model-selection.sh}.
     */
    String name();

    /**
     * Returns the path to the bash template that is the canonical equivalent of this auditor.
     * Used by {@code AuditEquivalenceSmokeIT} to locate the bash counterpart.
     */
    Path bashEquivalentTemplate();
}

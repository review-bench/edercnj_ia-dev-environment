package dev.iadev.domain.model;

import java.util.List;

/**
 * Metadata describing a single audit script in the generated {@code .claude/scripts/} directory.
 *
 * <p>Used by {@code DocsAssembler.renderCatalog()} to populate the {@code
 * _TEMPLATE-AUDIT-GATES-CATALOG.md} template with per-stack accurate data.
 *
 * @param name the script filename (e.g., {@code audit-model-selection.sh})
 * @param category one of {@code Template}, {@code Runtime}, or {@code Stack-Specific}
 * @param validates human-readable description of what the audit checks
 * @param guarantees the outcome guarantee when the audit passes
 * @param ruleAnchor the rule that mandates this audit (e.g., {@code Rule 23})
 * @param exitCodes list of exit code entries for this audit
 */
public record AuditScript(
        String name,
        String category,
        String validates,
        String guarantees,
        String ruleAnchor,
        List<ExitCodeEntry> exitCodes) {

    /**
     * A single exit code entry for an audit script.
     *
     * @param code the numeric exit code (0, 1, 2, etc.)
     * @param constant the named constant (e.g., {@code MODEL_SELECTION_VIOLATION})
     * @param meaning human-readable description of the condition
     */
    public record ExitCodeEntry(int code, String constant, String meaning) {}
}

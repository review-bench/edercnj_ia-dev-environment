package dev.iadev.audit;

import java.nio.file.Path;

/**
 * A single violation found by an {@link Auditor}.
 *
 * @param file the file containing the violation
 * @param line the line number (1-based), or 0 if not applicable
 * @param rule the rule identifier (e.g., {@code "MISSING_MODEL"})
 * @param message human-readable description of the violation
 */
public record AuditViolation(Path file, int line, String rule, String message) {}

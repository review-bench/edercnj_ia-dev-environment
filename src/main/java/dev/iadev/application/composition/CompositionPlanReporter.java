package dev.iadev.application.composition;

import java.util.Objects;

/**
 * Formats a {@link CompositionPlan} for dry-run output.
 *
 * <p>Supports text (default) and JSON modes (story-0064-0305, RULE-001 observability).
 */
public final class CompositionPlanReporter {

    public enum Format {
        TEXT,
        JSON
    }

    public String report(CompositionPlan plan, Format format) {
        Objects.requireNonNull(plan, "plan must not be null");
        return switch (format) {
            case TEXT -> buildText(plan);
            case JSON -> buildJson(plan);
        };
    }

    private String buildText(CompositionPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Capability Composition Plan (dry-run) ===\n");
        sb.append("Included artifacts: ").append(plan.included().size()).append('\n');
        sb.append("Excluded artifacts: ").append(plan.excluded().size()).append('\n');
        if (!plan.warnings().isEmpty()) {
            sb.append("Warnings: ").append(plan.warnings().size()).append('\n');
        }
        if (!plan.included().isEmpty()) {
            sb.append("\nIncluded:\n");
            plan.included().forEach(e -> sb.append("  + ").append(e.relativePath()).append('\n'));
        }
        if (!plan.excluded().isEmpty()) {
            sb.append("\nExcluded:\n");
            plan.excluded()
                    .forEach(
                            e ->
                                    sb.append("  - ")
                                            .append(e.relativePath())
                                            .append(" [")
                                            .append(e.excludeReason())
                                            .append("]\n"));
        }
        return sb.toString();
    }

    private String buildJson(CompositionPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"included\": ").append(plan.included().size()).append(",\n");
        sb.append("  \"excluded\": ").append(plan.excluded().size()).append(",\n");
        sb.append("  \"warnings\": ").append(plan.warnings().size()).append(",\n");
        sb.append("  \"includedArtifacts\": [\n");
        plan.included()
                .forEach(
                        e ->
                                sb.append("    \"")
                                        .append(escapeJson(e.relativePath()))
                                        .append("\",\n"));
        if (!plan.included().isEmpty()) sb.setLength(sb.length() - 2);
        sb.append("\n  ],\n");
        sb.append("  \"excludedArtifacts\": [\n");
        plan.excluded()
                .forEach(
                        e ->
                                sb.append("    \"")
                                        .append(escapeJson(e.relativePath()))
                                        .append("\",\n"));
        if (!plan.excluded().isEmpty()) sb.setLength(sb.length() - 2);
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch <= 0x1F) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
    }
}

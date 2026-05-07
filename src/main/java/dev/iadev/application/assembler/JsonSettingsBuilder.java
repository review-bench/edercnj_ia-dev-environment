package dev.iadev.application.assembler;

import dev.iadev.util.JsonHelpers;
import java.util.List;

/**
 * Builds JSON content for {@code settings.json} and {@code settings.local.json}.
 *
 * <p>Constructs the permissions and hooks sections as formatted JSON strings. Uses {@link
 * JsonHelpers} for escaping and indentation.
 *
 * @see SettingsAssembler
 * @see JsonHelpers
 */
public final class JsonSettingsBuilder {

    /**
     * Default Claude Code skill-listing budget fraction (5% of context).
     *
     * <p>The Claude Code default is 1%, which truncates the skill catalog when a project ships more
     * than ~30 skills. ia-dev-env-generated projects routinely ship ≥ 100 skills, so we pin to 5%
     * (~10k tokens) to keep the full catalog visible to the LLM. Adjust here if the catalog grows
     * beyond what 5% can fit.
     */
    private static final double SKILL_LISTING_BUDGET_FRACTION = 0.05;

    JsonSettingsBuilder() {
        // package-private constructor
    }

    /**
     * Builds the settings.json content as a formatted JSON string with telemetry explicitly
     * disabled.
     *
     * <p><b>Legacy/no-telemetry helper.</b> This overload hard-codes {@code telemetryEnabled=false}
     * and is preserved only for pre-EPIC-0040 test scenarios that assert the legacy (no-telemetry)
     * output shape. Production call sites MUST use {@link #build(List, HookPresence, boolean)}
     * directly so the {@code telemetryEnabled} intent — which defaults to {@code true} at the
     * {@link dev.iadev.domain.model.ProjectConfig} level — is explicit at the call site.
     *
     * @param permissions the list of allowed commands
     * @param hookPresence whether to include hooks section
     * @return formatted JSON string (telemetry disabled)
     * @deprecated Use {@link #build(List, HookPresence, boolean)} and pass the resolved {@code
     *     telemetryEnabled} explicitly. This overload silently disables telemetry even when the
     *     project config opts into it.
     */
    @Deprecated
    String build(List<String> permissions, HookPresence hookPresence) {
        return build(permissions, hookPresence, false);
    }

    /**
     * Builds the settings.json content as a formatted JSON string with explicit telemetry control.
     *
     * @param permissions the list of allowed commands
     * @param hookPresence whether the legacy post-compile-check hook is present
     * @param telemetryEnabled whether to emit the 5 telemetry event entries (story-0040-0004)
     * @return formatted JSON string
     */
    String build(List<String> permissions, HookPresence hookPresence, boolean telemetryEnabled) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(JsonHelpers.indent(1)).append("\"permissions\": {\n");
        sb.append(JsonHelpers.indent(2)).append("\"allow\": [\n");
        appendPermissions(sb, permissions);
        sb.append(JsonHelpers.indent(2)).append("]\n");
        sb.append(JsonHelpers.indent(1)).append("},\n");
        boolean hasAnyHook = hookPresence.hasHooks() || telemetryEnabled;
        sb.append(JsonHelpers.indent(1))
                .append("\"skillListingBudgetFraction\": ")
                .append(SKILL_LISTING_BUDGET_FRACTION)
                .append(hasAnyHook ? ",\n" : "\n");
        if (hasAnyHook) {
            HookConfigBuilder.appendHooksSection(sb, hookPresence.hasHooks(), telemetryEnabled);
        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Builds the settings.local.json content.
     *
     * @return formatted JSON string with empty permissions
     */
    String buildLocal() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(JsonHelpers.indent(1)).append("\"permissions\": {\n");
        sb.append(JsonHelpers.indent(2)).append("\"allow\": []\n");
        sb.append(JsonHelpers.indent(1)).append("}\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static void appendPermissions(StringBuilder sb, List<String> permissions) {
        for (int i = 0; i < permissions.size(); i++) {
            sb.append(JsonHelpers.indent(3))
                    .append('"')
                    .append(JsonHelpers.escapeJson(permissions.get(i)))
                    .append('"');
            if (i < permissions.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
    }
}

package dev.iadev.application.assembler;

import dev.iadev.util.JsonHelpers;

/**
 * Builds the hooks configuration section for {@code settings.json}.
 *
 * <p>Generates hook entries for:
 *
 * <ul>
 *   <li>{@code PostToolUse} with {@code Write|Edit} matcher running {@code post-compile-check.sh}
 *       (compiled languages only)
 *   <li>{@code SessionStart}, {@code PreToolUse}, {@code PostToolUse} ({@code *} matcher), {@code
 *       SubagentStop}, {@code Stop} — telemetry scripts (story-0040-0004, when {@link
 *       ProjectConfig#telemetryEnabled()} is {@code true})
 * </ul>
 *
 * @see SettingsAssembler
 * @see JsonSettingsBuilder
 */
public final class HookConfigBuilder {

    private static final int HOOK_TIMEOUT = 60;
    private static final int TELEMETRY_TIMEOUT = 5;
    private static final String CLAUDE_PROJECT_DIR_PREFIX =
            "\"\\\"$CLAUDE_PROJECT_DIR\\\"/.claude/hooks/";

    HookConfigBuilder() {
        // package-private constructor
    }

    /**
     * Appends the hooks section to the given StringBuilder.
     *
     * <p>Section content depends on the presence flags: legacy {@code post-compile-check.sh} is
     * emitted when {@code hasLegacy} is {@code true}; five telemetry event entries are emitted when
     * {@code telemetryEnabled} is {@code true}. When both flags are {@code true} the {@code
     * PostToolUse} array contains both matchers ({@code Write|Edit} for post-compile and {@code *}
     * for telemetry).
     *
     * @param sb the StringBuilder to append to
     * @param hasLegacy whether to emit the legacy post-compile-check entry
     * @param telemetryEnabled whether to emit the five telemetry event entries (story-0040-0004)
     */
    static void appendHooksSection(StringBuilder sb, boolean hasLegacy, boolean telemetryEnabled) {
        sb.append(JsonHelpers.indent(1)).append("\"hooks\": {\n");
        if (telemetryEnabled) {
            appendTelemetryEvent(sb, "SessionStart", "telemetry-session.sh", false, false);
        }
        // PreToolUse with Rule 25 Layer 3 (enforce-phase-sequence.sh)
        // is emitted regardless of telemetry — see Rule 25 enforcement
        // matrix: Layer 3 is runtime enforcement, not observability.
        appendPreToolUseWithPhaseSequence(sb, telemetryEnabled);
        appendPostToolUseArray(sb, hasLegacy, telemetryEnabled);
        if (telemetryEnabled) {
            appendTelemetryEvent(sb, "SubagentStop", "telemetry-subagent.sh", false, false);
        }
        // Stop with Rule 25 Layer 2 (verify-phase-gates.sh) is emitted
        // regardless of telemetry — same rationale as PreToolUse.
        appendStopEventWithEie(sb, telemetryEnabled);
        sb.append(JsonHelpers.indent(1)).append("}\n");
    }

    /**
     * Appends the {@code PostToolUse} event array. The array may contain 0, 1, or 2 entries
     * depending on the flags.
     */
    private static void appendPostToolUseArray(
            StringBuilder sb, boolean hasLegacy, boolean telemetryEnabled) {
        if (!hasLegacy && !telemetryEnabled) {
            return;
        }
        boolean isLastOuterEntry = !telemetryEnabled;
        sb.append(JsonHelpers.indent(2)).append("\"PostToolUse\": [\n");
        if (hasLegacy) {
            appendPostCompileEntry(sb, telemetryEnabled);
        }
        if (telemetryEnabled) {
            appendTelemetryPostToolEntry(sb);
        }
        sb.append(JsonHelpers.indent(2)).append("]");
        sb.append(isLastOuterEntry ? "\n" : ",\n");
    }

    private static void appendPostCompileEntry(StringBuilder sb, boolean hasSibling) {
        sb.append(JsonHelpers.indent(3)).append("{\n");
        sb.append(JsonHelpers.indent(4)).append("\"matcher\": \"Write|Edit\",\n");
        sb.append(JsonHelpers.indent(4)).append("\"hooks\": [\n");
        sb.append(JsonHelpers.indent(5)).append("{\n");
        sb.append(JsonHelpers.indent(6)).append("\"type\": \"command\",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"command\": ")
                .append(CLAUDE_PROJECT_DIR_PREFIX)
                .append("post-compile-check.sh\",\n");
        sb.append(JsonHelpers.indent(6)).append("\"timeout\": ").append(HOOK_TIMEOUT).append(",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"statusMessage\": ")
                .append("\"Checking compilation...\"\n");
        sb.append(JsonHelpers.indent(5)).append("}\n");
        sb.append(JsonHelpers.indent(4)).append("]\n");
        sb.append(JsonHelpers.indent(3)).append("}").append(hasSibling ? ",\n" : "\n");
    }

    private static void appendTelemetryPostToolEntry(StringBuilder sb) {
        sb.append(JsonHelpers.indent(3)).append("{\n");
        sb.append(JsonHelpers.indent(4)).append("\"matcher\": \"*\",\n");
        sb.append(JsonHelpers.indent(4)).append("\"hooks\": [\n");
        sb.append(JsonHelpers.indent(5)).append("{\n");
        sb.append(JsonHelpers.indent(6)).append("\"type\": \"command\",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"command\": ")
                .append(CLAUDE_PROJECT_DIR_PREFIX)
                .append("telemetry-posttool.sh\",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"timeout\": ")
                .append(TELEMETRY_TIMEOUT)
                .append("\n");
        sb.append(JsonHelpers.indent(5)).append("}\n");
        sb.append(JsonHelpers.indent(4)).append("]\n");
        sb.append(JsonHelpers.indent(3)).append("}\n");
    }

    /**
     * Emits the {@code Stop} event hooks sequentially. When telemetry is enabled the sequence is:
     * {@code telemetry-stop.sh} → {@code verify-story-completion.sh} (Rule 24 Camada 2 EIE) →
     * {@code verify-phase-gates.sh} (Rule 25 Layer 2) → {@code enforce-continuous-flow.sh}
     * (EPIC-0068 Camada 0) → {@code stage-telemetry.sh}. When telemetry is disabled the sequence
     * is: {@code verify-phase-gates.sh} → {@code enforce-continuous-flow.sh}.
     *
     * <p>{@code verify-story-completion.sh} exits 2 when a story commit is detected but mandatory
     * evidence artifacts are missing, which Claude Code surfaces to the LLM as a blocking
     * notification. {@code enforce-continuous-flow.sh} exits 2 when a non-interactive orchestrator
     * stalls mid-phase, emitting a {@code CONTINUOUS_FLOW_INTERRUPT} nudge.
     */
    private static void appendStopEventWithEie(StringBuilder sb, boolean telemetryEnabled) {
        sb.append(JsonHelpers.indent(2)).append("\"Stop\": [\n");
        sb.append(JsonHelpers.indent(3)).append("{\n");
        sb.append(JsonHelpers.indent(4)).append("\"hooks\": [\n");
        if (telemetryEnabled) {
            appendStopHookEntry(sb, "telemetry-stop.sh", true);
            appendStopHookEntry(sb, "verify-story-completion.sh", true);
        }
        // Rule 25 Layer 2 — always emitted, independent of telemetry.
        appendStopHookEntry(sb, "verify-phase-gates.sh", true);
        // EPIC-0068 Camada 0 — always emitted; detects mid-phase stalls in
        // non-interactive orchestrators and emits CONTINUOUS_FLOW_INTERRUPT nudge.
        // Runs after verify-phase-gates.sh to avoid duplicate warnings.
        if (telemetryEnabled) {
            appendStopHookEntry(sb, "enforce-continuous-flow.sh", true);
            // EPIC-0063 story-0063-0003 — stage-telemetry.sh: auto git add NDJSON turn-by-turn.
            appendStopHookEntry(sb, "stage-telemetry.sh", false);
        } else {
            appendStopHookEntry(sb, "enforce-continuous-flow.sh", false);
        }
        sb.append(JsonHelpers.indent(4)).append("]\n");
        sb.append(JsonHelpers.indent(3)).append("}\n");
        sb.append(JsonHelpers.indent(2)).append("]\n");
    }

    /**
     * Appends the {@code PreToolUse} event with:
     *
     * <ul>
     *   <li>Rule 25 Layer 3 enforcement ({@code enforce-phase-sequence.sh}) — always emitted
     *   <li>Rule 59 enforcement ({@code enforce-no-bypass-flags.sh}) — always emitted; blocks
     *       {@code --skip-*} flags outside recovery mode (story-0059-0003)
     *   <li>Rule 69 refinement gate ({@code enforce-refinement-gate.sh}) — always emitted; blocks
     *       orchestrators when {@code refinementVerdict.status != "approved"} (EPIC-0069, Rule 29)
     *   <li>Optional telemetry pretool hook ({@code telemetry-pretool.sh}) — only when {@code
     *       telemetryEnabled} is {@code true}
     * </ul>
     *
     * <p>All entries run under the same wildcard matcher. Both enforcement scripts short-circuit on
     * {@code tool_name != "Skill"}.
     *
     * <p>Decoupling from telemetry: Rule 25, Rule 59, and Rule 69 define enforcement as runtime
     * (not observability), so disabling telemetry does NOT disable these hooks.
     */
    private static void appendPreToolUseWithPhaseSequence(
            StringBuilder sb, boolean telemetryEnabled) {
        sb.append(JsonHelpers.indent(2)).append("\"PreToolUse\": [\n");
        sb.append(JsonHelpers.indent(3)).append("{\n");
        sb.append(JsonHelpers.indent(4)).append("\"matcher\": \"*\",\n");
        sb.append(JsonHelpers.indent(4)).append("\"hooks\": [\n");
        if (telemetryEnabled) {
            appendStopHookEntry(sb, "telemetry-pretool.sh", true);
        }
        appendStopHookEntry(sb, "enforce-phase-sequence.sh", true);
        appendStopHookEntry(sb, "enforce-no-bypass-flags.sh", true);
        // Rule 69: always the last PreToolUse entry (no sibling after).
        appendStopHookEntry(sb, "enforce-refinement-gate.sh", false);
        sb.append(JsonHelpers.indent(4)).append("]\n");
        sb.append(JsonHelpers.indent(3)).append("}\n");
        sb.append(JsonHelpers.indent(2)).append("],\n");
    }

    private static void appendStopHookEntry(
            StringBuilder sb, String scriptName, boolean hasSibling) {
        sb.append(JsonHelpers.indent(5)).append("{\n");
        sb.append(JsonHelpers.indent(6)).append("\"type\": \"command\",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"command\": ")
                .append(CLAUDE_PROJECT_DIR_PREFIX)
                .append(scriptName)
                .append("\",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"timeout\": ")
                .append(TELEMETRY_TIMEOUT)
                .append("\n");
        sb.append(JsonHelpers.indent(5)).append("}").append(hasSibling ? ",\n" : "\n");
    }

    private static void appendTelemetryEvent(
            StringBuilder sb,
            String eventName,
            String scriptName,
            boolean withWildcardMatcher,
            boolean isLastEvent) {
        sb.append(JsonHelpers.indent(2)).append('"').append(eventName).append("\": [\n");
        sb.append(JsonHelpers.indent(3)).append("{\n");
        if (withWildcardMatcher) {
            sb.append(JsonHelpers.indent(4)).append("\"matcher\": \"*\",\n");
        }
        sb.append(JsonHelpers.indent(4)).append("\"hooks\": [\n");
        sb.append(JsonHelpers.indent(5)).append("{\n");
        sb.append(JsonHelpers.indent(6)).append("\"type\": \"command\",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"command\": ")
                .append(CLAUDE_PROJECT_DIR_PREFIX)
                .append(scriptName)
                .append("\",\n");
        sb.append(JsonHelpers.indent(6))
                .append("\"timeout\": ")
                .append(TELEMETRY_TIMEOUT)
                .append("\n");
        sb.append(JsonHelpers.indent(5)).append("}\n");
        sb.append(JsonHelpers.indent(4)).append("]\n");
        sb.append(JsonHelpers.indent(3)).append("}\n");
        sb.append(JsonHelpers.indent(2)).append("]").append(isLastEvent ? "\n" : ",\n");
    }
}

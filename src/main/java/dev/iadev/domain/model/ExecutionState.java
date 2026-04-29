package dev.iadev.domain.model;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Value object representing the parsed {@code execution-state.json} discriminator fields.
 *
 * <p>The {@code flowVersion} field marks the lifecycle variant of an epic:
 *
 * <ul>
 *   <li>{@code "1"} — legacy (pre-EPIC-0049): story PRs target {@code develop}
 *   <li>{@code "2"} — EPIC-0049: story PRs target {@code epic/XXXX}, manual gate
 *   <li>{@code "3"} — EPIC-0061 local-first: non-interactive default, Java audits in CI
 *   <li>{@code "4"} — EPIC-0060 v4 layout: same as "2" but {@code ai/epics/} path layout
 * </ul>
 *
 * <p>{@code localFirstLifecycle} is derived from {@code flowVersion} — true when version is "3" or
 * "4". It is serialized explicitly for grep/jq discoverability (EPIC-0061 story-0061-0007 decision
 * rationale).
 *
 * @param flowVersion the flow version discriminator
 * @param epicId the epic identifier
 * @param storyStatuses per-story status map (may be null when not loaded)
 * @param taskTracking task tracking configuration (may be null)
 * @param localFirstLifecycle true when flowVersion is "3" or "4"
 */
public record ExecutionState(
        String flowVersion,
        String epicId,
        Map<String, Object> storyStatuses,
        Map<String, Object> taskTracking,
        boolean localFirstLifecycle) {

    private static final Pattern FLOW_VERSION_PATTERN =
            Pattern.compile("\"flowVersion\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EPIC_ID_PATTERN =
            Pattern.compile("\"epicId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern LOCAL_FIRST_PATTERN =
            Pattern.compile("\"localFirstLifecycle\"\\s*:\\s*(true|false)");

    /**
     * Parses a minimal subset of an {@code execution-state.json} string.
     *
     * @param json the raw JSON content
     * @return the parsed ExecutionState
     * @throws IllegalArgumentException if {@code localFirstLifecycle=true} is declared but {@code
     *     flowVersion} is not "3" or "4" (impossible combination)
     */
    public static ExecutionState parse(String json) {
        String version = extractString(json, FLOW_VERSION_PATTERN, "1");
        String epicId = extractString(json, EPIC_ID_PATTERN, "");
        boolean explicitLocalFirst = extractBoolean(json, LOCAL_FIRST_PATTERN);
        boolean derivedLocalFirst = "3".equals(version) || "4".equals(version);

        if (explicitLocalFirst && !derivedLocalFirst) {
            throw new IllegalArgumentException(
                    "localFirstLifecycle=true is inconsistent with flowVersion='"
                            + version
                            + "' — localFirstLifecycle requires flowVersion 3 or 4");
        }

        return new ExecutionState(version, epicId, null, null, derivedLocalFirst);
    }

    private static String extractString(String json, Pattern pattern, String defaultVal) {
        Matcher m = pattern.matcher(json);
        return m.find() ? m.group(1) : defaultVal;
    }

    private static boolean extractBoolean(String json, Pattern pattern) {
        Matcher m = pattern.matcher(json);
        return m.find() && "true".equals(m.group(1));
    }
}

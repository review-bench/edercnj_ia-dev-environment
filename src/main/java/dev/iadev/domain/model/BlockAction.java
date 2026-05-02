package dev.iadev.domain.model;

/**
 * Action taken when a dependency policy finding matches a threshold.
 *
 * <p>Used in {@link BlockOnPolicy} to declare per-dimension enforcement behaviour. {@code BLOCK}
 * causes {@code x-dep-policy-validate} to exit non-zero; {@code WARN_ONLY} surfaces the finding
 * but proceeds; {@code IGNORE} suppresses the finding entirely (not recommended for
 * security-sensitive dimensions).
 *
 * <p>Introduced by EPIC-0074 (Dependency Policy & SCA Final Gate) — story-0074-0001.
 */
public enum BlockAction {

    /** Hard block: exit non-zero, fail the gate. */
    BLOCK,

    /** Surface as warning but continue; does not fail the gate. */
    WARN_ONLY,

    /** Suppress the finding entirely. */
    IGNORE;

    /**
     * Parses a YAML string value to a {@link BlockAction}.
     *
     * @param value the string from YAML (case-insensitive; also accepts {@code "any-violation"}
     *     as alias for {@code BLOCK} and {@code "warn-only"} for {@code WARN_ONLY})
     * @return the matching enum constant
     * @throws ConfigValidationException if the value is not recognised
     */
    public static BlockAction fromYaml(String value) {
        if (value == null) {
            return BLOCK;
        }
        return switch (value.toLowerCase()) {
            case "block", "any-violation", "hard-block" -> BLOCK;
            case "warn_only", "warn-only", "warning" -> WARN_ONLY;
            case "ignore" -> IGNORE;
            default -> throw new ConfigValidationException(
                    "Unsupported block-on action: '%s'. Accepted: block, any-violation, warn-only, ignore"
                            .formatted(value));
        };
    }
}

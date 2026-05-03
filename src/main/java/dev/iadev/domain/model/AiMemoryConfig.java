package dev.iadev.domain.model;

import java.util.Map;

/**
 * AI memory layer configuration.
 *
 * <p>Parsed from the {@code governance.ai-memory} YAML block. When the block is absent, or when
 * {@code enabled=false}, the memory layer is a no-op (Rule 19 safe default). Existing projects are
 * completely unaffected until they opt in.
 *
 * <pre>{@code
 * governance:
 *   ai-memory:
 *     enabled: true
 * }</pre>
 *
 * <p>Default {@link #DEFAULT} has {@code enabled=false}.
 *
 * <p>Introduced by EPIC-0075 (AI Memory Layer) — story-0075-0001.
 *
 * @param enabled whether the AI memory layer is active; default {@code false}
 */
public record AiMemoryConfig(boolean enabled) {

    /** Safe default: memory layer disabled — existing projects unaffected (Rule 19). */
    public static final AiMemoryConfig DEFAULT = new AiMemoryConfig(false);

    /**
     * Parses an {@code ai-memory} sub-map. A null map resolves to {@link #DEFAULT}.
     *
     * @param map the {@code ai-memory} sub-map (may be null)
     * @return parsed config, never null
     */
    public static AiMemoryConfig fromMap(Map<String, Object> map) {
        if (map == null) {
            return DEFAULT;
        }
        boolean enabled = MapHelper.optionalBoolean(map, "enabled", false);
        return new AiMemoryConfig(enabled);
    }
}

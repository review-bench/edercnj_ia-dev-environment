package dev.iadev.domain.model;

import java.util.Map;
import java.util.Set;

/**
 * Per-scope enforcement override for the dependency policy gate.
 *
 * <p>Allows projects to apply stricter or more lenient actions to different dependency scopes
 * (D-R11 — EPIC-0074). Default per D-R11:
 *
 * <ul>
 *   <li>{@code compile} → {@link BlockAction#BLOCK}
 *   <li>{@code runtime} → {@link BlockAction#BLOCK}
 *   <li>{@code test} → {@link BlockAction#WARN_ONLY}
 *   <li>{@code dev} → {@link BlockAction#WARN_ONLY}
 *   <li>{@code provided} → {@link BlockAction#WARN_ONLY}
 *   <li>{@code build} → {@link BlockAction#WARN_ONLY}
 * </ul>
 *
 * <p>Introduced by EPIC-0074 — story-0074-0001.
 */
public record ScopePolicy(
        BlockAction compile,
        BlockAction runtime,
        BlockAction test,
        BlockAction dev,
        BlockAction provided,
        BlockAction build) {

    /** Default per D-R11 (compile+runtime block; test/dev/provided/build warn). */
    public static final ScopePolicy DEFAULT =
            new ScopePolicy(
                    BlockAction.BLOCK,
                    BlockAction.BLOCK,
                    BlockAction.WARN_ONLY,
                    BlockAction.WARN_ONLY,
                    BlockAction.WARN_ONLY,
                    BlockAction.WARN_ONLY);

    /** Compact constructor applying D-R11 defaults for any null field. */
    public ScopePolicy {
        compile = compile == null ? BlockAction.BLOCK : compile;
        runtime = runtime == null ? BlockAction.BLOCK : runtime;
        test = test == null ? BlockAction.WARN_ONLY : test;
        dev = dev == null ? BlockAction.WARN_ONLY : dev;
        provided = provided == null ? BlockAction.WARN_ONLY : provided;
        build = build == null ? BlockAction.WARN_ONLY : build;
    }

    /**
     * Returns the {@link BlockAction} for the given scope string.
     *
     * @param scope the scope name (case-insensitive: compile, runtime, test, dev, provided, build)
     * @return the configured action for that scope
     * @throws ConfigValidationException when the scope string is not recognised
     */
    public BlockAction actionFor(String scope) {
        if (scope == null) {
            return compile;
        }
        return switch (scope.toLowerCase()) {
            case "compile" -> compile;
            case "runtime" -> runtime;
            case "test" -> test;
            case "dev", "devdependency", "devdependencies" -> dev;
            case "provided" -> provided;
            case "build" -> build;
            default ->
                    throw new ConfigValidationException(
                            ("Unknown dependency scope: '%s'. Accepted: compile, runtime, test,"
                                            + " dev, provided, build")
                                    .formatted(scope));
        };
    }

    /**
     * Parses the {@code scope-policy} sub-map.
     *
     * @param scopeMap the sub-map (may be empty; absent fields use D-R11 defaults)
     * @return a populated {@link ScopePolicy}
     */
    static ScopePolicy fromMap(Map<String, Object> scopeMap) {
        Set<String> known = Set.of("compile", "runtime", "test", "dev", "provided", "build");
        for (String key : scopeMap.keySet()) {
            if (!known.contains(key)) {
                throw new ConfigValidationException(
                        ("Unknown scope-policy key: '%s'. Accepted: compile, runtime, test,"
                                        + " dev, provided, build")
                                .formatted(key));
            }
        }
        return new ScopePolicy(
                BlockAction.fromYaml(MapHelper.optionalString(scopeMap, "compile", null)),
                BlockAction.fromYaml(MapHelper.optionalString(scopeMap, "runtime", null)),
                BlockAction.fromYaml(MapHelper.optionalString(scopeMap, "test", "warn-only")),
                BlockAction.fromYaml(MapHelper.optionalString(scopeMap, "dev", "warn-only")),
                BlockAction.fromYaml(MapHelper.optionalString(scopeMap, "provided", "warn-only")),
                BlockAction.fromYaml(MapHelper.optionalString(scopeMap, "build", "warn-only")));
    }
}

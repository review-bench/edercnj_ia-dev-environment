package dev.iadev.domain.model;

import java.util.Map;

/**
 * A single version constraint entry in a {@code min-versions} or {@code max-versions} list.
 *
 * <p>Supports three disjoint formats (D-R9 — EPIC-0074):
 *
 * <ul>
 *   <li><b>JVM</b> — Maven/Gradle: {@code groupId + artifactId + version}. {@code artifactId}
 *       accepts the wildcard {@code "*"} to match all artifacts under the group.
 *   <li><b>NPM/PyPI</b> — {@code name + version}. Wildcard {@code "*"} in {@code name} is
 *       rejected.
 *   <li><b>Go</b> — {@code module + version}. Full module path (e.g.
 *       {@code github.com/foo/bar}).
 * </ul>
 *
 * <p>Formats are mutually exclusive. Combining fields from different formats (e.g. {@code groupId}
 * + {@code name}) is a {@link ConfigValidationException} (ambiguous — D-R9).
 *
 * <p>Introduced by EPIC-0074 — story-0074-0001.
 */
public sealed interface VersionConstraint
        permits VersionConstraint.JvmConstraint,
                VersionConstraint.NpmConstraint,
                VersionConstraint.GoConstraint {

    /** The minimum or maximum version string as declared in YAML. */
    String version();

    /**
     * JVM (Maven / Gradle) constraint: {@code groupId:artifactId:version}.
     *
     * @param groupId the Maven group ID (required)
     * @param artifactId the Maven artifact ID or {@code "*"} for wildcard
     * @param version the version string (required)
     */
    record JvmConstraint(String groupId, String artifactId, String version)
            implements VersionConstraint {}

    /**
     * NPM / PyPI constraint: {@code name:version}.
     *
     * @param name the package registry name (required; wildcard {@code "*"} forbidden)
     * @param version the version string (required)
     */
    record NpmConstraint(String name, String version) implements VersionConstraint {}

    /**
     * Go module constraint: {@code module:version}.
     *
     * @param module the full Go module path, e.g. {@code github.com/foo/bar} (required)
     * @param version the version string (required)
     */
    record GoConstraint(String module, String version) implements VersionConstraint {}

    /**
     * Parses one entry map from the YAML {@code min-versions} / {@code max-versions} list.
     *
     * @param entry the individual entry map from YAML
     * @return a typed {@link VersionConstraint}
     * @throws ConfigValidationException on format ambiguity, missing required fields, or forbidden
     *     wildcard
     */
    @SuppressWarnings("unchecked")
    static VersionConstraint fromMap(Object entry) {
        if (!(entry instanceof Map<?, ?> rawMap)) {
            throw new ConfigValidationException(
                    "VersionConstraint entry must be a map, got: "
                            + (entry == null ? "null" : entry.getClass().getSimpleName()));
        }
        var map = (Map<String, Object>) rawMap;

        boolean hasGroupId = map.containsKey("groupId");
        boolean hasName = map.containsKey("name");
        boolean hasModule = map.containsKey("module");

        int formatCount = (hasGroupId ? 1 : 0) + (hasName ? 1 : 0) + (hasModule ? 1 : 0);
        if (formatCount > 1) {
            throw new ConfigValidationException(
                    "VersionConstraint: fields {groupId, name, module} are mutually exclusive"
                            + " — see D-R9. Found: "
                            + map.keySet());
        }

        String version = MapHelper.requireString(map, "version", "VersionConstraint");

        if (hasGroupId) {
            String groupId = MapHelper.requireString(map, "groupId", "VersionConstraint");
            String artifactId = MapHelper.optionalString(map, "artifactId", "*");
            return new JvmConstraint(groupId, artifactId, version);
        }
        if (hasName) {
            String name = MapHelper.requireString(map, "name", "VersionConstraint");
            if ("*".equals(name)) {
                throw new ConfigValidationException(
                        "VersionConstraint: wildcard '*' in NPM/PyPI 'name' is not allowed"
                                + " (WILDCARD_NOT_ALLOWED). Use groupId+artifactId for JVM wildcards.");
            }
            return new NpmConstraint(name, version);
        }
        if (hasModule) {
            String module = MapHelper.requireString(map, "module", "VersionConstraint");
            return new GoConstraint(module, version);
        }
        throw new ConfigValidationException(
                "VersionConstraint: one of {groupId, name, module} is required — see D-R9");
    }
}

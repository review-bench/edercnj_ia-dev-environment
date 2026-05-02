package dev.iadev.domain.model;

import java.util.Map;

/**
 * Per-dimension enforcement matrix for the dependency policy gate.
 *
 * <p>Declares what action ({@link BlockAction}) is taken when a finding is detected in each
 * dimension. Defaults are defined per D-R10 (EPIC-0074):
 *
 * <ul>
 *   <li>{@code severity-cve: HIGH} → {@link BlockAction#BLOCK}
 *   <li>{@code license: any-violation} → {@link BlockAction#BLOCK}
 *   <li>{@code min-version: any-violation} → {@link BlockAction#BLOCK}
 *   <li>{@code max-version: warn-only} → {@link BlockAction#WARN_ONLY}
 *   <li>{@code freshness: warn-only} → {@link BlockAction#WARN_ONLY}
 * </ul>
 *
 * <p>Introduced by EPIC-0074 — story-0074-0001.
 *
 * @param severityCve action when CVE severity ≥ HIGH (CVSS ≥ 7.0)
 * @param license action when a dependency license is not in the {@link LicenseWhitelist}
 * @param minVersion action when a dependency version is below the declared minimum
 * @param maxVersion action when a dependency version exceeds the declared maximum
 * @param freshness action when a dependency has not been updated within
 *     {@code freshness-window-days}
 */
public record BlockOnPolicy(
        BlockAction severityCve,
        BlockAction license,
        BlockAction minVersion,
        BlockAction maxVersion,
        BlockAction freshness) {

    /** Default enforcement matrix per D-R10. */
    public static final BlockOnPolicy DEFAULT =
            new BlockOnPolicy(
                    BlockAction.BLOCK,
                    BlockAction.BLOCK,
                    BlockAction.BLOCK,
                    BlockAction.WARN_ONLY,
                    BlockAction.WARN_ONLY);

    /** Compact constructor applying D-R10 defaults for any null field. */
    public BlockOnPolicy {
        severityCve = severityCve == null ? BlockAction.BLOCK : severityCve;
        license = license == null ? BlockAction.BLOCK : license;
        minVersion = minVersion == null ? BlockAction.BLOCK : minVersion;
        maxVersion = maxVersion == null ? BlockAction.WARN_ONLY : maxVersion;
        freshness = freshness == null ? BlockAction.WARN_ONLY : freshness;
    }

    /**
     * Parses the {@code block-on} sub-map.
     *
     * @param blockOnMap the {@code block-on} sub-map (may be empty; absent fields use defaults)
     * @return a populated {@link BlockOnPolicy}
     */
    static BlockOnPolicy fromMap(Map<String, Object> blockOnMap) {
        return new BlockOnPolicy(
                BlockAction.fromYaml(MapHelper.optionalString(blockOnMap, "severity-cve", null)),
                BlockAction.fromYaml(MapHelper.optionalString(blockOnMap, "license", null)),
                BlockAction.fromYaml(MapHelper.optionalString(blockOnMap, "min-version", null)),
                BlockAction.fromYaml(MapHelper.optionalString(blockOnMap, "max-version", "warn-only")),
                BlockAction.fromYaml(MapHelper.optionalString(blockOnMap, "freshness", "warn-only")));
    }
}

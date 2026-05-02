package dev.iadev.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Dependency policy configuration for the SCA final gate.
 *
 * <p>Parsed from the {@code dependencies.policy} YAML block. When the block is absent, or when
 * {@code enabled=false}, the gate is a no-op (Rule 19 safe default). The schema supports
 * cross-stack syntax for version constraints (D-R9 — EPIC-0074).
 *
 * <p>Default {@link #DEFAULT} has {@code enabled=false} — existing projects are unaffected.
 *
 * <pre>{@code
 * dependencies:
 *   policy:
 *     enabled: true
 *     min-versions:
 *       - { groupId: org.springframework.boot, artifactId: "*", version: "3.2.0" }
 *       - { name: lodash, version: "4.17.21" }
 *     max-versions:
 *       - { groupId: org.springframework.boot, artifactId: "*", version: "3.x" }
 *     allowed-licenses:
 *       - Apache-2.0
 *       - MIT
 *       - BSD-3-Clause
 *     denied-cves:
 *       - CVE-2024-12345
 *     freshness-window-days: 365
 *     block-on:
 *       severity-cve: HIGH
 *       license: any-violation
 *       min-version: any-violation
 *       max-version: warn-only
 *       freshness: warn-only
 * }</pre>
 *
 * <p>Introduced by EPIC-0074 (Dependency Policy &amp; SCA Final Gate) — story-0074-0001.
 *
 * @param enabled whether the policy gate is active (default {@code false})
 * @param minVersions minimum required versions per dependency (cross-stack D-R9)
 * @param maxVersions maximum allowed versions per dependency (cross-stack D-R9)
 * @param allowedLicenses SPDX license identifiers that are whitelisted
 * @param deniedCves CVE identifiers that hard-block regardless of patch availability (RULE-074-01)
 * @param freshnessWindowDays days before a dependency is considered stale (default 365)
 * @param blockOn per-dimension enforcement actions (default matrix per D-R10)
 * @param scopePolicy per-scope enforcement overrides (default per D-R11)
 */
public record DependencyPolicyConfig(
        boolean enabled,
        List<VersionConstraint> minVersions,
        List<VersionConstraint> maxVersions,
        LicenseWhitelist allowedLicenses,
        List<String> deniedCves,
        int freshnessWindowDays,
        BlockOnPolicy blockOn,
        ScopePolicy scopePolicy) {

    /** Gate disabled — safe default when {@code dependencies.policy} block is absent. */
    public static final DependencyPolicyConfig DEFAULT =
            new DependencyPolicyConfig(
                    false,
                    List.of(),
                    List.of(),
                    LicenseWhitelist.EMPTY,
                    List.of(),
                    365,
                    BlockOnPolicy.DEFAULT,
                    ScopePolicy.DEFAULT);

    /** Compact constructor enforcing list immutability. */
    public DependencyPolicyConfig {
        minVersions = minVersions == null ? List.of() : List.copyOf(minVersions);
        maxVersions = maxVersions == null ? List.of() : List.copyOf(maxVersions);
        deniedCves = deniedCves == null ? List.of() : List.copyOf(deniedCves);
        allowedLicenses = allowedLicenses == null ? LicenseWhitelist.EMPTY : allowedLicenses;
        blockOn = blockOn == null ? BlockOnPolicy.DEFAULT : blockOn;
        scopePolicy = scopePolicy == null ? ScopePolicy.DEFAULT : scopePolicy;
        if (freshnessWindowDays < 0) {
            throw new ConfigValidationException(
                    "dependencies.policy.freshness-window-days must be ≥ 0, got: "
                            + freshnessWindowDays);
        }
    }

    /**
     * Creates a {@link DependencyPolicyConfig} from the {@code dependencies.policy} sub-map.
     *
     * <p>Returns {@link #DEFAULT} (disabled) when the map is empty or absent.
     *
     * @param policyMap the {@code dependencies.policy} sub-map (may be empty)
     * @return a populated config or {@link #DEFAULT} when policy is absent/disabled
     */
    @SuppressWarnings("unchecked")
    public static DependencyPolicyConfig fromMap(Map<String, Object> policyMap) {
        if (policyMap.isEmpty()) {
            return DEFAULT;
        }
        boolean enabled = MapHelper.optionalBoolean(policyMap, "enabled", false);
        if (!enabled) {
            return DEFAULT;
        }

        List<VersionConstraint> minVersions = parseConstraints(policyMap, "min-versions");
        List<VersionConstraint> maxVersions = parseConstraints(policyMap, "max-versions");
        LicenseWhitelist allowedLicenses = LicenseWhitelist.fromMap(policyMap);
        List<String> deniedCves = MapHelper.optionalStringList(policyMap, "denied-cves");
        int freshnessWindowDays = MapHelper.optionalInt(policyMap, "freshness-window-days", 365);
        BlockOnPolicy blockOn =
                BlockOnPolicy.fromMap(MapHelper.optionalMap(policyMap, "block-on"));
        ScopePolicy scopePolicy =
                ScopePolicy.fromMap(MapHelper.optionalMap(policyMap, "scope-policy"));

        return new DependencyPolicyConfig(
                true,
                minVersions,
                maxVersions,
                allowedLicenses,
                deniedCves,
                freshnessWindowDays,
                blockOn,
                scopePolicy);
    }

    @SuppressWarnings("unchecked")
    private static List<VersionConstraint> parseConstraints(
            Map<String, Object> policyMap, String key) {
        Object raw = policyMap.get(key);
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list)) {
            throw new ConfigValidationException(
                    "dependencies.policy." + key + " must be a YAML list");
        }
        return list.stream().map(VersionConstraint::fromMap).toList();
    }
}

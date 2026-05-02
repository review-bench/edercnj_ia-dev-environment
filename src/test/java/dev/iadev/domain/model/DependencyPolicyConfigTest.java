package dev.iadev.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DependencyPolicyConfig")
class DependencyPolicyConfigTest {

    @Nested
    @DisplayName("DEFAULT — safe no-op when policy block absent (Rule 19)")
    class Defaults {

        @Test
        void default_disabled() {
            assertThat(DependencyPolicyConfig.DEFAULT.enabled()).isFalse();
        }

        @Test
        void default_emptyVersionLists() {
            assertThat(DependencyPolicyConfig.DEFAULT.minVersions()).isEmpty();
            assertThat(DependencyPolicyConfig.DEFAULT.maxVersions()).isEmpty();
        }

        @Test
        void default_emptyDeniedCves() {
            assertThat(DependencyPolicyConfig.DEFAULT.deniedCves()).isEmpty();
        }

        @Test
        void default_freshnessWindow_365() {
            assertThat(DependencyPolicyConfig.DEFAULT.freshnessWindowDays()).isEqualTo(365);
        }

        @Test
        void default_blockOnPolicy_isD_R10() {
            var blockOn = DependencyPolicyConfig.DEFAULT.blockOn();
            assertThat(blockOn.severityCve()).isEqualTo(BlockAction.BLOCK);
            assertThat(blockOn.license()).isEqualTo(BlockAction.BLOCK);
            assertThat(blockOn.minVersion()).isEqualTo(BlockAction.BLOCK);
            assertThat(blockOn.maxVersion()).isEqualTo(BlockAction.WARN_ONLY);
            assertThat(blockOn.freshness()).isEqualTo(BlockAction.WARN_ONLY);
        }

        @Test
        void default_scopePolicy_isD_R11() {
            var scope = DependencyPolicyConfig.DEFAULT.scopePolicy();
            assertThat(scope.compile()).isEqualTo(BlockAction.BLOCK);
            assertThat(scope.runtime()).isEqualTo(BlockAction.BLOCK);
            assertThat(scope.test()).isEqualTo(BlockAction.WARN_ONLY);
            assertThat(scope.dev()).isEqualTo(BlockAction.WARN_ONLY);
            assertThat(scope.provided()).isEqualTo(BlockAction.WARN_ONLY);
            assertThat(scope.build()).isEqualTo(BlockAction.WARN_ONLY);
        }

        @Test
        void fromMap_emptyMap_returnsDefault() {
            var cfg = DependencyPolicyConfig.fromMap(Map.of());
            assertThat(cfg.enabled()).isFalse();
        }

        @Test
        void fromMap_enabledFalse_returnsDefault() {
            var cfg = DependencyPolicyConfig.fromMap(Map.of("enabled", false));
            assertThat(cfg.enabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("fromMap — full policy block")
    class FullPolicy {

        @Test
        void fromMap_enabled_parsesMinVersions() {
            var map =
                    Map.of(
                            "enabled",
                            true,
                            "min-versions",
                            List.of(
                                    Map.of(
                                            "groupId", "org.springframework.boot",
                                            "version", "3.2.0")));

            var cfg = DependencyPolicyConfig.fromMap(map);

            assertThat(cfg.enabled()).isTrue();
            assertThat(cfg.minVersions()).hasSize(1);
            var constraint = (VersionConstraint.JvmConstraint) cfg.minVersions().get(0);
            assertThat(constraint.groupId()).isEqualTo("org.springframework.boot");
            assertThat(constraint.artifactId()).isEqualTo("*");
            assertThat(constraint.version()).isEqualTo("3.2.0");
        }

        @Test
        void fromMap_enabled_parsesNpmConstraint() {
            var map =
                    Map.of(
                            "enabled",
                            true,
                            "min-versions",
                            List.of(Map.of("name", "lodash", "version", "4.17.21")));

            var cfg = DependencyPolicyConfig.fromMap(map);

            var constraint = (VersionConstraint.NpmConstraint) cfg.minVersions().get(0);
            assertThat(constraint.name()).isEqualTo("lodash");
            assertThat(constraint.version()).isEqualTo("4.17.21");
        }

        @Test
        void fromMap_enabled_parsesGoConstraint() {
            var map =
                    Map.of(
                            "enabled",
                            true,
                            "max-versions",
                            List.of(Map.of("module", "github.com/foo/bar", "version", "v1.2.0")));

            var cfg = DependencyPolicyConfig.fromMap(map);

            var constraint = (VersionConstraint.GoConstraint) cfg.maxVersions().get(0);
            assertThat(constraint.module()).isEqualTo("github.com/foo/bar");
            assertThat(constraint.version()).isEqualTo("v1.2.0");
        }

        @Test
        void fromMap_enabled_parsesDeniedCvesAndFreshness() {
            var map =
                    Map.of(
                            "enabled",
                            true,
                            "denied-cves",
                            List.of("CVE-2024-12345"),
                            "freshness-window-days",
                            180);

            var cfg = DependencyPolicyConfig.fromMap(map);

            assertThat(cfg.deniedCves()).containsExactly("CVE-2024-12345");
            assertThat(cfg.freshnessWindowDays()).isEqualTo(180);
        }

        @Test
        void fromMap_enabled_parsesAllowedLicenses() {
            var map =
                    Map.of(
                            "enabled",
                            true,
                            "allowed-licenses",
                            List.of("Apache-2.0", "MIT", "BSD-3-Clause"));

            var cfg = DependencyPolicyConfig.fromMap(map);

            assertThat(cfg.allowedLicenses().allowed())
                    .containsExactly("Apache-2.0", "MIT", "BSD-3-Clause");
        }
    }

    @Nested
    @DisplayName("compact constructor — immutability and null-safety")
    class CompactConstructor {

        @Test
        void nullMinVersions_defaults_toEmptyList() {
            var cfg = new DependencyPolicyConfig(false, null, null, null, null, 365, null, null);
            assertThat(cfg.minVersions()).isEmpty();
            assertThat(cfg.maxVersions()).isEmpty();
            assertThat(cfg.deniedCves()).isEmpty();
        }

        @Test
        void negativeFreshnessWindow_throws() {
            assertThatThrownBy(
                            () ->
                                    new DependencyPolicyConfig(
                                            false, List.of(), List.of(), null, List.of(), -1, null,
                                            null))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("freshness-window-days");
        }

        @Test
        void minVersionsList_isImmutable() {
            var mutable = new java.util.ArrayList<VersionConstraint>();
            var cfg =
                    new DependencyPolicyConfig(
                            true, mutable, List.of(), null, List.of(), 365, null, null);
            assertThatThrownBy(() -> cfg.minVersions().add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("VersionConstraint.fromMap — D-R9 validation")
    class VersionConstraintParsing {

        @Test
        void ambiguousFormat_groupIdAndName_throws() {
            assertThatThrownBy(
                            () ->
                                    VersionConstraint.fromMap(
                                            Map.of(
                                                    "groupId",
                                                    "com.example",
                                                    "name",
                                                    "foo",
                                                    "version",
                                                    "1.0")))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("mutually exclusive");
        }

        @Test
        void npmWildcardName_throws() {
            assertThatThrownBy(
                            () -> VersionConstraint.fromMap(Map.of("name", "*", "version", "1.0")))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("WILDCARD_NOT_ALLOWED");
        }

        @Test
        void missingIdentifierField_throws() {
            assertThatThrownBy(() -> VersionConstraint.fromMap(Map.of("version", "1.0")))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("groupId, name, module");
        }
    }

    @Nested
    @DisplayName("ScopePolicy.actionFor — D-R11 scope resolution")
    class ScopePolicyActionFor {

        @Test
        void nullScope_resolvesToCompileAction() {
            assertThat(ScopePolicy.DEFAULT.actionFor(null)).isEqualTo(BlockAction.BLOCK);
        }

        @Test
        void devDependencies_resolvesToDevAction() {
            assertThat(ScopePolicy.DEFAULT.actionFor("devDependencies"))
                    .isEqualTo(BlockAction.WARN_ONLY);
        }

        @Test
        void unknownScope_throws() {
            assertThatThrownBy(() -> ScopePolicy.DEFAULT.actionFor("optional"))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("Unknown dependency scope");
        }
    }
}

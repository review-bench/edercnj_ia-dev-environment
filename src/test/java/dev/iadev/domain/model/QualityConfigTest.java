package dev.iadev.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("QualityConfig")
class QualityConfigTest {

    @Nested
    @DisplayName("DEFAULT — safe defaults when quality block absent")
    class Defaults {

        @Test
        void default_performance_disabled() {
            assertThat(QualityConfig.DEFAULT.performance().enabled()).isFalse();
        }

        @Test
        void default_mutation_disabled() {
            assertThat(QualityConfig.DEFAULT.mutation().enabled()).isFalse();
        }

        @Test
        void default_contract_disabled() {
            assertThat(QualityConfig.DEFAULT.contract().enabled()).isFalse();
        }

        @Test
        void fromMap_emptyMap_returnsAllDisabled() {
            var cfg = QualityConfig.fromMap(Map.of());

            assertThat(cfg.performance().enabled()).isFalse();
            assertThat(cfg.mutation().enabled()).isFalse();
            assertThat(cfg.contract().enabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("fromMap — performance block")
    class PerformanceBlock {

        @Test
        void fromMap_performanceEnabled_parsesFlag() {
            var m = Map.<String, Object>of("performance", Map.of("enabled", true));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.performance().enabled()).isTrue();
        }

        @Test
        void fromMap_baselineTolerancePct_parsed() {
            var m =
                    Map.<String, Object>of(
                            "performance",
                            Map.<String, Object>of("enabled", true, "baseline_tolerance_pct", 15));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.performance().baselineTolerancePct()).isEqualTo(15);
        }

        @Test
        void fromMap_sloRestBlock_parsed() {
            var restSlo =
                    Map.<String, Object>of(
                            "p50_ms", 50, "p95_ms", 200, "p99_ms", 500, "throughput_rps", 1000);
            var m =
                    Map.<String, Object>of(
                            "performance",
                            Map.<String, Object>of(
                                    "enabled", true, "slo", Map.of("rest", restSlo)));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.performance().slo().rest().p50Ms()).isEqualTo(50);
            assertThat(cfg.performance().slo().rest().p95Ms()).isEqualTo(200);
            assertThat(cfg.performance().slo().rest().p99Ms()).isEqualTo(500);
            assertThat(cfg.performance().slo().rest().throughputRps()).isEqualTo(1000);
        }

        @Test
        void fromMap_sloGrpcBlock_parsed() {
            var grpcSlo = Map.<String, Object>of("p50_ms", 20, "p95_ms", 100, "p99_ms", 200);
            var m =
                    Map.<String, Object>of(
                            "performance",
                            Map.<String, Object>of(
                                    "enabled", true, "slo", Map.of("grpc", grpcSlo)));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.performance().slo().grpc().p50Ms()).isEqualTo(20);
            assertThat(cfg.performance().slo().grpc().p99Ms()).isEqualTo(200);
        }

        @Test
        void fromMap_sloCliBlock_parsed() {
            var cliSlo = Map.<String, Object>of("gen_time_p95_ms", 4000);
            var m =
                    Map.<String, Object>of(
                            "performance",
                            Map.<String, Object>of("enabled", true, "slo", Map.of("cli", cliSlo)));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.performance().slo().cli().genTimeP95Ms()).isEqualTo(4000);
        }

        @Test
        void fromMap_missingSloBlock_usesDefaults() {
            var m = Map.<String, Object>of("performance", Map.of("enabled", true));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.performance().slo().rest().p95Ms()).isEqualTo(200);
            assertThat(cfg.performance().slo().grpc().p95Ms()).isEqualTo(100);
            assertThat(cfg.performance().slo().cli().genTimeP95Ms()).isEqualTo(3000);
        }
    }

    @Nested
    @DisplayName("fromMap — mutation block")
    class MutationBlock {

        @Test
        void fromMap_mutationEnabled_parsesThreshold() {
            var m =
                    Map.<String, Object>of(
                            "mutation", Map.<String, Object>of("enabled", true, "threshold", 85));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.mutation().enabled()).isTrue();
            assertThat(cfg.mutation().threshold()).isEqualTo(85);
        }

        @Test
        void fromMap_mutationRuntimeCap_parsed() {
            var m =
                    Map.<String, Object>of(
                            "mutation",
                            Map.<String, Object>of("enabled", true, "runtime_cap_min", 20));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.mutation().runtimeCapMin()).isEqualTo(20);
        }

        @Test
        void fromMap_missingMutationBlock_usesDefaults() {
            var cfg = QualityConfig.fromMap(Map.of());

            assertThat(cfg.mutation().threshold()).isEqualTo(80);
            assertThat(cfg.mutation().runtimeCapMin()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("fromMap — contract block")
    class ContractBlock {

        @Test
        void fromMap_contractEnabled_parsesFlags() {
            var m =
                    Map.<String, Object>of(
                            "contract",
                            Map.<String, Object>of(
                                    "enabled", true,
                                    "pact", true,
                                    "openapi_breaking", false,
                                    "proto_breaking", false));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.contract().enabled()).isTrue();
            assertThat(cfg.contract().pact()).isTrue();
            assertThat(cfg.contract().openapiBreaking()).isFalse();
            assertThat(cfg.contract().protoBreaking()).isFalse();
        }

        @Test
        void fromMap_missingContractBlock_openapiBreakingDefaultsTrue() {
            var cfg = QualityConfig.fromMap(Map.of());

            assertThat(cfg.contract().openapiBreaking()).isTrue();
            assertThat(cfg.contract().protoBreaking()).isTrue();
            assertThat(cfg.contract().pact()).isFalse();
        }
    }

    @Nested
    @DisplayName("compact constructor — null safety")
    class NullSafety {

        @Test
        void constructor_nullPerformance_usesDefault() {
            var cfg =
                    new QualityConfig(
                            null,
                            QualityConfig.DEFAULT.mutation(),
                            QualityConfig.DEFAULT.contract(),
                            QualityConfig.DEFAULT.regression(),
                            QualityConfig.DEFAULT.dast());

            assertThat(cfg.performance().enabled()).isFalse();
        }

        @Test
        void constructor_nullMutation_usesDefault() {
            var cfg =
                    new QualityConfig(
                            QualityConfig.DEFAULT.performance(),
                            null,
                            QualityConfig.DEFAULT.contract(),
                            QualityConfig.DEFAULT.regression(),
                            QualityConfig.DEFAULT.dast());

            assertThat(cfg.mutation().threshold()).isEqualTo(80);
        }

        @Test
        void constructor_nullContract_usesDefault() {
            var cfg =
                    new QualityConfig(
                            QualityConfig.DEFAULT.performance(),
                            QualityConfig.DEFAULT.mutation(),
                            null,
                            QualityConfig.DEFAULT.regression(),
                            QualityConfig.DEFAULT.dast());

            assertThat(cfg.contract().openapiBreaking()).isTrue();
        }

        @Test
        void constructor_nullRegression_usesDefault() {
            var cfg =
                    new QualityConfig(
                            QualityConfig.DEFAULT.performance(),
                            QualityConfig.DEFAULT.mutation(),
                            QualityConfig.DEFAULT.contract(),
                            null,
                            QualityConfig.DEFAULT.dast());

            assertThat(cfg.regression().enabled()).isFalse();
            assertThat(cfg.regression().mode()).isEqualTo("service");
        }

        @Test
        void constructor_nullDast_usesDefault() {
            var cfg =
                    new QualityConfig(
                            QualityConfig.DEFAULT.performance(),
                            QualityConfig.DEFAULT.mutation(),
                            QualityConfig.DEFAULT.contract(),
                            QualityConfig.DEFAULT.regression(),
                            null);

            assertThat(cfg.dast().enabled()).isFalse();
            assertThat(cfg.dast().target()).isEqualTo("local-container");
        }
    }

    @Nested
    @DisplayName("fromMap — regression block")
    class RegressionBlock {

        @Test
        void fromMap_regressionEnabled_serviceMode() {
            var m =
                    Map.<String, Object>of(
                            "regression",
                            Map.<String, Object>of("enabled", true, "mode", "service"));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.regression().enabled()).isTrue();
            assertThat(cfg.regression().mode()).isEqualTo("service");
        }

        @Test
        void fromMap_regressionSelfMode_parsedCorrectly() {
            var m =
                    Map.<String, Object>of(
                            "regression",
                            Map.<String, Object>of("enabled", true, "mode", "self"));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.regression().mode()).isEqualTo("self");
        }

        @Test
        void fromMap_regressionScenariosFile_parsed() {
            var m =
                    Map.<String, Object>of(
                            "regression",
                            Map.<String, Object>of(
                                    "enabled", true,
                                    "scenarios-file", "tests/regression/custom.yaml"));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.regression().scenariosFile())
                    .isEqualTo("tests/regression/custom.yaml");
        }

        @Test
        void fromMap_missingRegressionBlock_usesDefaults() {
            var cfg = QualityConfig.fromMap(Map.of());

            assertThat(cfg.regression().enabled()).isFalse();
            assertThat(cfg.regression().mode()).isEqualTo("service");
            assertThat(cfg.regression().scenariosFile())
                    .isEqualTo("tests/regression/scenarios.yaml");
        }
    }

    @Nested
    @DisplayName("fromMap — dast block")
    class DastBlock {

        @Test
        void fromMap_dastEnabled_parsesBasicFields() {
            var m =
                    Map.<String, Object>of(
                            "dast",
                            Map.<String, Object>of(
                                    "enabled", true,
                                    "tier-pr", "smoke",
                                    "tier-nightly", "full",
                                    "target", "local-container"));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.dast().enabled()).isTrue();
            assertThat(cfg.dast().tierPr()).isEqualTo("smoke");
            assertThat(cfg.dast().tierNightly()).isEqualTo("full");
            assertThat(cfg.dast().target()).isEqualTo("local-container");
        }

        @Test
        void fromMap_missingDastBlock_usesDefaults() {
            var cfg = QualityConfig.fromMap(Map.of());

            assertThat(cfg.dast().enabled()).isFalse();
            assertThat(cfg.dast().tierPr()).isEqualTo("smoke");
            assertThat(cfg.dast().tierNightly()).isEqualTo("full");
            assertThat(cfg.dast().target()).isEqualTo("local-container");
        }

        @Test
        void dastConfig_targetProduction_throwsConfigValidationException() {
            var dastMap = Map.<String, Object>of("enabled", true, "target", "production");

            assertThatThrownBy(
                            () -> QualityConfig.DastConfig.fromMap(dastMap))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("DAST_TARGET_PRODUCTION_FORBIDDEN");
        }

        @Test
        void dastConfig_productionCaseInsensitive_alsoRejected() {
            assertThatThrownBy(
                            () ->
                                    new QualityConfig.DastConfig(
                                            true, "smoke", "full", "Production",
                                            QualityConfig.DastConfig.ZapConfig.DEFAULT,
                                            QualityConfig.DastConfig.NucleiConfig.DEFAULT,
                                            java.util.List.of()))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("DAST_TARGET_PRODUCTION_FORBIDDEN");
        }

        @Test
        void fromMap_zapBlock_parsed() {
            var zapMap = Map.<String, Object>of("enabled", true, "active-scan-policy", "custom-pci");
            var m = Map.<String, Object>of("dast", Map.<String, Object>of("zap", zapMap));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.dast().zap().enabled()).isTrue();
            assertThat(cfg.dast().zap().activeScanPolicy()).isEqualTo("custom-pci");
        }

        @Test
        void fromMap_nucleiBlock_pinnedVersion_accepted() {
            var nucleiMap =
                    Map.<String, Object>of("enabled", true, "templates-version", "v9.x");
            var m = Map.<String, Object>of("dast", Map.<String, Object>of("nuclei", nucleiMap));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.dast().nuclei().enabled()).isTrue();
            assertThat(cfg.dast().nuclei().templatesVersion()).isEqualTo("v9.x");
        }

        @Test
        void fromMap_nucleiBlock_pinnedVersionFull_accepted() {
            var nucleiMap =
                    Map.<String, Object>of("enabled", true, "templates-version", "v9.0.1");
            var m = Map.<String, Object>of("dast", Map.<String, Object>of("nuclei", nucleiMap));

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.dast().nuclei().templatesVersion()).isEqualTo("v9.0.1");
        }

        @Test
        void nucleiConfig_latestVersion_throwsNucleiVersionUnpinned() {
            assertThatThrownBy(
                            () ->
                                    new QualityConfig.DastConfig.NucleiConfig(
                                            true, "latest"))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("NUCLEI_VERSION_UNPINNED");
        }

        @Test
        void nucleiConfig_masterVersion_throwsNucleiVersionUnpinned() {
            assertThatThrownBy(
                            () ->
                                    new QualityConfig.DastConfig.NucleiConfig(
                                            true, "master"))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("NUCLEI_VERSION_UNPINNED");
        }

        @Test
        void nucleiConfig_headVersion_throwsNucleiVersionUnpinned() {
            assertThatThrownBy(
                            () ->
                                    new QualityConfig.DastConfig.NucleiConfig(
                                            true, "HEAD"))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("NUCLEI_VERSION_UNPINNED");
        }

        @Test
        void nucleiConfig_arbitraryUnpinnedString_throwsNucleiVersionUnpinned() {
            assertThatThrownBy(
                            () ->
                                    new QualityConfig.DastConfig.NucleiConfig(
                                            true, "nightly"))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("NUCLEI_VERSION_UNPINNED");
        }

        @Test
        void fromMap_complianceList_parsed() {
            var dastMap =
                    Map.<String, Object>of(
                            "enabled", true,
                            "compliance", java.util.List.of("pci", "lgpd"));
            var m = Map.<String, Object>of("dast", dastMap);

            var cfg = QualityConfig.fromMap(m);

            assertThat(cfg.dast().compliance()).containsExactly("pci", "lgpd");
        }

        @Test
        void fromMap_missingCompliance_defaultsEmpty() {
            var cfg = QualityConfig.fromMap(Map.of());

            assertThat(cfg.dast().compliance()).isEmpty();
        }
    }

    @Nested
    @DisplayName("DEFAULT — regression and dast safe defaults")
    class RegressionDastDefaults {

        @Test
        void default_regression_disabled() {
            assertThat(QualityConfig.DEFAULT.regression().enabled()).isFalse();
        }

        @Test
        void default_regression_modeIsService() {
            assertThat(QualityConfig.DEFAULT.regression().mode()).isEqualTo("service");
        }

        @Test
        void default_dast_disabled() {
            assertThat(QualityConfig.DEFAULT.dast().enabled()).isFalse();
        }

        @Test
        void default_dast_targetIsLocalContainer() {
            assertThat(QualityConfig.DEFAULT.dast().target()).isEqualTo("local-container");
        }

        @Test
        void fromMap_emptyMap_regressionAndDastDisabled() {
            var cfg = QualityConfig.fromMap(Map.of());

            assertThat(cfg.regression().enabled()).isFalse();
            assertThat(cfg.dast().enabled()).isFalse();
        }
    }
}

package dev.iadev.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

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
                            QualityConfig.DEFAULT.contract());

            assertThat(cfg.performance().enabled()).isFalse();
        }

        @Test
        void constructor_nullMutation_usesDefault() {
            var cfg =
                    new QualityConfig(
                            QualityConfig.DEFAULT.performance(),
                            null,
                            QualityConfig.DEFAULT.contract());

            assertThat(cfg.mutation().threshold()).isEqualTo(80);
        }

        @Test
        void constructor_nullContract_usesDefault() {
            var cfg =
                    new QualityConfig(
                            QualityConfig.DEFAULT.performance(),
                            QualityConfig.DEFAULT.mutation(),
                            null);

            assertThat(cfg.contract().openapiBreaking()).isTrue();
        }
    }
}

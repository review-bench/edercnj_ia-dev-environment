package dev.iadev.domain.model;

import java.util.Map;

/**
 * Quality-gate configuration for the three extended test dimensions introduced by EPIC-0072.
 *
 * <p>Parsed from the {@code quality:} top-level YAML block. When the block is absent, all
 * sub-configs default to {@code enabled=false} (safe defaults per Rule 19 — existing projects do
 * not break).
 *
 * <pre>{@code
 * quality:
 *   performance:
 *     enabled: true
 *     slo:
 *       rest: { p50_ms: 50, p95_ms: 200, p99_ms: 500, throughput_rps: 1000 }
 *       grpc: { p50_ms: 20, p95_ms: 100, p99_ms: 200 }
 *       cli:  { gen_time_p95_ms: 3000 }
 *     baseline_tolerance_pct: 10
 *   mutation:
 *     enabled: true
 *     threshold: 80
 *     runtime_cap_min: 10
 *   contract:
 *     enabled: true
 *     pact: false
 *     openapi_breaking: true
 *     proto_breaking: true
 * }</pre>
 *
 * @param performance performance-testing SLO thresholds (stack-aware via {@code /x-test-performance})
 * @param mutation    mutation-testing threshold (stack-aware via {@code /x-test-mutation})
 * @param contract    contract-breaking-change gate (stack-aware via {@code /x-test-contract})
 * @see dev.iadev.domain.model.Governance
 */
public record QualityConfig(
        PerformanceConfig performance, MutationConfig mutation, ContractConfig contract) {

    /** All quality gates disabled — safe default when {@code quality:} block is absent. */
    public static final QualityConfig DEFAULT =
            new QualityConfig(
                    PerformanceConfig.DEFAULT, MutationConfig.DEFAULT, ContractConfig.DEFAULT);

    /**
     * Creates a {@link QualityConfig} from the {@code quality} sub-map.
     *
     * @param qualityMap the {@code quality} sub-map from the YAML root (may be empty)
     * @return a populated QualityConfig, never null
     */
    public static QualityConfig fromMap(Map<String, Object> qualityMap) {
        return new QualityConfig(
                PerformanceConfig.fromMap(MapHelper.optionalMap(qualityMap, "performance")),
                MutationConfig.fromMap(MapHelper.optionalMap(qualityMap, "mutation")),
                ContractConfig.fromMap(MapHelper.optionalMap(qualityMap, "contract")));
    }

    /**
     * Performance-testing gate configuration.
     *
     * @param enabled              whether the performance gate is active
     * @param slo                  SLO thresholds per interface type
     * @param baselineTolerancePct tolerance % for baseline drift (default 10)
     * @param toolVersions         optional tool version pins (default empty)
     */
    public record PerformanceConfig(
            boolean enabled,
            SloConfig slo,
            int baselineTolerancePct,
            Map<String, Object> toolVersions) {

        static final PerformanceConfig DEFAULT =
                new PerformanceConfig(false, SloConfig.DEFAULT, 10, Map.of());

        public PerformanceConfig {
            slo = slo == null ? SloConfig.DEFAULT : slo;
            toolVersions = toolVersions == null ? Map.of() : Map.copyOf(toolVersions);
        }

        static PerformanceConfig fromMap(Map<String, Object> m) {
            return new PerformanceConfig(
                    MapHelper.optionalBoolean(m, "enabled", false),
                    SloConfig.fromMap(MapHelper.optionalMap(m, "slo")),
                    MapHelper.optionalInt(m, "baseline_tolerance_pct", 10),
                    MapHelper.optionalMap(m, "tool_versions"));
        }
    }

    /**
     * SLO thresholds across interface types.
     *
     * @param rest REST P50/P95/P99 latency + throughput
     * @param grpc gRPC P50/P95/P99 latency
     * @param cli  CLI generation-time P95
     */
    public record SloConfig(RestSlo rest, GrpcSlo grpc, CliSlo cli) {

        static final SloConfig DEFAULT =
                new SloConfig(RestSlo.DEFAULT, GrpcSlo.DEFAULT, CliSlo.DEFAULT);

        public SloConfig {
            rest = rest == null ? RestSlo.DEFAULT : rest;
            grpc = grpc == null ? GrpcSlo.DEFAULT : grpc;
            cli = cli == null ? CliSlo.DEFAULT : cli;
        }

        static SloConfig fromMap(Map<String, Object> m) {
            return new SloConfig(
                    RestSlo.fromMap(MapHelper.optionalMap(m, "rest")),
                    GrpcSlo.fromMap(MapHelper.optionalMap(m, "grpc")),
                    CliSlo.fromMap(MapHelper.optionalMap(m, "cli")));
        }
    }

    /** REST SLO thresholds (milliseconds + requests/second). */
    public record RestSlo(int p50Ms, int p95Ms, int p99Ms, int throughputRps) {

        static final RestSlo DEFAULT = new RestSlo(100, 200, 500, 100);

        static RestSlo fromMap(Map<String, Object> m) {
            return new RestSlo(
                    MapHelper.optionalInt(m, "p50_ms", 100),
                    MapHelper.optionalInt(m, "p95_ms", 200),
                    MapHelper.optionalInt(m, "p99_ms", 500),
                    MapHelper.optionalInt(m, "throughput_rps", 100));
        }
    }

    /** gRPC SLO thresholds (milliseconds). */
    public record GrpcSlo(int p50Ms, int p95Ms, int p99Ms) {

        static final GrpcSlo DEFAULT = new GrpcSlo(20, 100, 200);

        static GrpcSlo fromMap(Map<String, Object> m) {
            return new GrpcSlo(
                    MapHelper.optionalInt(m, "p50_ms", 20),
                    MapHelper.optionalInt(m, "p95_ms", 100),
                    MapHelper.optionalInt(m, "p99_ms", 200));
        }
    }

    /** CLI generation-time SLO threshold (milliseconds). */
    public record CliSlo(int genTimeP95Ms) {

        static final CliSlo DEFAULT = new CliSlo(3000);

        static CliSlo fromMap(Map<String, Object> m) {
            return new CliSlo(MapHelper.optionalInt(m, "gen_time_p95_ms", 3000));
        }
    }

    /**
     * Mutation-testing gate configuration.
     *
     * @param enabled      whether the mutation gate is active
     * @param threshold    minimum mutation score % (default 80)
     * @param runtimeCapMin maximum mutation-run time in minutes (default 10)
     * @param toolVersions optional tool version pins
     */
    public record MutationConfig(
            boolean enabled, int threshold, int runtimeCapMin, Map<String, Object> toolVersions) {

        static final MutationConfig DEFAULT = new MutationConfig(false, 80, 10, Map.of());

        public MutationConfig {
            toolVersions = toolVersions == null ? Map.of() : Map.copyOf(toolVersions);
        }

        static MutationConfig fromMap(Map<String, Object> m) {
            return new MutationConfig(
                    MapHelper.optionalBoolean(m, "enabled", false),
                    MapHelper.optionalInt(m, "threshold", 80),
                    MapHelper.optionalInt(m, "runtime_cap_min", 10),
                    MapHelper.optionalMap(m, "tool_versions"));
        }
    }

    /**
     * Contract-testing gate configuration.
     *
     * @param enabled        whether the contract gate is active
     * @param pact           whether Pact consumer-driven contracts are enabled
     * @param openapiBreaking whether OpenAPI breaking-change detection is enabled (default true)
     * @param protoBreaking   whether Protobuf breaking-change detection is enabled (default true)
     */
    public record ContractConfig(
            boolean enabled, boolean pact, boolean openapiBreaking, boolean protoBreaking) {

        static final ContractConfig DEFAULT = new ContractConfig(false, false, true, true);

        static ContractConfig fromMap(Map<String, Object> m) {
            return new ContractConfig(
                    MapHelper.optionalBoolean(m, "enabled", false),
                    MapHelper.optionalBoolean(m, "pact", false),
                    MapHelper.optionalBoolean(m, "openapi_breaking", true),
                    MapHelper.optionalBoolean(m, "proto_breaking", true));
        }
    }
}

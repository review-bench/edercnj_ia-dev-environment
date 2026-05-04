package dev.iadev.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Quality-gate configuration for extended test dimensions.
 *
 * <p>Parsed from the {@code quality:} top-level YAML block. When the block is absent, all
 * sub-configs default to {@code enabled=false} (safe defaults per Rule 19 — existing projects do
 * not break).
 *
 * <p>Extended by EPIC-0073 with {@link RegressionConfig} and {@link DastConfig}.
 *
 * <pre>{@code
 * quality:
 *   performance:
 *     enabled: true
 *     slo:
 *       rest: { p50_ms: 50, p95_ms: 200, p99_ms: 500, throughput_rps: 1000 }
 *   mutation:
 *     enabled: true
 *     threshold: 80
 *   contract:
 *     enabled: true
 *   regression:
 *     enabled: true
 *     mode: service
 *     scenarios-file: tests/regression/scenarios.yaml
 *   dast:
 *     enabled: true
 *     tier-pr: smoke
 *     tier-nightly: full
 *     target: local-container
 *     zap:
 *       enabled: true
 *       active-scan-policy: default
 *     nuclei:
 *       enabled: true
 *       templates-version: v9.x
 * }</pre>
 *
 * @param performance performance-testing SLO thresholds (stack-aware via {@code
 *     /x-execute-performance-tests})
 * @param mutation mutation-testing threshold (stack-aware via {@code /x-test-mutation})
 * @param contract contract-breaking-change gate (stack-aware via {@code /x-execute-contract-tests})
 * @param regression regression-shell gate (stack-aware via {@code /x-execute-shell-regression-tests})
 * @param dast dynamic application security testing gate (stack-aware via {@code
 *     /x-run-dynamic-pentest})
 * @see dev.iadev.domain.model.Governance
 */
public record QualityConfig(
        PerformanceConfig performance,
        MutationConfig mutation,
        ContractConfig contract,
        RegressionConfig regression,
        DastConfig dast) {

    public QualityConfig {
        performance = performance == null ? PerformanceConfig.DEFAULT : performance;
        mutation = mutation == null ? MutationConfig.DEFAULT : mutation;
        contract = contract == null ? ContractConfig.DEFAULT : contract;
        regression = regression == null ? RegressionConfig.DEFAULT : regression;
        dast = dast == null ? DastConfig.DEFAULT : dast;
    }

    /** All quality gates disabled — safe default when {@code quality:} block is absent. */
    public static final QualityConfig DEFAULT =
            new QualityConfig(
                    PerformanceConfig.DEFAULT,
                    MutationConfig.DEFAULT,
                    ContractConfig.DEFAULT,
                    RegressionConfig.DEFAULT,
                    DastConfig.DEFAULT);

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
                ContractConfig.fromMap(MapHelper.optionalMap(qualityMap, "contract")),
                RegressionConfig.fromMap(MapHelper.optionalMap(qualityMap, "regression")),
                DastConfig.fromMap(MapHelper.optionalMap(qualityMap, "dast")));
    }

    /**
     * Performance-testing gate configuration.
     *
     * @param enabled whether the performance gate is active
     * @param slo SLO thresholds per interface type
     * @param baselineTolerancePct tolerance % for baseline drift (default 10)
     * @param toolVersions optional tool version pins (default empty)
     */
    public record PerformanceConfig(
            boolean enabled,
            SloConfig slo,
            int baselineTolerancePct,
            Map<String, Object> toolVersions) {

        public static final PerformanceConfig DEFAULT =
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
     * @param cli CLI generation-time P95
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
     * @param enabled whether the mutation gate is active
     * @param threshold minimum mutation score % (default 80)
     * @param runtimeCapMin maximum mutation-run time in minutes (default 10)
     * @param toolVersions optional tool version pins
     */
    public record MutationConfig(
            boolean enabled, int threshold, int runtimeCapMin, Map<String, Object> toolVersions) {

        public static final MutationConfig DEFAULT = new MutationConfig(false, 80, 10, Map.of());

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
     * @param enabled whether the contract gate is active
     * @param pact whether Pact consumer-driven contracts are enabled
     * @param openapiBreaking whether OpenAPI breaking-change detection is enabled (default true)
     * @param protoBreaking whether Protobuf breaking-change detection is enabled (default true)
     */
    public record ContractConfig(
            boolean enabled, boolean pact, boolean openapiBreaking, boolean protoBreaking) {

        public static final ContractConfig DEFAULT = new ContractConfig(false, false, true, true);

        static ContractConfig fromMap(Map<String, Object> m) {
            return new ContractConfig(
                    MapHelper.optionalBoolean(m, "enabled", false),
                    MapHelper.optionalBoolean(m, "pact", false),
                    MapHelper.optionalBoolean(m, "openapi_breaking", true),
                    MapHelper.optionalBoolean(m, "proto_breaking", true));
        }
    }

    /**
     * Regression-shell gate configuration.
     *
     * @param enabled whether the regression gate is active
     * @param mode execution mode — SELF (generator validates its own output) or SERVICE (client
     *     project)
     * @param scenariosFile path to regression scenarios YAML file
     */
    public record RegressionConfig(boolean enabled, String mode, String scenariosFile) {

        public static final RegressionConfig DEFAULT =
                new RegressionConfig(false, "service", "tests/regression/scenarios.yaml");

        public RegressionConfig {
            mode = mode == null ? "service" : mode;
            scenariosFile =
                    scenariosFile == null ? "tests/regression/scenarios.yaml" : scenariosFile;
        }

        static RegressionConfig fromMap(Map<String, Object> m) {
            return new RegressionConfig(
                    MapHelper.optionalBoolean(m, "enabled", false),
                    MapHelper.optionalString(m, "mode", "service"),
                    MapHelper.optionalString(
                            m, "scenarios-file", "tests/regression/scenarios.yaml"));
        }
    }

    /**
     * Dynamic Application Security Testing (DAST) gate configuration.
     *
     * @param enabled whether the DAST gate is active
     * @param tierPr scan tier for PR pipelines (smoke = ZAP passive + Nuclei lightweight)
     * @param tierNightly scan tier for nightly pipelines (full = ZAP active + Nuclei full set)
     * @param target scan target environment — production is explicitly forbidden
     * @param zap ZAP scanner configuration
     * @param nuclei Nuclei scanner configuration
     * @param compliance compliance frameworks to check (e.g., pci, lgpd, hipaa)
     */
    public record DastConfig(
            boolean enabled,
            String tierPr,
            String tierNightly,
            String target,
            ZapConfig zap,
            NucleiConfig nuclei,
            List<String> compliance) {

        public static final DastConfig DEFAULT =
                new DastConfig(
                        false,
                        "smoke",
                        "full",
                        "local-container",
                        ZapConfig.DEFAULT,
                        NucleiConfig.DEFAULT,
                        List.of());

        public DastConfig {
            if ("production".equalsIgnoreCase(target)) {
                throw new ConfigValidationException(
                        "DAST_TARGET_PRODUCTION_FORBIDDEN: target=production is not allowed;"
                                + " use local-container, preview-env, or staging");
            }
            tierPr = tierPr == null ? "smoke" : tierPr;
            tierNightly = tierNightly == null ? "full" : tierNightly;
            target = target == null ? "local-container" : target;
            zap = zap == null ? ZapConfig.DEFAULT : zap;
            nuclei = nuclei == null ? NucleiConfig.DEFAULT : nuclei;
            compliance = compliance == null ? List.of() : List.copyOf(compliance);
        }

        static DastConfig fromMap(Map<String, Object> m) {
            return new DastConfig(
                    MapHelper.optionalBoolean(m, "enabled", false),
                    MapHelper.optionalString(m, "tier-pr", "smoke"),
                    MapHelper.optionalString(m, "tier-nightly", "full"),
                    MapHelper.optionalString(m, "target", "local-container"),
                    ZapConfig.fromMap(MapHelper.optionalMap(m, "zap")),
                    NucleiConfig.fromMap(MapHelper.optionalMap(m, "nuclei")),
                    MapHelper.optionalStringList(m, "compliance"));
        }

        /** OWASP ZAP scanner configuration. */
        public record ZapConfig(boolean enabled, String activeScanPolicy) {

            public static final ZapConfig DEFAULT = new ZapConfig(false, "default");

            public ZapConfig {
                activeScanPolicy = activeScanPolicy == null ? "default" : activeScanPolicy;
            }

            static ZapConfig fromMap(Map<String, Object> m) {
                return new ZapConfig(
                        MapHelper.optionalBoolean(m, "enabled", false),
                        MapHelper.optionalString(m, "active-scan-policy", "default"));
            }
        }

        /**
         * Nuclei scanner configuration.
         *
         * <p>Templates version must be pinned to a stable series (vN.x or vN.M.P). Values 'latest',
         * 'master', 'HEAD' are rejected ({@code NUCLEI_VERSION_UNPINNED}).
         */
        public record NucleiConfig(boolean enabled, String templatesVersion) {

            private static final java.util.regex.Pattern PINNED_VERSION =
                    java.util.regex.Pattern.compile("^v\\d+(\\.x|\\.\\d+(\\.\\d+)?)$");

            private static final Set<String> UNPINNED = Set.of("latest", "master", "head");

            public static final NucleiConfig DEFAULT = new NucleiConfig(false, "v9.x");

            public NucleiConfig {
                templatesVersion = templatesVersion == null ? "v9.x" : templatesVersion;
                if (UNPINNED.contains(templatesVersion.toLowerCase())) {
                    throw new ConfigValidationException(
                            ("NUCLEI_VERSION_UNPINNED: templates-version='%s' is not allowed;"
                                            + " use a pinned version series such as v9.x or v9.0.1")
                                    .formatted(templatesVersion));
                }
                if (!PINNED_VERSION.matcher(templatesVersion).matches()) {
                    throw new ConfigValidationException(
                            ("NUCLEI_VERSION_UNPINNED: templates-version='%s' does not match"
                                            + " required format vN.x or vN.M.P")
                                    .formatted(templatesVersion));
                }
            }

            static NucleiConfig fromMap(Map<String, Object> m) {
                return new NucleiConfig(
                        MapHelper.optionalBoolean(m, "enabled", false),
                        MapHelper.optionalString(m, "templates-version", "v9.x"));
            }
        }
    }
}

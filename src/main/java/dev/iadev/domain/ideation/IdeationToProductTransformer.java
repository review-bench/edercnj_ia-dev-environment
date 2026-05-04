package dev.iadev.domain.ideation;

import dev.iadev.domain.product.Product;
import dev.iadev.domain.product.RNFCategory;
import dev.iadev.domain.product.RNFRoot;
import java.util.ArrayList;
import java.util.List;

public final class IdeationToProductTransformer {

    public Product transform(IdeationTemplate ideation) {
        if (ideation == null) {
            throw new IllegalArgumentException("ideation must not be null");
        }
        List<RNFRoot> rnfRoots = buildRnfRoots(ideation);
        return new Product(ideation.title(), rnfRoots);
    }

    private List<RNFRoot> buildRnfRoots(IdeationTemplate ideation) {
        List<RNFRoot> roots = new ArrayList<>();
        roots.addAll(buildMandatoryRnfs(ideation));
        roots.addAll(buildOptionalRnfs());
        return roots;
    }

    private List<RNFRoot> buildMandatoryRnfs(IdeationTemplate ideation) {
        String requirements = ideation.sectionContent(IdeationSection.BUSINESS_REQUIREMENTS);
        List<RNFRoot> mandatory = new ArrayList<>();

        mandatory.add(buildPerformanceRnf(requirements));
        mandatory.add(buildScalabilityRnf(requirements));
        mandatory.add(buildReliabilityRnf(requirements));
        mandatory.add(buildSecurityRnf(requirements));
        mandatory.add(buildComplianceRnf(requirements));
        mandatory.add(buildObservabilityRnf());

        return mandatory;
    }

    private RNFRoot buildPerformanceRnf(String requirements) {
        String desc = extractLatencyRequirement(requirements);
        return new RNFRoot(RNFCategory.PERFORMANCE, desc, "Load test p99 latency measurement", true);
    }

    private RNFRoot buildScalabilityRnf(String requirements) {
        String desc = extractScalabilityRequirement(requirements);
        return new RNFRoot(RNFCategory.SCALABILITY, desc, "Load test concurrent user benchmark", true);
    }

    private RNFRoot buildReliabilityRnf(String requirements) {
        String desc = extractUptimeRequirement(requirements);
        return new RNFRoot(RNFCategory.RELIABILITY, desc, "SLO monitoring + alert validation", true);
    }

    private RNFRoot buildSecurityRnf(String requirements) {
        String desc = extractSecurityRequirement(requirements);
        return new RNFRoot(RNFCategory.SECURITY, desc, "Security audit + penetration test", true);
    }

    private RNFRoot buildComplianceRnf(String requirements) {
        String desc = extractComplianceRequirement(requirements);
        return new RNFRoot(RNFCategory.COMPLIANCE, desc, "Compliance audit report", true);
    }

    private RNFRoot buildObservabilityRnf() {
        return new RNFRoot(
                RNFCategory.OBSERVABILITY,
                "Structured logging, metrics, and distributed tracing for all operations",
                "Observability coverage review",
                true);
    }

    private List<RNFRoot> buildOptionalRnfs() {
        return List.of(
                new RNFRoot(RNFCategory.DATA_INTEGRITY,
                        "All data mutations are atomic and consistent",
                        "Integration test suite validation", false),
                new RNFRoot(RNFCategory.MAINTAINABILITY,
                        "Code coverage ≥ 95%, cyclomatic complexity ≤ 10 per method",
                        "Static analysis report", false),
                new RNFRoot(RNFCategory.PORTABILITY,
                        "Cloud-agnostic deployment: Docker + Kubernetes compatible",
                        "Multi-cloud deployment test", false),
                new RNFRoot(RNFCategory.USABILITY,
                        "CLI response time < 2s for all commands",
                        "UX benchmarking", false),
                new RNFRoot(RNFCategory.PERFORMANCE,
                        "Startup time < 5s under normal load",
                        "Startup benchmark test", true),
                new RNFRoot(RNFCategory.RELIABILITY,
                        "Graceful degradation under 2x peak load",
                        "Chaos engineering validation", true));
    }

    private String extractLatencyRequirement(String requirements) {
        return requirements.lines()
                .filter(l -> l.toLowerCase().contains("latency") || l.toLowerCase().contains("ms"))
                .map(l -> "Latency SLA: " + l.replaceFirst("^BIZ-\\d+\\s*", "").trim())
                .findFirst()
                .orElse("System response latency p99 < 500ms under normal load");
    }

    private String extractScalabilityRequirement(String requirements) {
        return requirements.lines()
                .filter(l -> l.toLowerCase().contains("user") || l.toLowerCase().contains("events")
                        || l.toLowerCase().contains("100k") || l.toLowerCase().contains("1m"))
                .map(l -> "Scalability: " + l.replaceFirst("^BIZ-\\d+\\s*", "").trim())
                .findFirst()
                .orElse("System must scale to 10K concurrent users without degradation");
    }

    private String extractUptimeRequirement(String requirements) {
        return requirements.lines()
                .filter(l -> l.toLowerCase().contains("uptime") || l.toLowerCase().contains("sla")
                        || l.toLowerCase().contains("99."))
                .map(l -> "Reliability: " + l.replaceFirst("^BIZ-\\d+\\s*", "").trim())
                .findFirst()
                .orElse("System availability ≥ 99.9% monthly (≤ 43.8 min downtime/month)");
    }

    private String extractSecurityRequirement(String requirements) {
        return requirements.lines()
                .filter(l -> l.toLowerCase().contains("encrypt") || l.toLowerCase().contains("pii")
                        || l.toLowerCase().contains("secur"))
                .map(l -> "Security: " + l.replaceFirst("^BIZ-\\d+\\s*", "").trim())
                .findFirst()
                .orElse("All PII encrypted at rest (AES-256) and in transit (TLS 1.3+)");
    }

    private String extractComplianceRequirement(String requirements) {
        return requirements.lines()
                .filter(l -> l.toLowerCase().contains("gdpr") || l.toLowerCase().contains("complian")
                        || l.toLowerCase().contains("pci") || l.toLowerCase().contains("sox"))
                .map(l -> "Compliance: " + l.replaceFirst("^BIZ-\\d+\\s*", "").trim())
                .findFirst()
                .orElse("GDPR and LGPD compliant; data residency controls enforced");
    }
}

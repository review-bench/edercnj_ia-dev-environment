package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.adapter.outbound.documentation.C4DiagramGenerator;
import dev.iadev.adapter.outbound.product.ProductArtifactWriter;
import dev.iadev.application.architecture.ArchitectureRefactoringUseCase;
import dev.iadev.application.feature.CreateEpicFromFeatureUseCase;
import dev.iadev.application.feature.CreateFeaturesOrchestrationUseCase;
import dev.iadev.application.feature.CreateStoriesFromFeatureUseCase;
import dev.iadev.application.feature.FeatureEpicSourceLoader;
import dev.iadev.application.feature.FeatureMarkdownParser;
import dev.iadev.application.feature.FeatureToStoryDecompositionUseCase;
import dev.iadev.application.product.CapabilityDecompositionUseCase;
import dev.iadev.application.product.CreateCapabilitiesOrchestrationUseCase;
import dev.iadev.application.product.CreateProductOrchestrationUseCase;
import dev.iadev.application.quality.ExecuteC4PhaseGateUseCase;
import dev.iadev.domain.architecture.C4LevelValidator;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.feature.AutoDecomposeFeatureHeuristic;
import dev.iadev.domain.feature.CapabilityToFeatureTransformer;
import dev.iadev.domain.feature.GherkinACGenerator;
import dev.iadev.domain.ideation.IdeationSection;
import dev.iadev.domain.ideation.IdeationTemplate;
import dev.iadev.domain.ideation.IdeationToProductTransformer;
import dev.iadev.domain.ideation.IdeationValidator;
import dev.iadev.domain.product.AutoDecomposeHeuristic;
import dev.iadev.domain.product.Product;
import dev.iadev.domain.product.ProductToCapabilityTransformer;
import dev.iadev.domain.product.RNFRoot;
import dev.iadev.domain.product.RNFRootValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Epic0077ProductFirstSmokeIT — Product-First lifecycle E2E")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "POSIX audit scripts")
class Epic0077ProductFirstSmokeIT {

    private static final Path AUDIT_PRODUCT_UPSTREAM = auditScript("audit-product-upstream.sh");
    private static final Path AUDIT_C4_COMPLETENESS = auditScript("audit-c4-completeness.sh");
    private static final Path AUDIT_RNF_GATES = auditScript("audit-rnf-gates.sh");
    private static final Path AUDIT_PENTEST_COVERAGE = auditScript("audit-pentest-coverage.sh");

    @Test
    void endToEnd_productFirstLifecycle_createsArtifactsAndPassesGates(@TempDir Path tmp) throws Exception {
        Path productsDir = tmp.resolve("ai/products");
        Path featuresDir = tmp.resolve("ai/features");
        Path epicsDir = tmp.resolve("ai/epics");
        Product product = createProduct(productsDir);
        createCapability(productsDir, product);
        createFeatures(featuresDir);

        Path productMarkdown = writeProductMarkdown(tmp, product);
        Path capabilityMarkdown = writeCapabilityMarkdown(tmp);
        Path featureMarkdown = writeFeatureMarkdown(tmp);

        Path epicFile = new CreateEpicFromFeatureUseCase(new FeatureEpicSourceLoader())
                .execute("0077", featureMarkdown, capabilityMarkdown, productMarkdown, epicsDir)
                .epicFile();
        List<Path> stories = new CreateStoriesFromFeatureUseCase(
                new FeatureMarkdownParser(),
                new FeatureEpicSourceLoader(),
                new FeatureToStoryDecompositionUseCase())
                .execute("0077", featureMarkdown, capabilityMarkdown, productMarkdown, epicsDir)
                .storyFiles();

        writePentestPlans(stories);
        assertDerivedArtifacts(epicFile, stories);
        assertC4PhaseGatePasses();
        assertAuditPasses(AUDIT_PRODUCT_UPSTREAM, tmp);
        assertAuditPasses(AUDIT_C4_COMPLETENESS, tmp);
        assertAuditPasses(AUDIT_RNF_GATES, tmp);
        assertAuditPasses(AUDIT_PENTEST_COVERAGE, tmp);
    }

    private Product createProduct(Path productsDir) throws IOException {
        var useCase = new CreateProductOrchestrationUseCase(
                new IdeationValidator(),
                new IdeationToProductTransformer(),
                new dev.iadev.domain.capability.CapabilityStubFactory(),
                new RNFRootValidator());
        var result = useCase.execute("product-0001", validIdeation());
        assertThat(result.successful()).isTrue();
        ProductArtifactWriter.write(result.product(), "product-0001", productsDir);
        return result.product();
    }

    private void createCapability(Path productsDir, Product product) throws IOException {
        var useCase = new CreateCapabilitiesOrchestrationUseCase(
                new CapabilityDecompositionUseCase(
                        new AutoDecomposeHeuristic(),
                        new ProductToCapabilityTransformer()));
        var result = useCase.execute("product-0001", product, List.of("auth", "access", "audit"), productsDir);
        assertThat(result.capabilityCount()).isEqualTo(3);
        assertThat(productsDir.resolve("product-0001-capability-c1.json")).exists();
    }

    private void createFeatures(Path featuresDir) throws IOException {
        Files.createDirectories(featuresDir);
        var useCase = new CreateFeaturesOrchestrationUseCase(
                new dev.iadev.application.feature.FeatureDecompositionUseCase(
                        new AutoDecomposeFeatureHeuristic(),
                        new CapabilityToFeatureTransformer()),
                new GherkinACGenerator());
        var result = 0;
        for (String capabilityId : List.of("capability-c1", "capability-c2", "capability-c3")) {
            result += useCase.execute(
                            capabilityId,
                            List.of("OAuth2 Login", "GitHub Login", "Auto Provisioning", "Session Audit"),
                            featuresDir)
                    .featuresCreated();
        }
        assertThat(result).isEqualTo(12);
        assertThat(featuresDir.resolve("capability-c1-feature-0001.json")).exists();
        assertThat(featuresDir.resolve("capability-c2-feature-0001.json")).exists();
        assertThat(featuresDir.resolve("capability-c3-feature-0001.json")).exists();
    }

    private Path writeProductMarkdown(Path root, Product product) throws IOException {
        Path file = root.resolve("product-product-first.md");
        StringBuilder content = new StringBuilder("# Product: Product-First Smoke\n\n");
        content.append("## 4. RNFs Root (Non-Functional Requirements Raiz)\n\n");
        content.append("| Categoria | Requisito | Target Mensurável | Mandatory | Verificação |\n");
        content.append("| :--- | :--- | :--- | :--- | :--- |\n");
        for (RNFRoot rnf : product.rnfRoots()) {
            if (!rnf.mandatory()) {
                continue;
            }
            content.append("| ").append(rnf.category().name()).append(" | ")
                    .append(rnf.description()).append(" | ")
                    .append(rnf.verificationMethod()).append(" | ")
                    .append(rnf.mandatory() ? "Sim" : "Não").append(" | audit |\n");
        }
        Files.writeString(file, content.toString());
        return file;
    }

    private Path writeCapabilityMarkdown(Path root) throws IOException {
        Path file = root.resolve("capability-product-first.md");
        String content = """
                # Capability: Authentication

                ## 2. RNFs Herdadas

                | Categoria | RNF Original (Produto) | no-relax? | Override Value | Justificação | Approval Status | Approver |
                | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
                | PERFORMANCE | P99 < 500ms | true | — | — | — | — |
                | RELIABILITY | 99.95%% uptime | true | — | — | — | — |
                | SECURITY | MFA obrigatório | true | — | — | — | — |
                | COMPLIANCE | LGPD Art.46 | true | — | — | — | — |
                """;
        Files.writeString(file, content);
        return file;
    }

    private Path writeFeatureMarkdown(Path root) throws IOException {
        Path file = root.resolve("feature-product-first.md");
        String content = """
                # Feature: OAuth2 Login

                **Feature ID:** feature-0001
                **Capability:** capability-c1
                **Status:** Draft

                ## 1.2 Escopo

                **In-scope:**
                - OAuth2 callback orchestration
                - Account provisioning on first login

                **Out-of-scope:**
                - Legacy password login

                ## 2. Casos de Uso

                ### UC-001: Login com Google

                | Campo | Valor |
                | :--- | :--- |
                | **Ator** | Usuário autenticado |
                | **Ação** | autenticar via Google |
                | **Benefício** | acessar a plataforma sem senha local |

                ### UC-002: Login com GitHub

                | Campo | Valor |
                | :--- | :--- |
                | **Ator** | Tech Lead |
                | **Ação** | autenticar via GitHub |
                | **Benefício** | reutilizar a identidade corporativa |

                ### UC-003: Provisionamento automático

                | Campo | Valor |
                | :--- | :--- |
                | **Ator** | Novo usuário |
                | **Ação** | criar conta local no primeiro login |
                | **Benefício** | acessar o sistema sem intervenção manual |

                ## 5. Acceptance Criteria Detalhados

                ```gherkin
                Cenário: Login com Google retorna sessão ativa
                Cenário: Login com GitHub retorna sessão ativa
                Cenário: Primeiro login cria conta local
                ```
                """;
        Files.writeString(file, content);
        return file;
    }

    private void writePentestPlans(List<Path> stories) throws IOException {
        for (Path story : stories) {
            Path plan = story.getParent().resolve("pentest-plan-" + story.getFileName());
            Files.writeString(plan, "# Pentest plan for " + story.getFileName());
        }
    }

    private void assertDerivedArtifacts(Path epicFile, List<Path> stories) throws IOException {
        assertThat(epicFile).exists();
        assertThat(Files.readString(epicFile)).contains("## C4 Context").contains("## C4 Code");
        assertThat(stories).hasSize(3);
        assertThat(Files.readString(stories.get(0)))
                .contains("## 6. Tasks")
                .contains("TASK-0077-0077-001")
                .contains("## C4 Context")
                .contains("## C4 Code");
    }

    private void assertC4PhaseGatePasses() {
        var planner = new ArchitectureRefactoringUseCase(new C4DiagramGenerator(), new C4LevelValidator());
        var featureModel = planner.planFeature("feature-0001", C4OutputFormat.MERMAID);
        var result = new ExecuteC4PhaseGateUseCase().execute(
                List.of(
                        featureModel.contextDiagram(),
                        featureModel.containerDiagram(),
                        featureModel.componentDiagram()),
                null,
                null);
        assertThat(result.passed()).isTrue();
    }

    private void assertAuditPasses(Path script, Path workingDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("bash", script.toString());
        pb.directory(workingDir.toFile());
        ProcessResult result = waitProcess(pb);
        assertThat(result.exitCode).as(script.getFileName() + " stderr=" + result.stderr).isEqualTo(0);
    }

    private static ProcessResult waitProcess(ProcessBuilder pb) throws Exception {
        Process process = pb.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Process timed out: " + pb.command());
        }
        return new ProcessResult(
                process.exitValue(),
                new String(process.getInputStream().readAllBytes()),
                new String(process.getErrorStream().readAllBytes()));
    }

    private static IdeationTemplate validIdeation() {
        return IdeationTemplate.builder()
                .title("Product First Platform")
                .sections(Map.of(
                        IdeationSection.VISION_AND_SCOPE, "vision",
                        IdeationSection.STAKEHOLDERS, "stakeholders",
                        IdeationSection.BUSINESS_REQUIREMENTS,
                        """
                                BIZ-001 process 1M events/sec
                                BIZ-002 99.99%% uptime SLA
                                BIZ-003 data encrypted at rest AES-256
                                BIZ-004 GDPR and LGPD compliant
                                BIZ-005 p99 latency < 100ms
                                """,
                        IdeationSection.CONSTRAINTS, "constraints",
                        IdeationSection.SUCCESS_CRITERIA, "success",
                        IdeationSection.RISKS, "risks",
                        IdeationSection.ROADMAP, "roadmap"))
                .build();
    }

    private static Path auditScript(String name) {
        return repoRoot().resolve("src/main/resources/targets/claude/scripts").resolve(name);
    }

    private static Path repoRoot() {
        return Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {
    }
}

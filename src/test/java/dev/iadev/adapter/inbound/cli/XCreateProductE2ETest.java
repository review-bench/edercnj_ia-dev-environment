package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.adapter.outbound.product.CapabilityStubWriter;
import dev.iadev.adapter.outbound.product.ProductArtifactWriter;
import dev.iadev.application.product.CreateProductOrchestrationUseCase;
import dev.iadev.application.product.CreateProductResult;
import dev.iadev.domain.capability.CapabilityStubFactory;
import dev.iadev.domain.ideation.IdeationSection;
import dev.iadev.domain.ideation.IdeationTemplate;
import dev.iadev.domain.ideation.IdeationToProductTransformer;
import dev.iadev.domain.ideation.IdeationValidator;
import dev.iadev.domain.product.RNFRootValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("XCreateProduct E2E Smoke")
class XCreateProductE2ETest {

    @TempDir
    Path outputDir;

    private static IdeationTemplate validIdeation() {
        return IdeationTemplate.builder()
                .title("Analytics Platform")
                .sections(Map.of(
                        IdeationSection.VISION_AND_SCOPE, "Real-time analytics for enterprises",
                        IdeationSection.STAKEHOLDERS, "Data analysts, engineers",
                        IdeationSection.BUSINESS_REQUIREMENTS,
                                "BIZ-001 process 1M events/sec\n"
                                + "BIZ-002 99.99% uptime SLA\n"
                                + "BIZ-003 data encrypted at rest AES-256\n"
                                + "BIZ-004 GDPR and LGPD compliant\n"
                                + "BIZ-005 p99 latency < 100ms",
                        IdeationSection.CONSTRAINTS, "On-prem first",
                        IdeationSection.SUCCESS_CRITERIA, "50K events/sec MVP",
                        IdeationSection.RISKS, "Vendor lock-in",
                        IdeationSection.ROADMAP, "Q1: ingest, Q2: query"))
                .build();
    }

    @Test
    @DisplayName("fullPipeline_validIdeation_writesProductAndCapabilityArtifacts")
    void fullPipeline_validIdeation_writesProductAndCapabilityArtifacts() throws IOException {
        var useCase = new CreateProductOrchestrationUseCase(
                new IdeationValidator(), new IdeationToProductTransformer(),
                new CapabilityStubFactory(), new RNFRootValidator());

        CreateProductResult result = useCase.execute("product-0001", validIdeation());

        assertThat(result.successful()).isTrue();

        var productWrite = ProductArtifactWriter.write(result.product(), "product-0001", outputDir);
        var capabilityWrite = CapabilityStubWriter.write(result.c1Stub(), outputDir);

        assertThat(productWrite.path()).exists();
        assertThat(capabilityWrite.path()).exists();
        assertThat(productWrite.skipped()).isFalse();
        assertThat(capabilityWrite.skipped()).isFalse();
    }

    @Test
    @DisplayName("fullPipeline_rerun_secondExecutionSkipsWrite")
    void fullPipeline_rerun_secondExecutionSkipsWrite() throws IOException {
        var useCase = new CreateProductOrchestrationUseCase(
                new IdeationValidator(), new IdeationToProductTransformer(),
                new CapabilityStubFactory(), new RNFRootValidator());

        CreateProductResult result = useCase.execute("product-0001", validIdeation());
        ProductArtifactWriter.write(result.product(), "product-0001", outputDir);
        CapabilityStubWriter.write(result.c1Stub(), outputDir);

        CreateProductResult result2 = useCase.execute("product-0001", validIdeation());
        var productWrite2 = ProductArtifactWriter.write(result2.product(), "product-0001", outputDir);
        var capabilityWrite2 = CapabilityStubWriter.write(result2.c1Stub(), outputDir);

        assertThat(productWrite2.skipped()).isTrue();
        assertThat(capabilityWrite2.skipped()).isTrue();
    }

    @Test
    @DisplayName("fullPipeline_productArtifactContainsExpectedRnfRoots")
    void fullPipeline_productArtifactContainsExpectedRnfRoots() throws IOException {
        var useCase = new CreateProductOrchestrationUseCase(
                new IdeationValidator(), new IdeationToProductTransformer(),
                new CapabilityStubFactory(), new RNFRootValidator());

        CreateProductResult result = useCase.execute("product-0001", validIdeation());
        ProductArtifactWriter.write(result.product(), "product-0001", outputDir);

        Path artifact = outputDir.resolve("product-0001-product.json");
        String content = Files.readString(artifact);
        assertThat(content).contains("PERFORMANCE");
        assertThat(content).contains("RELIABILITY");
        assertThat(content).contains("SECURITY");
        assertThat(content).contains("COMPLIANCE");
    }

    @Test
    @DisplayName("fullPipeline_invalidIdeation_noArtifactsWritten")
    void fullPipeline_invalidIdeation_noArtifactsWritten() {
        var useCase = new CreateProductOrchestrationUseCase(
                new IdeationValidator(), new IdeationToProductTransformer(),
                new CapabilityStubFactory(), new RNFRootValidator());

        IdeationTemplate invalid = IdeationTemplate.builder()
                .title("Broken")
                .sections(Map.of(IdeationSection.VISION_AND_SCOPE, "too short"))
                .build();

        CreateProductResult result = useCase.execute("product-0001", invalid);

        assertThat(result.successful()).isFalse();
        assertThat(outputDir.toFile().listFiles()).isEmpty();
    }
}

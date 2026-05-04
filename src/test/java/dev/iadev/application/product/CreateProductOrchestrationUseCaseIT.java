package dev.iadev.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.capability.CapabilityStubFactory;
import dev.iadev.domain.ideation.IdeationSection;
import dev.iadev.domain.ideation.IdeationTemplate;
import dev.iadev.domain.ideation.IdeationToProductTransformer;
import dev.iadev.domain.ideation.IdeationValidationResult;
import dev.iadev.domain.ideation.IdeationValidator;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CreateProductOrchestrationUseCase — integration tests")
class CreateProductOrchestrationUseCaseIT {

    private final IdeationValidator validator = new IdeationValidator();
    private final IdeationToProductTransformer transformer = new IdeationToProductTransformer();
    private final CapabilityStubFactory capabilityStubFactory = new CapabilityStubFactory();
    private final CreateProductOrchestrationUseCase useCase =
            new CreateProductOrchestrationUseCase(validator, transformer, capabilityStubFactory);

    private static IdeationTemplate validIdeation() {
        return IdeationTemplate.builder()
                .title("Analytics Platform")
                .sections(Map.of(
                        IdeationSection.VISION_AND_SCOPE, "Real-time analytics for enterprises",
                        IdeationSection.STAKEHOLDERS, "Data analysts, engineers",
                        IdeationSection.BUSINESS_REQUIREMENTS,
                                "BIZ-001 system must process 1M events/sec\n"
                                + "BIZ-002 99.99% uptime SLA\n"
                                + "BIZ-003 data encrypted at rest\n"
                                + "BIZ-004 GDPR compliant\n"
                                + "BIZ-005 p99 query latency < 100ms",
                        IdeationSection.CONSTRAINTS, "On-prem first, cloud optional",
                        IdeationSection.SUCCESS_CRITERIA, "50K events/sec in MVP",
                        IdeationSection.RISKS, "Vendor lock-in, data migration complexity",
                        IdeationSection.ROADMAP, "Q1: ingest, Q2: query engine"))
                .build();
    }

    private static IdeationTemplate invalidIdeation() {
        return IdeationTemplate.builder()
                .title("Broken")
                .sections(Map.of(
                        IdeationSection.VISION_AND_SCOPE, "X"))
                .build();
    }

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        void execute_withValidIdeation_returnsSuccessResult() {
            var result = useCase.execute("product-0001", validIdeation());
            assertThat(result.successful()).isTrue();
        }

        @Test
        void execute_withValidIdeation_resultContainsProduct() {
            var result = useCase.execute("product-0001", validIdeation());
            assertThat(result.product()).isNotNull();
            assertThat(result.product().name()).isEqualTo("Analytics Platform");
        }

        @Test
        void execute_withValidIdeation_resultContainsTwelveOrMoreRnfRoots() {
            var result = useCase.execute("product-0001", validIdeation());
            assertThat(result.product().rnfRoots()).hasSizeGreaterThanOrEqualTo(12);
        }

        @Test
        void execute_withValidIdeation_resultContainsC1CapabilityStub() {
            var result = useCase.execute("product-0001", validIdeation());
            assertThat(result.c1Stub()).isNotNull();
            assertThat(result.c1Stub().capabilityId()).isEqualTo("capability-c1");
            assertThat(result.c1Stub().productId()).isEqualTo("product-0001");
        }

        @Test
        void execute_withValidIdeation_executionTimeMsIsPositive() {
            var result = useCase.execute("product-0001", validIdeation());
            assertThat(result.executionTimeMs()).isGreaterThanOrEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Validation failure")
    class ValidationFailure {

        @Test
        void execute_withInvalidIdeation_returnsFailureResult() {
            var result = useCase.execute("product-0001", invalidIdeation());
            assertThat(result.successful()).isFalse();
        }

        @Test
        void execute_withInvalidIdeation_resultContainsValidationErrors() {
            var result = useCase.execute("product-0001", invalidIdeation());
            assertThat(result.validationErrors()).isNotEmpty();
        }

        @Test
        void execute_withInvalidIdeation_productIsNull() {
            var result = useCase.execute("product-0001", invalidIdeation());
            assertThat(result.product()).isNull();
        }
    }

    @Nested
    @DisplayName("Null checks")
    class NullChecks {

        @Test
        void execute_withNullIdeation_throwsIllegalArgument() {
            assertThatThrownBy(() -> useCase.execute("product-0001", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void execute_withNullProductId_throwsIllegalArgument() {
            assertThatThrownBy(() -> useCase.execute(null, validIdeation()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}

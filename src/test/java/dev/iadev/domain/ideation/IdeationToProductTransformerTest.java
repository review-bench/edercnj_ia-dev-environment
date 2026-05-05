package dev.iadev.domain.ideation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.product.RNFCategory;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("IdeationToProductTransformer — unit tests")
class IdeationToProductTransformerTest {

    private final IdeationToProductTransformer transformer = new IdeationToProductTransformer();

    private static IdeationTemplate validIdeation() {
        return IdeationTemplate.builder()
                .title("My SaaS Product")
                .sections(
                        Map.of(
                                IdeationSection.VISION_AND_SCOPE, "A platform for X",
                                IdeationSection.STAKEHOLDERS, "PM, Dev, QA",
                                IdeationSection.BUSINESS_REQUIREMENTS,
                                        "BIZ-001 system must handle 100K users\n"
                                                + "BIZ-002 encrypt all PII at rest\n"
                                                + "BIZ-003 99.9% uptime guarantee\n"
                                                + "BIZ-004 latency p99 < 200ms\n"
                                                + "BIZ-005 GDPR compliance mandatory",
                                IdeationSection.CONSTRAINTS, "Budget limited",
                                IdeationSection.SUCCESS_CRITERIA, "10K MAU in 6 months",
                                IdeationSection.RISKS, "Risk: integration complexity",
                                IdeationSection.ROADMAP, "Q1: MVP, Q2: GA"))
                .build();
    }

    @Nested
    @DisplayName("Null input")
    class NullInput {

        @Test
        void transform_withNullIdeation_throwsIllegalArgument() {
            assertThatThrownBy(() -> transformer.transform(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Product name")
    class ProductName {

        @Test
        void transform_setsProductNameFromIdeationTitle() {
            var product = transformer.transform(validIdeation());
            assertThat(product.name()).isEqualTo("My SaaS Product");
        }
    }

    @Nested
    @DisplayName("RNF Root generation")
    class RnfRoots {

        @Test
        void transform_producesAtLeastTwelveRnfRoots() {
            var product = transformer.transform(validIdeation());
            assertThat(product.rnfRoots()).hasSizeGreaterThanOrEqualTo(12);
        }

        @Test
        void transform_includesMandatoryRnfCategories() {
            var product = transformer.transform(validIdeation());
            var categories = product.rnfRoots().stream().map(rnf -> rnf.category()).toList();
            assertThat(categories)
                    .contains(
                            RNFCategory.PERFORMANCE,
                            RNFCategory.SCALABILITY,
                            RNFCategory.SECURITY,
                            RNFCategory.RELIABILITY,
                            RNFCategory.COMPLIANCE,
                            RNFCategory.OBSERVABILITY);
        }

        @Test
        void transform_mandatoryRnfsHaveMandatoryFlag() {
            var product = transformer.transform(validIdeation());
            product.rnfRoots().stream()
                    .filter(rnf -> rnf.category().isMandatory())
                    .forEach(rnf -> assertThat(rnf.mandatory()).isTrue());
        }

        @Test
        void transform_allRnfsHaveNonBlankDescription() {
            var product = transformer.transform(validIdeation());
            product.rnfRoots().forEach(rnf -> assertThat(rnf.description()).isNotBlank());
        }

        @Test
        void transform_allRnfsHaveVerificationMethod() {
            var product = transformer.transform(validIdeation());
            product.rnfRoots().forEach(rnf -> assertThat(rnf.verificationMethod()).isNotBlank());
        }
    }

    @Nested
    @DisplayName("Business requirement mapping")
    class BizRequirementMapping {

        @Test
        void transform_withScalabilityRequirement_includesScalabilityRnf() {
            var product = transformer.transform(validIdeation());
            var scalabilityRnf =
                    product.rnfRoots().stream()
                            .filter(rnf -> rnf.category() == RNFCategory.SCALABILITY)
                            .findFirst();
            assertThat(scalabilityRnf).isPresent();
            assertThat(scalabilityRnf.get().description()).containsIgnoringCase("100K");
        }

        @Test
        void transform_withSecurityRequirement_includesSecurityRnf() {
            var product = transformer.transform(validIdeation());
            var securityRnf =
                    product.rnfRoots().stream()
                            .filter(rnf -> rnf.category() == RNFCategory.SECURITY)
                            .findFirst();
            assertThat(securityRnf).isPresent();
            assertThat(securityRnf.get().description()).containsIgnoringCase("PII");
        }
    }
}

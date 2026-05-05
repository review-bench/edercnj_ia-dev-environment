package dev.iadev.application.ideation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.ideation.IdeationSection;
import dev.iadev.domain.ideation.IdeationTemplate;
import dev.iadev.domain.ideation.IdeationValidationResult;
import dev.iadev.domain.ideation.IdeationValidator;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PromoteIdeationOrchestrationUseCaseTest {

    private static final IdeationValidator VALIDATOR = new IdeationValidator();

    private PromoteIdeationOrchestrationUseCase useCase() {
        return new PromoteIdeationOrchestrationUseCase(VALIDATOR);
    }

    private IdeationTemplate validTemplate() {
        String bizRequirements =
                """
                BIZ-001 requirement one
                BIZ-002 requirement two
                BIZ-003 requirement three
                BIZ-004 requirement four
                BIZ-005 requirement five
                """;
        return IdeationTemplate.builder()
                .title("My Feature Idea")
                .sections(
                        Map.of(
                                IdeationSection.VISION_AND_SCOPE, "vision content",
                                IdeationSection.STAKEHOLDERS, "stakeholders content",
                                IdeationSection.BUSINESS_REQUIREMENTS, bizRequirements,
                                IdeationSection.CONSTRAINTS, "constraints content",
                                IdeationSection.SUCCESS_CRITERIA, "success criteria content",
                                IdeationSection.RISKS, "risks content",
                                IdeationSection.ROADMAP, "roadmap content"))
                .build();
    }

    @Test
    void constructor_nullValidator_throwsIllegalArgument() {
        assertThatThrownBy(() -> new PromoteIdeationOrchestrationUseCase(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_nullTemplate_returnsFailure() {
        IdeationValidationResult result = useCase().validate(null);
        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void validate_validTemplate_returnsPassed() {
        IdeationValidationResult result = useCase().validate(validTemplate());
        assertThat(result.passed()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void validate_missingTitle_returnsFailure() {
        IdeationTemplate noTitle =
                IdeationTemplate.builder()
                        .title("")
                        .sections(Map.of(IdeationSection.VISION_AND_SCOPE, "content"))
                        .build();
        IdeationValidationResult result = useCase().validate(noTitle);
        assertThat(result.passed()).isFalse();
    }

    @Test
    void promote_validTemplate_explicitId_returnsSuccessWithRequestedId() {
        IdeationPromotionResult result = useCase().promote(validTemplate(), "ideation-0042", 1);
        assertThat(result.success()).isTrue();
        assertThat(result.resolvedId()).isEqualTo("ideation-0042");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void promote_validTemplate_noId_autoAssignsFromSequence() {
        IdeationPromotionResult result = useCase().promote(validTemplate(), null, 7);
        assertThat(result.success()).isTrue();
        assertThat(result.resolvedId()).isEqualTo("ideation-0007");
    }

    @Test
    void promote_validTemplate_blankId_autoAssignsFromSequence() {
        IdeationPromotionResult result = useCase().promote(validTemplate(), "  ", 3);
        assertThat(result.success()).isTrue();
        assertThat(result.resolvedId()).isEqualTo("ideation-0003");
    }

    @Test
    void promote_invalidTemplate_returnsFailure() {
        IdeationTemplate empty = IdeationTemplate.builder().title("").build();
        IdeationPromotionResult result = useCase().promote(empty, null, 1);
        assertThat(result.success()).isFalse();
        assertThat(result.resolvedId()).isNull();
        assertThat(result.failureReason()).isNotBlank();
    }

    @Test
    void promote_nullTemplate_returnsFailure() {
        IdeationPromotionResult result = useCase().promote(null, null, 1);
        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isNotBlank();
    }

    @Test
    void promote_sequenceZeroPaddedToFourDigits() {
        IdeationPromotionResult result = useCase().promote(validTemplate(), null, 1);
        assertThat(result.resolvedId()).isEqualTo("ideation-0001");

        IdeationPromotionResult result2 = useCase().promote(validTemplate(), null, 100);
        assertThat(result2.resolvedId()).isEqualTo("ideation-0100");
    }
}

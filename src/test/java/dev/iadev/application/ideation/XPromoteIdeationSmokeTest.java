package dev.iadev.application.ideation;

import dev.iadev.domain.ideation.IdeationSection;
import dev.iadev.domain.ideation.IdeationTemplate;
import dev.iadev.domain.ideation.IdeationValidationResult;
import dev.iadev.domain.ideation.IdeationValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("x-promote-ideation smoke — CLI + orchestration round-trip")
class XPromoteIdeationSmokeTest {

    private static IdeationTemplate minimalValidTemplate() {
        String biz = """
                BIZ-001 req one
                BIZ-002 req two
                BIZ-003 req three
                BIZ-004 req four
                BIZ-005 req five
                """;
        return IdeationTemplate.builder()
                .title("Smoke Feature Idea")
                .sections(Map.of(
                        IdeationSection.VISION_AND_SCOPE, "vision",
                        IdeationSection.STAKEHOLDERS, "stakeholders",
                        IdeationSection.BUSINESS_REQUIREMENTS, biz,
                        IdeationSection.CONSTRAINTS, "constraints",
                        IdeationSection.SUCCESS_CRITERIA, "success",
                        IdeationSection.RISKS, "risks",
                        IdeationSection.ROADMAP, "roadmap"))
                .build();
    }

    @Test
    @DisplayName("CLI --from-stdin --validate exits 0 for valid input")
    void cli_fromStdin_validate_exits0() {
        int exit = new CommandLine(new dev.iadev.adapter.inbound.cli.XPromoteIdeationCommand())
                .execute("--from-stdin", "--validate");
        assertThat(exit).isEqualTo(0);
    }

    @Test
    @DisplayName("CLI --ideation-id format validated by command")
    void cli_invalidIdeationId_exits1() {
        int exit = new CommandLine(new dev.iadev.adapter.inbound.cli.XPromoteIdeationCommand())
                .execute("--from-stdin", "--ideation-id", "bad-id");
        assertThat(exit).isEqualTo(1);
    }

    @Test
    @DisplayName("CLI --ideation-id ideation-0001 accepted")
    void cli_validIdeationId_exits0() {
        int exit = new CommandLine(new dev.iadev.adapter.inbound.cli.XPromoteIdeationCommand())
                .execute("--from-stdin", "--ideation-id", "ideation-0001");
        assertThat(exit).isEqualTo(0);
    }

    @Test
    @DisplayName("Orchestration use case: valid template with explicit id promotes successfully")
    void orchestration_validTemplate_explicitId_succeeds() {
        PromoteIdeationOrchestrationUseCase useCase =
                new PromoteIdeationOrchestrationUseCase(new IdeationValidator());

        IdeationPromotionResult result = useCase.promote(minimalValidTemplate(), "ideation-0001", 1);

        assertThat(result.success()).isTrue();
        assertThat(result.resolvedId()).isEqualTo("ideation-0001");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    @DisplayName("Orchestration use case: valid template with auto-sequence assigns ideation-NNNN")
    void orchestration_validTemplate_autoSequence_assignsPaddedId() {
        PromoteIdeationOrchestrationUseCase useCase =
                new PromoteIdeationOrchestrationUseCase(new IdeationValidator());

        IdeationPromotionResult result = useCase.promote(minimalValidTemplate(), null, 42);

        assertThat(result.success()).isTrue();
        assertThat(result.resolvedId()).isEqualTo("ideation-0042");
    }

    @Test
    @DisplayName("Orchestration use case: invalid template (empty) is rejected with failure reason")
    void orchestration_invalidTemplate_returnsFailureWithReason() {
        PromoteIdeationOrchestrationUseCase useCase =
                new PromoteIdeationOrchestrationUseCase(new IdeationValidator());

        IdeationTemplate broken = IdeationTemplate.builder().title("").build();
        IdeationPromotionResult result = useCase.promote(broken, null, 1);

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isNotBlank();
        assertThat(result.resolvedId()).isNull();
    }

    @Test
    @DisplayName("Domain: IdeationValidator.validate accepts fully populated template")
    void domain_validator_fullyPopulatedTemplate_passes() {
        IdeationValidationResult result = new IdeationValidator().validate(minimalValidTemplate());
        assertThat(result.passed()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("Domain: IdeationValidator.validate rejects template missing sections")
    void domain_validator_missingSections_fails() {
        IdeationTemplate incomplete = IdeationTemplate.builder().title("Incomplete").build();
        IdeationValidationResult result = new IdeationValidator().validate(incomplete);
        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }
}

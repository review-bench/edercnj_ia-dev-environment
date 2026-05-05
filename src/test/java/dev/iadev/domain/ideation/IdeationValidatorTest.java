package dev.iadev.domain.ideation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class IdeationValidatorTest {

    private final IdeationValidator validator = new IdeationValidator();

    @Test
    void completeIdeation_allSections_passes() {
        IdeationTemplate ideation = completeIdeation();

        IdeationValidationResult result = validator.validate(ideation);

        assertThat(result.passed()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void missingSection5_reportsSpecificError() {
        IdeationTemplate ideation =
                IdeationTemplate.builder()
                        .title("Test Ideation")
                        .sections(sectionsWithout(IdeationSection.SUCCESS_CRITERIA))
                        .build();

        IdeationValidationResult result = validator.validate(ideation);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors())
                .anyMatch(e -> e.contains("Section 5 missing: SUCCESS_CRITERIA"));
    }

    @Test
    void emptyTitle_reportsError() {
        IdeationTemplate ideation =
                IdeationTemplate.builder().title("").sections(allSections()).build();

        IdeationValidationResult result = validator.validate(ideation);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("title must not be blank"));
    }

    @Test
    void belowMinimumRequirements_failsCount() {
        Map<IdeationSection, String> sections = allSections();
        sections.put(IdeationSection.BUSINESS_REQUIREMENTS, "BIZ-001: req1\nBIZ-002: req2");

        IdeationTemplate ideation =
                IdeationTemplate.builder()
                        .title("Short Requirements Ideation")
                        .sections(sections)
                        .build();

        IdeationValidationResult result = validator.validate(ideation);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("minimum 5 requirements"));
    }

    @Test
    void largeStakeholderList_stillValid() {
        Map<IdeationSection, String> sections = allSections();
        StringBuilder stakeholders = new StringBuilder();
        for (int i = 1; i <= 105; i++) {
            stakeholders
                    .append("| Stakeholder ")
                    .append(i)
                    .append(" | Role | Decisions | Daily |\n");
        }
        sections.put(IdeationSection.STAKEHOLDERS, stakeholders.toString());

        IdeationTemplate ideation =
                IdeationTemplate.builder()
                        .title("Large Stakeholder Ideation")
                        .sections(sections)
                        .build();

        IdeationValidationResult result = validator.validate(ideation);

        assertThat(result.passed()).isTrue();
    }

    @Test
    void allSectionsPresent_noErrors() {
        IdeationTemplate ideation = completeIdeation();

        IdeationValidationResult result = validator.validate(ideation);

        assertThat(result.errors()).isEmpty();
        assertThat(IdeationSection.values()).hasSize(7);
    }

    @Test
    void multipleViolations_reportsAll() {
        IdeationTemplate ideation =
                IdeationTemplate.builder()
                        .title("")
                        .sections(sectionsWithout(IdeationSection.RISKS))
                        .build();

        IdeationValidationResult result = validator.validate(ideation);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.errors()).anyMatch(e -> e.contains("title must not be blank"));
        assertThat(result.errors()).anyMatch(e -> e.contains("Section 6 missing: RISKS"));
    }

    private IdeationTemplate completeIdeation() {
        return IdeationTemplate.builder()
                .title("Complete Test Ideation")
                .sections(allSections())
                .build();
    }

    private Map<IdeationSection, String> allSections() {
        java.util.EnumMap<IdeationSection, String> sections =
                new java.util.EnumMap<>(IdeationSection.class);
        sections.put(IdeationSection.VISION_AND_SCOPE, "Vision content for testing purposes");
        sections.put(IdeationSection.STAKEHOLDERS, "Stakeholder list for testing");
        sections.put(
                IdeationSection.BUSINESS_REQUIREMENTS,
                "BIZ-001: req one\nBIZ-002: req two\nBIZ-003: req three\nBIZ-004: req four\nBIZ-005: req five");
        sections.put(IdeationSection.CONSTRAINTS, "Constraints and assumptions content");
        sections.put(IdeationSection.SUCCESS_CRITERIA, "KPIs and success metrics");
        sections.put(IdeationSection.RISKS, "Risk mitigation plan");
        sections.put(IdeationSection.ROADMAP, "Phase 1: MVP in 8 weeks");
        return sections;
    }

    private Map<IdeationSection, String> sectionsWithout(IdeationSection excluded) {
        Map<IdeationSection, String> sections = allSections();
        sections.remove(excluded);
        return sections;
    }
}

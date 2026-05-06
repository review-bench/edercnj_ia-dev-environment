package dev.iadev.domain.ideation;

import java.util.ArrayList;
import java.util.List;

public final class IdeationValidator {

    private static final int MIN_BUSINESS_REQUIREMENTS = 5;

    public IdeationValidationResult validate(IdeationTemplate ideation) {
        List<String> errors = new ArrayList<>();
        validateTitle(ideation, errors);
        validateRequiredSections(ideation, errors);
        validateBusinessRequirementsCount(ideation, errors);
        if (errors.isEmpty()) {
            return IdeationValidationResult.success();
        }
        return IdeationValidationResult.failure(errors);
    }

    private void validateTitle(IdeationTemplate ideation, List<String> errors) {
        if (ideation.title() == null || ideation.title().isBlank()) {
            errors.add("Ideation title must not be blank");
        }
    }

    private void validateRequiredSections(IdeationTemplate ideation, List<String> errors) {
        for (IdeationSection section : IdeationSection.values()) {
            if (!ideation.hasSection(section)) {
                errors.add("Section " + section.number() + " missing: " + section.name());
            }
        }
    }

    private void validateBusinessRequirementsCount(IdeationTemplate ideation, List<String> errors) {
        if (!ideation.hasSection(IdeationSection.BUSINESS_REQUIREMENTS)) {
            return;
        }
        String content = ideation.sectionContent(IdeationSection.BUSINESS_REQUIREMENTS);
        long count = content.lines().filter(line -> line.startsWith("BIZ-")).count();
        if (count < MIN_BUSINESS_REQUIREMENTS) {
            errors.add(
                    "Section 3 (BUSINESS_REQUIREMENTS): minimum 5 requirements required, found "
                            + count);
        }
    }
}

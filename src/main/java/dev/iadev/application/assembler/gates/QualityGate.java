package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * Contributes quality-gate skills based on {@code quality.*} config (EPIC-0072).
 *
 * <p>Adds {@code x-execute-performance-tests} when {@code quality.performance.enabled=true}, {@code
 * x-execute-mutation-tests} when {@code quality.mutation.enabled=true}, and {@code x-execute-contract-tests} when
 * {@code quality.contract.enabled=true} (EPIC-0072).
 */
public final class QualityGate implements SkillGateEvaluator {

    @Override
    public List<String> evaluate(ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        if (config.quality().performance().enabled()) {
            skills.add("x-execute-performance-tests");
        }
        if (config.quality().mutation().enabled()) {
            skills.add("x-execute-mutation-tests");
        }
        if (config.quality().contract().enabled()) {
            skills.add("x-execute-contract-tests");
        }
        return skills;
    }
}

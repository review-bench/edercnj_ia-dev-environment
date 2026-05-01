package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * Contributes quality-gate skills based on {@code quality.*} config (EPIC-0072).
 *
 * <p>Adds {@code x-test-performance} when {@code quality.performance.enabled=true},
 * {@code x-test-mutation} when {@code quality.mutation.enabled=true}, and
 * {@code x-test-contract-quality} when {@code quality.contract.enabled=true}.
 */
public final class QualityGate implements SkillGateEvaluator {

    @Override
    public List<String> evaluate(ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        if (config.quality().performance().enabled()) {
            skills.add("x-test-performance");
        }
        if (config.quality().mutation().enabled()) {
            skills.add("x-test-mutation");
        }
        if (config.quality().contract().enabled()) {
            skills.add("x-test-contract-quality");
        }
        return skills;
    }
}

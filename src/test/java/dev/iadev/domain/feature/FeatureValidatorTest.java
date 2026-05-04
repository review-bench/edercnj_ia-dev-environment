package dev.iadev.domain.feature;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureValidatorTest {

    private final FeatureValidator validator = new FeatureValidator();

    @Test
    void noUseCases_fails() {
        Feature feature = new Feature("feat-001", "cap-auth", Collections.emptyList(),
                List.of(new AcceptanceCriterion("Given X When Y Then Z")));

        FeatureValidationResult result = validator.validate(feature);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("use cases"));
    }

    @Test
    void oneUseCase_belowMinimum_fails() {
        List<UseCase> useCases = List.of(new UseCase("Dev", "I want to login", "so that I access"));
        Feature feature = new Feature("feat-001", "cap-auth", useCases,
                List.of(new AcceptanceCriterion("Given X When Y Then Z")));

        FeatureValidationResult result = validator.validate(feature);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("use cases"));
    }

    @Test
    void threeUseCases_noAcceptanceCriteria_fails() {
        List<UseCase> useCases = buildUseCases(3);
        Feature feature = new Feature("feat-001", "cap-auth", useCases, Collections.emptyList());

        FeatureValidationResult result = validator.validate(feature);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("acceptance criteria"));
    }

    @Test
    void threeUseCases_tenAcceptanceCriteria_passes() {
        List<UseCase> useCases = buildUseCases(3);
        List<AcceptanceCriterion> acs = buildAcs(10);
        Feature feature = new Feature("feat-001", "cap-auth", useCases, acs);

        FeatureValidationResult result = validator.validate(feature);

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void nineUseCases_exceedsMaximum_fails() {
        List<UseCase> useCases = buildUseCases(9);
        List<AcceptanceCriterion> acs = buildAcs(10);
        Feature feature = new Feature("feat-001", "cap-auth", useCases, acs);

        FeatureValidationResult result = validator.validate(feature);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("use cases"));
    }

    private List<UseCase> buildUseCases(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new UseCase("Actor " + i, "I want to do " + i, "so that " + i))
                .toList();
    }

    private List<AcceptanceCriterion> buildAcs(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new AcceptanceCriterion("Given state " + i + " When action " + i + " Then result " + i))
                .toList();
    }
}

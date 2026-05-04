package dev.iadev.application.feature;

import dev.iadev.domain.feature.AcceptanceCriterion;
import dev.iadev.domain.feature.Feature;
import dev.iadev.domain.feature.UseCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureStoryDecompositionIT {

    private final FeatureToStoryDecompositionUseCase useCase = new FeatureToStoryDecompositionUseCase();

    @Test
    void threeUseCases_producesThreeStories() {
        Feature feature = buildFeature(3);

        List<StoryProposal> stories = useCase.decompose(feature);

        assertThat(stories).hasSize(3);
    }

    @Test
    void useCasesInheritFeatureAcceptanceCriteria() {
        Feature feature = buildFeature(3);

        List<StoryProposal> stories = useCase.decompose(feature);

        stories.forEach(story ->
                assertThat(story.inheritedAcs()).isEqualTo(feature.acceptanceCriteria())
        );
    }

    @Test
    void storiesCarryUseCaseAsTitle() {
        List<UseCase> useCases = List.of(
                new UseCase("Developer", "I want to login with Google", "so that I skip password setup")
        );
        Feature feature = new Feature("oauth2-login", "cap-auth", useCases, buildAcs(10));

        List<StoryProposal> stories = useCase.decompose(feature);

        assertThat(stories.get(0).title()).contains("login with Google");
    }

    private Feature buildFeature(int useCaseCount) {
        List<UseCase> useCases = java.util.stream.IntStream.range(0, useCaseCount)
                .mapToObj(i -> new UseCase("Actor " + i, "I want to do " + i, "so that " + i))
                .toList();
        return new Feature("feat-001", "cap-auth", useCases, buildAcs(10));
    }

    private List<AcceptanceCriterion> buildAcs(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new AcceptanceCriterion("Given state " + i + " When action " + i + " Then result " + i))
                .toList();
    }
}

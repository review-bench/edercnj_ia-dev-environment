package dev.iadev.application.feature;

import dev.iadev.domain.feature.Feature;
import dev.iadev.domain.feature.UseCase;

import java.util.List;

public class FeatureToStoryDecompositionUseCase {

    public List<StoryProposal> decompose(Feature feature) {
        return feature.useCases().stream()
                .map(uc -> toStory(uc, feature))
                .toList();
    }

    private StoryProposal toStory(UseCase useCase, Feature feature) {
        return new StoryProposal(useCase.action(), useCase.actor(), feature.acceptanceCriteria());
    }
}

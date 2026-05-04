package dev.iadev.application.feature;

import dev.iadev.domain.feature.AcceptanceCriterion;

import java.util.List;

public record StoryProposal(String title, String actor, List<AcceptanceCriterion> inheritedAcs) {
}

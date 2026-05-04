package dev.iadev.domain.feature;

import java.util.List;

public final class Feature {

    private final String featureId;
    private final String capabilityId;
    private final List<UseCase> useCases;
    private final List<AcceptanceCriterion> acceptanceCriteria;

    public Feature(String featureId, String capabilityId,
                   List<UseCase> useCases, List<AcceptanceCriterion> acceptanceCriteria) {
        this.featureId = featureId;
        this.capabilityId = capabilityId;
        this.useCases = List.copyOf(useCases);
        this.acceptanceCriteria = List.copyOf(acceptanceCriteria);
    }

    public String featureId() { return featureId; }
    public String capabilityId() { return capabilityId; }
    public List<UseCase> useCases() { return useCases; }
    public List<AcceptanceCriterion> acceptanceCriteria() { return acceptanceCriteria; }
}

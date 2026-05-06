package dev.iadev.application.feature;

import java.nio.file.Path;

public record CreateEpicFromFeatureResult(Path epicFile, int inheritedRnfCount) {

    public CreateEpicFromFeatureResult {
        if (epicFile == null) {
            throw new IllegalArgumentException("epicFile must not be null");
        }
    }
}

package dev.iadev.application.feature;

import java.nio.file.Path;
import java.util.List;

public record CreateStoriesFromFeatureResult(List<Path> storyFiles) {

    public CreateStoriesFromFeatureResult {
        storyFiles = List.copyOf(storyFiles);
    }
}

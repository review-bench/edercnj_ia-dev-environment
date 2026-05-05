package dev.iadev.application.feature;

public record InheritedRnfLine(
        String id, String sourceLevel, String requirement, boolean waivable) {

    public InheritedRnfLine {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be null or blank");
        }
        if (sourceLevel == null || sourceLevel.isBlank()) {
            throw new IllegalArgumentException("sourceLevel must not be null or blank");
        }
        if (requirement == null || requirement.isBlank()) {
            throw new IllegalArgumentException("requirement must not be null or blank");
        }
    }
}

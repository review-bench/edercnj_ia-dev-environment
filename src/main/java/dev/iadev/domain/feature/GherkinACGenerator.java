package dev.iadev.domain.feature;

import java.util.List;

public final class GherkinACGenerator {

    public List<String> generate(String featureName) {
        if (featureName == null || featureName.isBlank()) {
            throw new IllegalArgumentException("featureName must not be null or blank");
        }
        return List.of(
                "Scenario: degenerate - null input rejected for " + featureName
                        + "\n  Given no valid input is provided"
                        + "\n  When " + featureName + " is invoked with null"
                        + "\n  Then an IllegalArgumentException is thrown",
                "Scenario: degenerate - blank input rejected for " + featureName
                        + "\n  Given a blank string is provided as input"
                        + "\n  When " + featureName + " is invoked"
                        + "\n  Then an IllegalArgumentException is thrown",
                "Scenario: degenerate - missing required fields for " + featureName
                        + "\n  Given required fields are absent"
                        + "\n  When " + featureName + " is invoked"
                        + "\n  Then a validation error is returned",
                "Scenario: happy path - successful execution of " + featureName
                        + "\n  Given valid input is provided"
                        + "\n  When " + featureName + " is executed"
                        + "\n  Then the operation completes successfully",
                "Scenario: happy path - expected output produced by " + featureName
                        + "\n  Given a standard valid request"
                        + "\n  When " + featureName + " processes the input"
                        + "\n  Then the expected output is returned",
                "Scenario: happy path - idempotent execution of " + featureName
                        + "\n  Given the same input is provided twice"
                        + "\n  When " + featureName + " is executed twice"
                        + "\n  Then the result is identical both times",
                "Scenario: happy path - all optional fields accepted by " + featureName
                        + "\n  Given all optional fields are provided"
                        + "\n  When " + featureName + " is executed"
                        + "\n  Then all fields are reflected in the output",
                "Scenario: error - invalid format rejected by " + featureName
                        + "\n  Given input with invalid format is provided"
                        + "\n  When " + featureName + " is invoked"
                        + "\n  Then a format validation error is returned",
                "Scenario: error - unsupported operation handled by " + featureName
                        + "\n  Given an unsupported operation is requested"
                        + "\n  When " + featureName + " is invoked"
                        + "\n  Then an UnsupportedOperationException is raised",
                "Scenario: error - concurrent modification handled in " + featureName
                        + "\n  Given concurrent requests arrive simultaneously"
                        + "\n  When " + featureName + " processes them"
                        + "\n  Then each request is handled independently without corruption",
                "Scenario: boundary - maximum input size for " + featureName
                        + "\n  Given input at the maximum allowed size"
                        + "\n  When " + featureName + " processes it"
                        + "\n  Then it succeeds within the performance SLO of 20s",
                "Scenario: boundary - minimum input size for " + featureName
                        + "\n  Given input at the minimum allowed size"
                        + "\n  When " + featureName + " processes it"
                        + "\n  Then it succeeds and produces minimal valid output"
        );
    }
}

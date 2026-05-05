package dev.iadev.domain.architecture;

import dev.iadev.domain.architecture.C4Diagram.C4Level;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class C4LevelValidator {

    private static final List<C4Level> REQUIRED_LEVELS =
            List.of(C4Level.CONTEXT, C4Level.CONTAINER, C4Level.COMPONENT);

    public ValidationResult validate(List<C4Diagram> diagrams) {
        Set<C4Level> present = diagrams == null ? EnumSet.noneOf(C4Level.class) : collectPresentLevels(diagrams);
        List<C4Level> missing = REQUIRED_LEVELS.stream().filter(level -> !present.contains(level)).toList();
        return missing.isEmpty() ? validResult(present) : invalidResult(missing, present);
    }

    private Set<C4Level> collectPresentLevels(List<C4Diagram> diagrams) {
        return diagrams.stream()
                .map(C4Diagram::level)
                .filter(REQUIRED_LEVELS::contains)
                .collect(() -> EnumSet.noneOf(C4Level.class), Set::add, Set::addAll);
    }

    private ValidationResult validResult(Set<C4Level> present) {
        return new ValidationResult(true, List.of(), REQUIRED_LEVELS.stream().filter(present::contains).toList(), "OK");
    }

    private ValidationResult invalidResult(List<C4Level> missing, Set<C4Level> present) {
        List<C4Level> presentLevels = REQUIRED_LEVELS.stream().filter(present::contains).toList();
        return new ValidationResult(false, missing, presentLevels, "C4 mandatory levels missing: " + missing);
    }

    public record ValidationResult(boolean valid, List<C4Level> missingLevels, List<C4Level> presentLevels,
            String message) {

        public ValidationResult {
            missingLevels = List.copyOf(missingLevels);
            presentLevels = List.copyOf(presentLevels);
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("message must not be blank");
            }
        }
    }
}

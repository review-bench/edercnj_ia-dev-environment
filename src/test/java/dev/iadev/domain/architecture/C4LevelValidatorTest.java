package dev.iadev.domain.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.architecture.C4Diagram.C4Level;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("C4LevelValidator")
class C4LevelValidatorTest {

    private final C4LevelValidator validator = new C4LevelValidator();

    @Test
    void validate_whenMandatoryLevelsPresent_returnsValid() {
        var result = validator.validate(List.of(
                diagram(C4Level.CONTEXT),
                diagram(C4Level.CONTAINER),
                diagram(C4Level.COMPONENT)));

        assertThat(result.valid()).isTrue();
        assertThat(result.missingLevels()).isEmpty();
        assertThat(result.presentLevels()).containsExactly(C4Level.CONTEXT, C4Level.CONTAINER, C4Level.COMPONENT);
    }

    @Test
    void validate_whenComponentMissing_returnsInvalid() {
        var result = validator.validate(List.of(
                diagram(C4Level.CONTEXT),
                diagram(C4Level.CONTAINER)));

        assertThat(result.valid()).isFalse();
        assertThat(result.missingLevels()).containsExactly(C4Level.COMPONENT);
        assertThat(result.message()).isEqualTo("C4 mandatory levels missing: [COMPONENT]");
    }

    @Test
    void validate_whenListNull_reportsAllMandatoryLevelsMissing() {
        var result = validator.validate(null);

        assertThat(result.valid()).isFalse();
        assertThat(result.missingLevels()).containsExactly(C4Level.CONTEXT, C4Level.CONTAINER, C4Level.COMPONENT);
    }

    private C4Diagram diagram(C4Level level) {
        return new C4Diagram(level + " title", level, C4OutputFormat.MERMAID, level + " content");
    }
}

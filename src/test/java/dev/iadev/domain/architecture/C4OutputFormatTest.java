package dev.iadev.domain.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("C4OutputFormat")
class C4OutputFormatTest {

    @Test
    void fromString_null_returnsMermaid() {
        assertThat(C4OutputFormat.fromString(null)).isEqualTo(C4OutputFormat.MERMAID);
    }

    @Test
    void fromString_mmd_returnsMermaid() {
        assertThat(C4OutputFormat.fromString("mmd")).isEqualTo(C4OutputFormat.MERMAID);
    }

    @Test
    void fromString_mermaid_returnsMermaid() {
        assertThat(C4OutputFormat.fromString("mermaid")).isEqualTo(C4OutputFormat.MERMAID);
    }

    @Test
    void fromString_plantuml_returnsPlantuml() {
        assertThat(C4OutputFormat.fromString("plantuml")).isEqualTo(C4OutputFormat.PLANTUML);
    }

    @Test
    void fromString_puml_returnsPlantuml() {
        assertThat(C4OutputFormat.fromString("puml")).isEqualTo(C4OutputFormat.PLANTUML);
    }

    @Test
    void fromString_unknown_throws() {
        assertThatThrownBy(() -> C4OutputFormat.fromString("svg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("svg");
    }
}

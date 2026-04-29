package dev.iadev.domain.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ParameterSpec")
class ParameterSpecTest {

    @Nested
    @DisplayName("of() factory")
    class Factory {

        @Test
        @DisplayName("creates string param with defaults")
        void createsStringParam() {
            var spec = ParameterSpec.of("version", ParameterSpec.ParameterType.STRING);
            assertThat(spec.name()).isEqualTo("version");
            assertThat(spec.type()).isEqualTo(ParameterSpec.ParameterType.STRING);
            assertThat(spec.values()).isEmpty();
            assertThat(spec.defaultValue()).isEmpty();
            assertThat(spec.description()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ENUM type validation")
    class EnumValidation {

        @Test
        @DisplayName("ENUM without values throws")
        void enumWithoutValuesThrows() {
            assertThatThrownBy(() -> ParameterSpec.of("mode", ParameterSpec.ParameterType.ENUM))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ENUM parameter");
        }

        @Test
        @DisplayName("ENUM with values accepted")
        void enumWithValuesAccepted() {
            var spec = new ParameterSpec("env", ParameterSpec.ParameterType.ENUM,
                    List.of("dev", "prod"), Optional.empty(), "environment");
            assertThat(spec.values()).containsExactly("dev", "prod");
        }
    }

    @Nested
    @DisplayName("null safety")
    class NullSafety {

        @Test
        @DisplayName("null name throws")
        void nullNameThrows() {
            assertThatThrownBy(() -> ParameterSpec.of(null, ParameterSpec.ParameterType.STRING))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("null type throws")
        void nullTypeThrows() {
            assertThatThrownBy(() -> ParameterSpec.of("version", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("null values defaults to empty list")
        void nullValuesDefaultToEmpty() {
            var spec = new ParameterSpec("x", ParameterSpec.ParameterType.STRING, null, null, null);
            assertThat(spec.values()).isEmpty();
            assertThat(spec.defaultValue()).isEmpty();
            assertThat(spec.description()).isEmpty();
        }
    }
}

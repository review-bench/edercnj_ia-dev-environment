package dev.iadev.domain.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ResolutionWarning")
class ResolutionWarningTest {

    @Nested
    @DisplayName("of() factory")
    class Factory {

        @Test
        @DisplayName("creates warning with empty context")
        void createsWithEmptyContext() {
            var w = ResolutionWarning.of(ResolutionWarning.Kind.UNKNOWN_CAPABILITY, "test message");
            assertThat(w.kind()).isEqualTo(ResolutionWarning.Kind.UNKNOWN_CAPABILITY);
            assertThat(w.message()).isEqualTo("test message");
            assertThat(w.context()).isEmpty();
        }

        @Test
        @DisplayName("null kind throws")
        void nullKindThrows() {
            assertThatThrownBy(() -> new ResolutionWarning(null, "msg", "ctx"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("null message throws")
        void nullMessageThrows() {
            assertThatThrownBy(() -> new ResolutionWarning(ResolutionWarning.Kind.UNKNOWN_CAPABILITY, null, "ctx"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("null context defaults to empty string")
        void nullContextDefaultsToEmpty() {
            var w = new ResolutionWarning(ResolutionWarning.Kind.CYCLIC_DEPENDENCY_SUSPECT, "msg", null);
            assertThat(w.context()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("includes kind and message")
        void includesKindAndMessage() {
            var w = ResolutionWarning.of(ResolutionWarning.Kind.DEPRECATED_CAPABILITY, "deprecated!");
            assertThat(w.toString()).contains("DEPRECATED_CAPABILITY").contains("deprecated!");
        }

        @Test
        @DisplayName("includes context when present")
        void includesContextWhenPresent() {
            var w = new ResolutionWarning(ResolutionWarning.Kind.GLOB_MATCHED_ZERO, "no match", "data.database.*");
            assertThat(w.toString()).contains("data.database.*");
        }
    }
}

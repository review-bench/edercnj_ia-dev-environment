package dev.iadev.domain.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ResolvedCapabilitySet")
class ResolvedCapabilitySetTest {

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("null capabilities default to empty list")
        void nullCapabilitiesDefaultToEmpty() {
            var set = new ResolvedCapabilitySet("test", null, null, null);
            assertThat(set.capabilities()).isEmpty();
            assertThat(set.effectiveParameters()).isEmpty();
            assertThat(set.warnings()).isEmpty();
        }

        @Test
        @DisplayName("null profileName throws")
        void nullProfileNameThrows() {
            assertThatThrownBy(
                            () -> new ResolvedCapabilitySet(null, List.of(), Map.of(), List.of()))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("contains()")
    class Contains {

        @Test
        @DisplayName("contains returns true for active capability")
        void containsReturnsTrueForActive() {
            CapabilityId id = CapabilityId.of("data.database.postgres");
            var set = new ResolvedCapabilitySet("test", List.of(id), Map.of(), List.of());
            assertThat(set.contains(id)).isTrue();
        }

        @Test
        @DisplayName("contains returns false for inactive capability")
        void containsReturnsFalseForInactive() {
            var set =
                    new ResolvedCapabilitySet(
                            "test",
                            List.of(CapabilityId.of("data.database.postgres")),
                            Map.of(),
                            List.of());
            assertThat(set.contains(CapabilityId.of("data.cache.redis"))).isFalse();
        }
    }

    @Nested
    @DisplayName("parameter()")
    class Parameter {

        @Test
        @DisplayName("returns present parameter value")
        void returnsParameter() {
            var set =
                    new ResolvedCapabilitySet(
                            "test",
                            List.of(),
                            Map.of("data.database.postgres.version", "16"),
                            List.of());
            assertThat(set.parameter("data.database.postgres.version")).contains("16");
        }

        @Test
        @DisplayName("returns empty for missing parameter")
        void returnsEmptyForMissing() {
            var set = new ResolvedCapabilitySet("test", List.of(), Map.of(), List.of());
            assertThat(set.parameter("missing.key")).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasWarnings()")
    class HasWarnings {

        @Test
        @DisplayName("false when no warnings")
        void falseWhenNoWarnings() {
            var set = new ResolvedCapabilitySet("test", List.of(), Map.of(), List.of());
            assertThat(set.hasWarnings()).isFalse();
        }

        @Test
        @DisplayName("true when warnings present")
        void trueWhenWarnings() {
            var set =
                    new ResolvedCapabilitySet(
                            "test",
                            List.of(),
                            Map.of(),
                            List.of(
                                    ResolutionWarning.of(
                                            ResolutionWarning.Kind.UNKNOWN_CAPABILITY, "test")));
            assertThat(set.hasWarnings()).isTrue();
        }
    }
}

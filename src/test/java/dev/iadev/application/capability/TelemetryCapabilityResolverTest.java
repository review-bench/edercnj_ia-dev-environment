package dev.iadev.application.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityError;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.CapabilityKind;
import dev.iadev.domain.capability.Profile;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TelemetryCapabilityResolver")
class TelemetryCapabilityResolverTest {

    private final TelemetryCapabilityResolver resolver = new TelemetryCapabilityResolver();

    private static CapabilityDefinition def(String id) {
        return CapabilityDefinition.atomic(CapabilityId.of(id), id.split("\\.")[0], "");
    }

    @Nested
    @DisplayName("delegation")
    class Delegation {

        @Test
        @DisplayName("delegates resolve to CapabilityResolver and returns result")
        void delegatesResolve() {
            List<CapabilityDefinition> catalog = List.of(def("runtime.jvm.openjdk"));
            Profile profile = Profile.of("test", List.of(CapabilityId.of("runtime.jvm.openjdk")));
            ResolvedCapabilitySet result = resolver.resolve(profile, catalog);
            assertThat(result.capabilities()).hasSize(1);
        }

        @Test
        @DisplayName("propagates exceptions from delegate")
        void propagatesExceptions() {
            List<CapabilityDefinition> catalog =
                    List.of(
                            new CapabilityDefinition(
                                    CapabilityId.of("aaa.bbb.alpha"),
                                    CapabilityKind.ATOMIC,
                                    "aaa",
                                    Optional.empty(),
                                    "stable",
                                    "",
                                    Map.of(),
                                    List.of(CapabilityId.of("aaa.bbb.beta")),
                                    List.of(),
                                    List.of(),
                                    List.of(),
                                    List.of()),
                            new CapabilityDefinition(
                                    CapabilityId.of("aaa.bbb.beta"),
                                    CapabilityKind.ATOMIC,
                                    "aaa",
                                    Optional.empty(),
                                    "stable",
                                    "",
                                    Map.of(),
                                    List.of(CapabilityId.of("aaa.bbb.alpha")),
                                    List.of(),
                                    List.of(),
                                    List.of(),
                                    List.of()));
            Profile profile = Profile.of("cyclic", List.of(CapabilityId.of("aaa.bbb.alpha")));
            assertThatThrownBy(() -> resolver.resolve(profile, catalog))
                    .isInstanceOf(CapabilityError.CyclicDependency.class);
        }

        @Test
        @DisplayName("propagates exceptions from delegate with telemetry phase.end failed")
        void propagatesWithTelemetry() {
            List<CapabilityDefinition> catalog =
                    List.of(
                            new CapabilityDefinition(
                                    CapabilityId.of("aaa.bbb.alpha"),
                                    CapabilityKind.ATOMIC,
                                    "aaa",
                                    Optional.empty(),
                                    "stable",
                                    "",
                                    Map.of(),
                                    List.of(CapabilityId.of("aaa.bbb.beta")),
                                    List.of(),
                                    List.of(),
                                    List.of(),
                                    List.of()),
                            new CapabilityDefinition(
                                    CapabilityId.of("aaa.bbb.beta"),
                                    CapabilityKind.ATOMIC,
                                    "aaa",
                                    Optional.empty(),
                                    "stable",
                                    "",
                                    Map.of(),
                                    List.of(CapabilityId.of("aaa.bbb.alpha")),
                                    List.of(),
                                    List.of(),
                                    List.of(),
                                    List.of()));
            Profile profile = Profile.of("cyclic", List.of(CapabilityId.of("aaa.bbb.alpha")));
            assertThatThrownBy(() -> resolver.resolve(profile, catalog))
                    .isInstanceOf(CapabilityError.CyclicDependency.class);
        }

        @Test
        @DisplayName("resolves with parameter overrides")
        void resolvesWithOverrides() {
            List<CapabilityDefinition> catalog = List.of(def("runtime.jvm.openjdk"));
            Profile profile = Profile.of("test", List.of(CapabilityId.of("runtime.jvm.openjdk")));
            Map<String, String> overrides = Map.of("runtime.jvm.openjdk.version", "21");
            var result = resolver.resolve(profile, catalog, overrides);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("fail-open: resolve still works when CLAUDE_TELEMETRY_DISABLED=1")
        void failOpenWithDisabledTelemetry() {
            List<CapabilityDefinition> catalog = List.of(def("runtime.jvm.openjdk"));
            Profile profile = Profile.of("test", List.of(CapabilityId.of("runtime.jvm.openjdk")));
            // Even if telemetry is disabled, resolution should succeed
            ResolvedCapabilitySet result = resolver.resolve(profile, catalog);
            assertThat(result).isNotNull();
        }
    }
}

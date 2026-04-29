package dev.iadev.application.capability;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.CapabilityKind;
import dev.iadev.domain.capability.Profile;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import dev.iadev.domain.capability.ResolutionWarning;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ProfileExpander")
class ProfileExpanderTest {

    private final ProfileExpander expander = new ProfileExpander();

    private static CapabilityDefinition def(String id, List<String> requires) {
        return new CapabilityDefinition(
                CapabilityId.of(id), CapabilityKind.ATOMIC, id.split("\\.")[0],
                Optional.empty(), "stable", "",
                Map.of(), requires.stream().map(CapabilityId::of).toList(),
                List.of(), List.of(), List.of(), List.of());
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("null overrides treated as empty map")
        void nullOverridesTreatedAsEmpty() {
            List<CapabilityDefinition> catalog = List.of(def("runtime.jvm.openjdk", List.of()));
            Profile profile = Profile.of("test", List.of(CapabilityId.of("runtime.jvm.openjdk")));
            ResolvedCapabilitySet result = expander.expand(profile, catalog, null);
            assertThat(result.capabilities()).hasSize(1);
            assertThat(result.effectiveParameters()).isEmpty();
        }

        @Test
        @DisplayName("profile with glob capability skips it during expansion")
        void globCapabilitySkippedDuringExpansion() {
            List<CapabilityDefinition> catalog = List.of(
                    def("data.database.postgres", List.of()),
                    def("data.database.mysql", List.of())
            );
            Profile profile = Profile.of("glob-test", List.of(
                    CapabilityId.of("data.database.*"),
                    CapabilityId.of("data.database.postgres")
            ));
            // Glob capabilities are excluded by Profile.expand(), only atomic is expanded
            ResolvedCapabilitySet result = expander.expand(profile, catalog, Map.of());
            assertThat(result.contains(CapabilityId.of("data.database.postgres"))).isTrue();
        }
    }

    @Nested
    @DisplayName("basic expansion")
    class BasicExpansion {

        @Test
        @DisplayName("minimal profile with single capability (degenerate)")
        void minimalProfile() {
            List<CapabilityDefinition> catalog = List.of(def("runtime.jvm.openjdk", List.of()));
            Profile profile = Profile.of("minimal", List.of(CapabilityId.of("runtime.jvm.openjdk")));

            ResolvedCapabilitySet result = expander.expand(profile, catalog, Map.of());
            assertThat(result.capabilities()).hasSize(1);
            assertThat(result.effectiveParameters()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("profile with transitive prerequisites expands all (happy)")
        void transitivePrerequsites() {
            List<CapabilityDefinition> catalog = List.of(
                    def("runtime.jvm.openjdk", List.of()),
                    def("web.servlet.api", List.of("runtime.jvm.openjdk")),
                    def("framework.spring-boot.mvc", List.of("web.servlet.api"))
            );
            Profile profile = Profile.of("spring", List.of(
                    CapabilityId.of("framework.spring-boot.mvc")
            ));
            ResolvedCapabilitySet result = expander.expand(profile, catalog, Map.of());
            assertThat(result.capabilities()).hasSize(3);
            assertThat(result.contains(CapabilityId.of("runtime.jvm.openjdk"))).isTrue();
        }

        @Test
        @DisplayName("parameter override applied for active capability (composition)")
        void parameterOverrideApplied() {
            List<CapabilityDefinition> catalog = List.of(def("data.database.postgres", List.of()));
            Profile profile = Profile.of("pg-profile", List.of(CapabilityId.of("data.database.postgres")));
            Map<String, String> overrides = Map.of("data.database.postgres.version", "15");

            ResolvedCapabilitySet result = expander.expand(profile, catalog, overrides);
            assertThat(result.parameter("data.database.postgres.version")).contains("15");
        }

        @Test
        @DisplayName("override for inactive capability emits warning and is skipped")
        void inactiveCapabilityOverrideWarns() {
            List<CapabilityDefinition> catalog = List.of(
                    def("data.database.postgres", List.of()),
                    def("data.database.mysql", List.of())
            );
            Profile profile = Profile.of("pg-only", List.of(CapabilityId.of("data.database.postgres")));
            Map<String, String> overrides = Map.of("data.database.mysql.version", "8");

            ResolvedCapabilitySet result = expander.expand(profile, catalog, overrides);
            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0).kind()).isEqualTo(ResolutionWarning.Kind.UNKNOWN_CAPABILITY);
            assertThat(result.effectiveParameters()).doesNotContainKey("data.database.mysql.version");
        }
    }
}

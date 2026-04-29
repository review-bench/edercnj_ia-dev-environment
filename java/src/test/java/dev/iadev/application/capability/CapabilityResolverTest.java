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

@DisplayName("CapabilityResolver")
class CapabilityResolverTest {

    private final CapabilityResolver resolver = new CapabilityResolver();

    private static CapabilityDefinition def(
            String id, List<String> requires, List<String> excludes) {
        return new CapabilityDefinition(
                CapabilityId.of(id),
                CapabilityKind.ATOMIC,
                id.split("\\.")[0],
                Optional.empty(),
                "stable",
                "",
                Map.of(),
                requires.stream().map(CapabilityId::of).toList(),
                List.of(),
                excludes.stream().map(CapabilityId::of).toList(),
                List.of(),
                List.of());
    }

    private static CapabilityDefinition def(String id) {
        return def(id, List.of(), List.of());
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("resolves valid profile with transitive prerequisites")
        void resolvesValidProfile() {
            List<CapabilityDefinition> catalog =
                    List.of(
                            def("runtime.jvm.openjdk"),
                            def(
                                    "framework.spring-boot.mvc",
                                    List.of("runtime.jvm.openjdk"),
                                    List.of()));
            Profile profile =
                    Profile.of("spring", List.of(CapabilityId.of("framework.spring-boot.mvc")));
            ResolvedCapabilitySet result = resolver.resolve(profile, catalog);
            assertThat(result.capabilities()).hasSize(2);
            assertThat(result.contains(CapabilityId.of("runtime.jvm.openjdk"))).isTrue();
        }

        @Test
        @DisplayName("output is deterministic across 3 consecutive invocations (RULE-004)")
        void deterministic() {
            List<CapabilityDefinition> catalog =
                    List.of(def("runtime.jvm.openjdk"), def("data.database.postgres"));
            Profile profile =
                    Profile.of(
                            "multi",
                            List.of(
                                    CapabilityId.of("runtime.jvm.openjdk"),
                                    CapabilityId.of("data.database.postgres")));
            ResolvedCapabilitySet r1 = resolver.resolve(profile, catalog);
            ResolvedCapabilitySet r2 = resolver.resolve(profile, catalog);
            ResolvedCapabilitySet r3 = resolver.resolve(profile, catalog);
            assertThat(r1.capabilities()).isEqualTo(r2.capabilities());
            assertThat(r2.capabilities()).isEqualTo(r3.capabilities());
        }
    }

    @Nested
    @DisplayName("error paths")
    class ErrorPaths {

        @Test
        @DisplayName("cyclic catalog throws CyclicDependency")
        void cyclicCatalogFails() {
            List<CapabilityDefinition> catalog =
                    List.of(
                            def("aaa.bbb.alpha", List.of("aaa.bbb.beta"), List.of()),
                            def("aaa.bbb.beta", List.of("aaa.bbb.alpha"), List.of()));
            Profile profile = Profile.of("cyclic", List.of(CapabilityId.of("aaa.bbb.alpha")));
            assertThatThrownBy(() -> resolver.resolve(profile, catalog))
                    .isInstanceOf(CapabilityError.CyclicDependency.class);
        }

        @Test
        @DisplayName("profile with mutex pair throws MutexConflict")
        void mutexConflictFails() {
            List<CapabilityDefinition> catalog =
                    List.of(
                            def(
                                    "data.database.postgres",
                                    List.of(),
                                    List.of("data.database.mongo")),
                            def(
                                    "data.database.mongo",
                                    List.of(),
                                    List.of("data.database.postgres")));
            Profile profile =
                    Profile.of(
                            "conflict",
                            List.of(
                                    CapabilityId.of("data.database.postgres"),
                                    CapabilityId.of("data.database.mongo")));
            assertThatThrownBy(() -> resolver.resolve(profile, catalog))
                    .isInstanceOf(CapabilityError.MutexConflict.class);
        }

        @Test
        @DisplayName("missing prerequisite throws MissingPrerequisite")
        void missingPrerequisiteFails() {
            List<CapabilityDefinition> catalog =
                    List.of(def("aaa.bbb.x", List.of("aaa.bbb.missing"), List.of()));
            Profile profile = Profile.of("broken", List.of(CapabilityId.of("aaa.bbb.x")));
            assertThatThrownBy(() -> resolver.resolve(profile, catalog))
                    .isInstanceOf(CapabilityError.MissingPrerequisite.class);
        }
    }
}

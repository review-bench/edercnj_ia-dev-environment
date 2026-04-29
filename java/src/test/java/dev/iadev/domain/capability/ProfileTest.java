package dev.iadev.domain.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Profile")
class ProfileTest {

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects null name")
        void rejectsNullName() {
            assertThatThrownBy(() -> Profile.of(null, List.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects blank name")
        void rejectsBlankName() {
            assertThatThrownBy(() -> Profile.of("  ", List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("capabilities() accessor")
    class Capabilities {

        @Test
        @DisplayName("capabilities() includes globs")
        void capabilitiesIncludesGlobs() {
            Profile profile =
                    Profile.of(
                            "test",
                            List.of(
                                    CapabilityId.of("data.database.postgres"),
                                    CapabilityId.of("data.cache.*")));
            assertThat(profile.capabilities()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("expand()")
    class ExpandMethod {

        @Test
        @DisplayName("expand returns set of non-glob atomics")
        void expandReturnsAtomics() {
            Profile profile =
                    Profile.of(
                            "spring-rest-api-postgres",
                            List.of(
                                    CapabilityId.of("framework.spring-boot.mvc"),
                                    CapabilityId.of("framework.spring-boot.data-jpa"),
                                    CapabilityId.of("data.database.postgres"),
                                    CapabilityId.of("interface.rest.openapi"),
                                    CapabilityId.of("testing.framework.junit5")));

            Set<CapabilityId> expanded = profile.expand();
            assertThat(expanded).hasSize(5);
            assertThat(expanded).allMatch(id -> !id.isGlob());
        }

        @Test
        @DisplayName("expand excludes glob capabilities")
        void expandExcludesGlobs() {
            Profile profile =
                    Profile.of(
                            "mixed",
                            List.of(
                                    CapabilityId.of("data.database.postgres"),
                                    CapabilityId.of("data.cache.*")));

            Set<CapabilityId> expanded = profile.expand();
            assertThat(expanded).hasSize(1);
            assertThat(expanded).containsExactly(CapabilityId.of("data.database.postgres"));
        }

        @Test
        @DisplayName("expand is deterministic across invocations")
        void expandIsDeterministic() {
            Profile profile =
                    Profile.of(
                            "spring-rest-api-postgres",
                            List.of(
                                    CapabilityId.of("framework.spring-boot.mvc"),
                                    CapabilityId.of("data.database.postgres"),
                                    CapabilityId.of("testing.framework.junit5")));

            Set<CapabilityId> first = profile.expand();
            Set<CapabilityId> second = profile.expand();
            Set<CapabilityId> third = profile.expand();

            assertThat(first).isEqualTo(second);
            assertThat(second).isEqualTo(third);
            assertThat(List.copyOf(first)).isEqualTo(List.copyOf(second));
        }
    }
}

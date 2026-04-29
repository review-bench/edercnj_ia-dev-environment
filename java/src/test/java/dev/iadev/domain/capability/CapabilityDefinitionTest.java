package dev.iadev.domain.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CapabilityDefinition")
class CapabilityDefinitionTest {

    @Nested
    @DisplayName("atomic() factory")
    class Atomic {

        @Test
        @DisplayName("creates atomic with defaults")
        void createsAtomicDefaults() {
            CapabilityDefinition def = CapabilityDefinition.atomic(
                    CapabilityId.of("data.database.postgres"), "data", "PostgreSQL");
            assertThat(def.kind()).isEqualTo(CapabilityKind.ATOMIC);
            assertThat(def.status()).isEqualTo("stable");
            assertThat(def.requires()).isEmpty();
            assertThat(def.excludes()).isEmpty();
            assertThat(def.expandsTo()).isEmpty();
        }
    }

    @Nested
    @DisplayName("profile() factory")
    class Profile {

        @Test
        @DisplayName("creates profile with expands-to")
        void createsProfile() {
            CapabilityDefinition def = CapabilityDefinition.profile(
                    CapabilityId.of("framework.spring-boot.full"),
                    "framework",
                    List.of(CapabilityId.of("framework.spring-boot.mvc")));
            assertThat(def.kind()).isEqualTo(CapabilityKind.PROFILE);
            assertThat(def.expandsTo()).hasSize(1);
        }

        @Test
        @DisplayName("profile without expands-to throws")
        void profileWithoutExpandsToThrows() {
            assertThatThrownBy(() -> new CapabilityDefinition(
                    CapabilityId.of("framework.spring-boot.full"), CapabilityKind.PROFILE, "framework",
                    Optional.empty(), "stable", "", java.util.Map.of(), List.of(), List.of(), List.of(), List.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("isDeprecated()")
    class IsDeprecated {

        @Test
        @DisplayName("stable is not deprecated")
        void stableNotDeprecated() {
            assertThat(CapabilityDefinition.atomic(
                    CapabilityId.of("data.database.postgres"), "data", "").isDeprecated()).isFalse();
        }

        @Test
        @DisplayName("deprecated status returns true")
        void deprecatedStatusTrue() {
            CapabilityDefinition def = new CapabilityDefinition(
                    CapabilityId.of("data.database.postgres"), CapabilityKind.ATOMIC, "data",
                    Optional.empty(), "deprecated", "", java.util.Map.of(),
                    List.of(), List.of(), List.of(), List.of(), List.of());
            assertThat(def.isDeprecated()).isTrue();
        }
    }

    @Nested
    @DisplayName("null safety in canonical constructor")
    class NullSafety {

        @Test
        @DisplayName("null lists default to empty")
        void nullListsDefaultToEmpty() {
            CapabilityDefinition def = new CapabilityDefinition(
                    CapabilityId.of("data.database.postgres"), CapabilityKind.ATOMIC, "data",
                    null, null, null, null, null, null, null, null, null);
            assertThat(def.requires()).isEmpty();
            assertThat(def.tags()).isEmpty();
            assertThat(def.status()).isEqualTo("stable");
        }
    }
}

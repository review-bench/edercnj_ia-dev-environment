package dev.iadev.application.capability;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityError;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.CapabilityKind;
import dev.iadev.domain.capability.Profile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MutexValidator")
class MutexValidatorTest {

    private final MutexValidator validator = new MutexValidator();

    private static CapabilityDefinition def(String id, List<String> excludes) {
        return new CapabilityDefinition(
                CapabilityId.of(id),
                CapabilityKind.ATOMIC,
                id.split("\\.")[0],
                Optional.empty(),
                "stable",
                "",
                Map.of(),
                List.of(),
                List.of(),
                excludes.stream().map(CapabilityId::of).toList(),
                List.of(),
                List.of());
    }

    @Nested
    @DisplayName("validateCatalog()")
    class ValidateCatalog {

        @Test
        @DisplayName("no excludes — passes (degenerate)")
        void noExcludesPasses() {
            List<CapabilityDefinition> catalog =
                    List.of(
                            def("data.database.postgres", List.of()),
                            def("data.database.mysql", List.of()));
            assertThatCode(() -> validator.validateCatalog(catalog)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("symmetric excludes — passes (happy)")
        void symmetricPasses() {
            List<CapabilityDefinition> catalog =
                    List.of(
                            def("data.database.postgres", List.of("data.database.mongo")),
                            def("data.database.mongo", List.of("data.database.postgres")));
            assertThatCode(() -> validator.validateCatalog(catalog)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("asymmetric excludes — throws MutexConflict (error path — catalog defect)")
        void asymmetricThrows() {
            List<CapabilityDefinition> catalog =
                    List.of(
                            def("data.database.postgres", List.of("data.database.mongo")),
                            def("data.database.mongo", List.of()));
            assertThatThrownBy(() -> validator.validateCatalog(catalog))
                    .isInstanceOf(CapabilityError.MutexConflict.class)
                    .hasMessageContaining("asymmetric mutex")
                    .hasMessageContaining("postgres")
                    .hasMessageContaining("mongo");
        }
    }

    @Nested
    @DisplayName("validateProfile()")
    class ValidateProfile {

        @Test
        @DisplayName(
                "profile with excluded pair active — throws MutexConflict (error path — usage)")
        void conflictingProfileThrows() {
            List<CapabilityDefinition> catalog =
                    List.of(
                            def("data.database.postgres", List.of("data.database.mongo")),
                            def("data.database.mongo", List.of("data.database.postgres")));
            Profile profile =
                    Profile.of(
                            "my-profile",
                            List.of(
                                    CapabilityId.of("data.database.postgres"),
                                    CapabilityId.of("data.database.mongo")));
            assertThatThrownBy(() -> validator.validateProfile(profile, catalog))
                    .isInstanceOf(CapabilityError.MutexConflict.class)
                    .hasMessageContaining("mutually exclusive")
                    .hasMessageContaining("my-profile");
        }

        @Test
        @DisplayName("profile without conflicting pair — passes")
        void noConflictPasses() {
            List<CapabilityDefinition> catalog =
                    List.of(
                            def("data.database.postgres", List.of("data.database.mongo")),
                            def("data.database.mongo", List.of("data.database.postgres")),
                            def("data.cache.redis", List.of()));
            Profile profile =
                    Profile.of(
                            "safe-profile",
                            List.of(
                                    CapabilityId.of("data.database.postgres"),
                                    CapabilityId.of("data.cache.redis")));
            assertThatCode(() -> validator.validateProfile(profile, catalog))
                    .doesNotThrowAnyException();
        }
    }
}

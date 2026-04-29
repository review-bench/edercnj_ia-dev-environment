package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.application.capability.CapabilityResolver;
import dev.iadev.domain.capability.CapabilityError;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.Profile;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import dev.iadev.infrastructure.adapter.output.YamlCapabilityCatalogAdapter;
import dev.iadev.domain.capability.CapabilityDefinition;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Epic0064CapabilityResolutionSmokeTest")
class Epic0064CapabilityResolutionSmokeTest {

    private static List<CapabilityDefinition> catalog;
    private static CapabilityResolver resolver;

    @BeforeAll
    static void loadCatalog() throws URISyntaxException {
        var resource = Epic0064CapabilityResolutionSmokeTest.class
                .getClassLoader().getResource("capabilities");
        assertThat(resource).isNotNull();
        Path catalogRoot = Path.of(resource.toURI());
        catalog = new YamlCapabilityCatalogAdapter().loadAll(catalogRoot);
        resolver = new CapabilityResolver();
    }

    @Nested
    @DisplayName("happy path — java-cli-picocli profile")
    class HappyPath {

        @Test
        @DisplayName("resolves java-cli-picocli profile with transitive prerequisites")
        void resolvesJavaCliPicocliProfile() {
            Profile profile = Profile.of("java-cli-picocli", List.of(
                    CapabilityId.of("cli.picocli.framework"),
                    CapabilityId.of("build.maven.standard"),
                    CapabilityId.of("runtime.docker.container")
            ));
            ResolvedCapabilitySet result = resolver.resolve(profile, catalog);
            assertThat(result.capabilities()).isNotEmpty();
            assertThat(result.contains(CapabilityId.of("runtime.jvm.openjdk"))).isTrue();
            assertThat(result.contains(CapabilityId.of("cli.picocli.framework"))).isTrue();
            assertThat(result.contains(CapabilityId.of("build.maven.standard"))).isTrue();
            assertThat(result.contains(CapabilityId.of("runtime.docker.container"))).isTrue();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("empty profile returns empty set (boundary)")
        void emptyProfileReturnsEmpty() {
            Profile profile = Profile.of("empty", List.of());
            ResolvedCapabilitySet result = resolver.resolve(profile, catalog);
            assertThat(result.capabilities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("error paths")
    class ErrorPaths {

        @Test
        @DisplayName("mutex pair in profile throws MutexConflict")
        void mutexPairThrows() {
            Profile profile = Profile.of("conflict-profile", List.of(
                    CapabilityId.of("data.database.postgres"),
                    CapabilityId.of("data.database.mongo")
            ));
            assertThatThrownBy(() -> resolver.resolve(profile, catalog))
                    .isInstanceOf(CapabilityError.MutexConflict.class)
                    .hasMessageContaining("mutually exclusive");
        }
    }

    @Nested
    @DisplayName("determinism — RULE-004")
    class Determinism {

        @Test
        @DisplayName("3 consecutive resolutions produce identical results")
        void deterministicAcross3Invocations() {
            Profile profile = Profile.of("det-test", List.of(
                    CapabilityId.of("cli.picocli.framework"),
                    CapabilityId.of("build.maven.standard")
            ));
            ResolvedCapabilitySet r1 = resolver.resolve(profile, catalog);
            ResolvedCapabilitySet r2 = resolver.resolve(profile, catalog);
            ResolvedCapabilitySet r3 = resolver.resolve(profile, catalog);
            assertThat(r1.capabilities()).isEqualTo(r2.capabilities());
            assertThat(r2.capabilities()).isEqualTo(r3.capabilities());
        }
    }
}

package dev.iadev.infrastructure.adapter.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityError;
import dev.iadev.domain.capability.CapabilityKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("YamlCapabilityCatalogAdapter")
class YamlCapabilityCatalogAdapterTest {

    private final YamlCapabilityCatalogAdapter adapter = new YamlCapabilityCatalogAdapter();

    @Nested
    @DisplayName("load(path) — single file")
    class LoadSingle {

        @Test
        @DisplayName("throws UnknownCapability for missing file (degenerate)")
        void missingFileFails(@TempDir Path tmp) {
            Path missing = tmp.resolve("missing.yaml");
            assertThatThrownBy(() -> adapter.load(missing))
                    .isInstanceOf(CapabilityError.UnknownCapability.class)
                    .hasMessageContaining("file not found");
        }

        @Test
        @DisplayName("parses valid capability YAML (happy)")
        void parsesValidYaml(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("postgres.yaml");
            Files.writeString(file, """
                    id: data.database.postgres
                    category: data
                    kind: atomic
                    description: PostgreSQL 16
                    """);
            CapabilityDefinition def = adapter.load(file).orElseThrow();
            assertThat(def.id().value()).isEqualTo("data.database.postgres");
            assertThat(def.kind()).isEqualTo(CapabilityKind.ATOMIC);
            assertThat(def.category()).isEqualTo("data");
        }

        @Test
        @DisplayName("throws with file:line for malformed YAML (error)")
        void malformedYamlFails(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("broken.yaml");
            Files.writeString(file, """
                    id: data.database.postgres
                    category: data
                    kind: [unclosed
                    """);
            assertThatThrownBy(() -> adapter.load(file))
                    .isInstanceOf(CapabilityError.UnknownCapability.class)
                    .hasMessageContaining("broken.yaml");
        }

        @Test
        @DisplayName("rejects YAML missing required fields (schema v3.0 check)")
        void rejectsMissingRequiredFields(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("legacy.yaml");
            Files.writeString(file, """
                    id: data.database.postgres
                    description: missing kind and category
                    """);
            assertThatThrownBy(() -> adapter.load(file))
                    .isInstanceOf(CapabilityError.UnknownCapability.class)
                    .hasMessageContaining("schema v3.0 required");
        }

        @Test
        @DisplayName("parses requires list correctly")
        void parsesRequiresList(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("composite.yaml");
            Files.writeString(file, """
                    id: framework.spring-boot.data-jpa
                    category: framework
                    kind: composite
                    requires:
                      - data.database.postgres
                    """);
            CapabilityDefinition def = adapter.load(file).orElseThrow();
            assertThat(def.requires()).hasSize(1);
            assertThat(def.requires().get(0).value()).isEqualTo("data.database.postgres");
        }
    }

    @Nested
    @DisplayName("loadAll(catalogRoot) — batch")
    class LoadAll {

        @Test
        @DisplayName("returns empty list for empty directory")
        void emptyDirectory(@TempDir Path tmp) throws IOException {
            Path catalog = tmp.resolve("capabilities");
            Files.createDirectories(catalog);
            assertThat(adapter.loadAll(catalog)).isEmpty();
        }

        @Test
        @DisplayName("throws for missing directory")
        void missingDirectory(@TempDir Path tmp) {
            Path missing = tmp.resolve("no-such-dir");
            assertThatThrownBy(() -> adapter.loadAll(missing))
                    .isInstanceOf(CapabilityError.UnknownCapability.class)
                    .hasMessageContaining("catalog root not found");
        }

        @Test
        @DisplayName("loads all yaml files and sorts by id deterministically (boundary)")
        void loadsSortedDeterministically(@TempDir Path tmp) throws IOException {
            Path catalog = tmp.resolve("capabilities");
            Path dataDb = catalog.resolve("data").resolve("database");
            Path dataCache = catalog.resolve("data").resolve("cache");
            Files.createDirectories(dataDb);
            Files.createDirectories(dataCache);

            Files.writeString(dataDb.resolve("postgres.yaml"),
                    "id: data.database.postgres\ncategory: data\nkind: atomic\n");
            Files.writeString(dataDb.resolve("mysql.yaml"),
                    "id: data.database.mysql\ncategory: data\nkind: atomic\n");
            Files.writeString(dataCache.resolve("redis.yaml"),
                    "id: data.cache.redis\ncategory: data\nkind: atomic\n");

            List<CapabilityDefinition> first = adapter.loadAll(catalog);
            List<CapabilityDefinition> second = adapter.loadAll(catalog);

            assertThat(first).hasSize(3);
            assertThat(first.stream().map(d -> d.id().value()).toList())
                    .containsExactly("data.cache.redis", "data.database.mysql", "data.database.postgres");
            assertThat(first.stream().map(d -> d.id().value()).toList())
                    .isEqualTo(second.stream().map(d -> d.id().value()).toList());
        }
    }
}

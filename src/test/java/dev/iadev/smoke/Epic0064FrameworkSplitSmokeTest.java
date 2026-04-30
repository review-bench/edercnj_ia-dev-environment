package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test for Phase 4 of EPIC-0064: Framework-specific content split.
 *
 * <p>Validates that: 1. Old spring-patterns/ and quarkus-patterns/ directories no longer exist 2.
 * New stack-patterns/spring/, quarkus/, picocli/, helidon/, micronaut/ subdirs exist 3. All new
 * stack-pattern index files have requires-capabilities: [...] set (non-empty) 4. The capabilities
 * catalog has symmetric excludes for production databases 5. JPA fragments exist for all four
 * database engines 6. R2DBC index exists with requires-capabilities
 */
@DisplayName("Epic0064FrameworkSplitSmokeTest")
class Epic0064FrameworkSplitSmokeTest {

    private static final Path KNOWLEDGE_ROOT =
            Path.of("src/main/resources/targets/claude/knowledge");
    private static final Path STACK_PATTERNS = KNOWLEDGE_ROOT.resolve("stack-patterns");
    private static final Path DB_PATTERNS = KNOWLEDGE_ROOT.resolve("database-patterns");
    private static final Path CAPABILITIES_ROOT = Path.of("capabilities");

    // --- Story 0402: Stack-patterns reorganization ---

    @Test
    @DisplayName("Old spring-patterns/ dir must not exist")
    void oldSpringPatternsDir_doesNotExist() {
        assertThat(STACK_PATTERNS.resolve("spring-patterns")).doesNotExist();
    }

    @Test
    @DisplayName("Old quarkus-patterns/ dir must not exist")
    void oldQuarkusPatternsDir_doesNotExist() {
        assertThat(STACK_PATTERNS.resolve("quarkus-patterns")).doesNotExist();
    }

    @Test
    @DisplayName("New framework subdirectories must exist with index.md")
    void newFrameworkSubdirs_existWithIndexFile() {
        List<String> expectedDirs = List.of("spring", "quarkus", "picocli", "helidon", "micronaut");
        for (String dir : expectedDirs) {
            var indexFile = STACK_PATTERNS.resolve(dir).resolve("index.md");
            assertThat(indexFile)
                    .withFailMessage("Missing index.md for framework: " + dir)
                    .exists();
        }
    }

    @Test
    @DisplayName("All new stack-pattern index files must declare requires-capabilities (non-empty)")
    void stackPatternIndexFiles_haveRequiresCapabilities() throws IOException {
        List<String> dirs = List.of("spring", "quarkus", "picocli", "helidon", "micronaut");
        for (String dir : dirs) {
            var indexFile = STACK_PATTERNS.resolve(dir).resolve("index.md");
            String content = Files.readString(indexFile);
            assertThat(content)
                    .withFailMessage(
                            "stack-patterns/"
                                    + dir
                                    + "/index.md missing non-empty requires-capabilities")
                    .containsPattern("requires-capabilities: \\[\\S+");
        }
    }

    @Test
    @DisplayName("spring/index.md must require web.spring.boot capability")
    void springIndex_requiresSpringBootCapability() throws IOException {
        var content = Files.readString(STACK_PATTERNS.resolve("spring/index.md"));
        assertThat(content).contains("requires-capabilities: [web.spring.boot]");
    }

    @Test
    @DisplayName("quarkus/index.md must require web.quarkus.framework capability")
    void quarkusIndex_requiresQuarkusFrameworkCapability() throws IOException {
        var content = Files.readString(STACK_PATTERNS.resolve("quarkus/index.md"));
        assertThat(content).contains("requires-capabilities: [web.quarkus.framework]");
    }

    @Test
    @DisplayName("picocli/index.md must require cli.picocli.framework capability")
    void picocliIndex_requiresPicocliFrameworkCapability() throws IOException {
        var content = Files.readString(STACK_PATTERNS.resolve("picocli/index.md"));
        assertThat(content).contains("requires-capabilities: [cli.picocli.framework]");
    }

    // --- Story 0401: Capabilities catalog correctness ---

    @Test
    @DisplayName(
            "Capabilities catalog: postgres/mysql/mariadb must not exclude h2 (h2 is test-only)")
    void productionDbs_mustNotExcludeH2() throws IOException {
        List<String> productionDbs = List.of("postgres", "mysql", "mariadb");
        for (String db : productionDbs) {
            var yamlPath = CAPABILITIES_ROOT.resolve("data/database/" + db + ".yaml");
            if (!Files.exists(yamlPath)) continue;
            var content = Files.readString(yamlPath);
            assertThat(content)
                    .withFailMessage(
                            "capabilities/data/database/"
                                    + db
                                    + ".yaml must not exclude data.database.h2")
                    .doesNotContain("data.database.h2");
        }
    }

    @Test
    @DisplayName("Capabilities catalog: postgres capability YAML must exist")
    void postgresCapability_exists() {
        assertThat(CAPABILITIES_ROOT.resolve("data/database/postgres.yaml")).exists();
    }

    @Test
    @DisplayName("Capabilities catalog: symmetric excludes between postgres/mysql/mariadb")
    void productionDbs_haveSymmetricExcludes() throws IOException {
        var postgres = Files.readString(CAPABILITIES_ROOT.resolve("data/database/postgres.yaml"));
        var mysql = Files.readString(CAPABILITIES_ROOT.resolve("data/database/mysql.yaml"));
        var mariadb = Files.readString(CAPABILITIES_ROOT.resolve("data/database/mariadb.yaml"));

        // postgres excludes mysql and vice versa
        assertThat(postgres).contains("data.database.mysql");
        assertThat(mysql).contains("data.database.postgres");

        // postgres excludes mariadb and vice versa
        assertThat(postgres).contains("data.database.mariadb");
        assertThat(mariadb).contains("data.database.postgres");

        // mysql excludes mariadb and vice versa
        assertThat(mysql).contains("data.database.mariadb");
        assertThat(mariadb).contains("data.database.mysql");
    }

    // --- Story 0403: JPA fragments ---

    @Test
    @DisplayName("JPA fragment files must exist for all four database engines")
    void jpaFragments_existForAllEngines() {
        List<String> engines = List.of("postgres.md", "mysql.md", "mariadb.md", "h2.md");
        for (String engine : engines) {
            assertThat(DB_PATTERNS.resolve("jpa/" + engine))
                    .withFailMessage("Missing JPA fragment: database-patterns/jpa/" + engine)
                    .exists();
        }
    }

    @Test
    @DisplayName("JPA fragments must declare requires-capabilities and fragment-slot")
    void jpaFragments_haveFrontmatterFields() throws IOException {
        try (Stream<Path> files = Files.list(DB_PATTERNS.resolve("jpa"))) {
            files.filter(p -> p.toString().endsWith(".md"))
                    .forEach(
                            file -> {
                                String content;
                                try {
                                    content = Files.readString(file);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }

                                assertThat(content)
                                        .withFailMessage(file + " missing requires-capabilities")
                                        .containsPattern("requires-capabilities: \\[\\S+");
                                assertThat(content)
                                        .withFailMessage(file + " missing fragment-slot")
                                        .contains("fragment-slot:");
                            });
        }
    }

    // --- Story 0404: R2DBC ---

    @Test
    @DisplayName("R2DBC knowledge pack index must exist")
    void r2dbcIndex_exists() {
        assertThat(DB_PATTERNS.resolve("r2dbc/index.md")).exists();
    }

    @Test
    @DisplayName("R2DBC index must declare requires-capabilities")
    void r2dbcIndex_hasRequiresCapabilities() throws IOException {
        var content = Files.readString(DB_PATTERNS.resolve("r2dbc/index.md"));
        assertThat(content).containsPattern("requires-capabilities: \\[\\S+");
    }

    // --- Stories 0405-0409: Framework skills ---

    @Test
    @DisplayName("Framework-specific skills must exist with SKILL.md")
    void frameworkSkills_exist() {
        List<String> skills =
                List.of(
                        "spring-controller",
                        "quarkus-resource",
                        "picocli-command",
                        "helidon-scaffold",
                        "micronaut-scaffold");
        for (String skill : skills) {
            var skillFile =
                    Path.of("src/main/resources/targets/claude/skills/core/dev")
                            .resolve(skill)
                            .resolve("SKILL.md");
            assertThat(skillFile).withFailMessage("Missing SKILL.md for: " + skill).exists();
        }
    }

    @Test
    @DisplayName("Framework skills must declare requires-capabilities in frontmatter")
    void frameworkSkills_haveRequiresCapabilities() throws IOException {
        List<String> skills =
                List.of(
                        "spring-controller",
                        "quarkus-resource",
                        "picocli-command",
                        "helidon-scaffold",
                        "micronaut-scaffold");
        for (String skill : skills) {
            var skillFile =
                    Path.of("src/main/resources/targets/claude/skills/core/dev")
                            .resolve(skill)
                            .resolve("SKILL.md");
            if (!Files.exists(skillFile)) continue;
            var content = Files.readString(skillFile);
            assertThat(content)
                    .withFailMessage(skill + "/SKILL.md missing requires-capabilities")
                    .containsPattern("requires-capabilities: \\[\\S+");
        }
    }

    // --- Stories 0410-0411: Audit scripts ---

    private static final Path SCRIPTS_ROOT = Path.of("src/main/resources/targets/claude/scripts");

    @Test
    @DisplayName("audit-fragment-coherence.sh must exist and be executable")
    void auditFragmentCoherence_exists() {
        var script = SCRIPTS_ROOT.resolve("audit-fragment-coherence.sh");
        assertThat(script).exists();
        assertThat(script.toFile().canExecute()).isTrue();
    }

    @Test
    @DisplayName("audit-output-pruning.sh must exist and be executable")
    void auditOutputPruning_exists() {
        var script = SCRIPTS_ROOT.resolve("audit-output-pruning.sh");
        assertThat(script).exists();
        assertThat(script.toFile().canExecute()).isTrue();
    }
}

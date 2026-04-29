package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test for Phase 5 of EPIC-0064: x-review composite skill composition.
 *
 * <p>Validates that: 1. The x-review parent SKILL.md declares fragment-slots with
 * review-specialist slot 2. The 8 specialist fragment files exist under x-review/fragments/ 3.
 * Each fragment file has required frontmatter fields (fragment-slot + requires-capabilities/any) 4.
 * Universal fragments (qa, perf, security) have requires-capabilities: [] 5. Conditional fragments
 * (devops, db, api, event, compliance) have appropriate capability requirements 6. Fragment order
 * is deterministic (fragment-order values unique and ascending)
 */
@DisplayName("Epic0064ReviewCompositionSmokeTest")
class Epic0064ReviewCompositionSmokeTest {

    private static final Path X_REVIEW_ROOT =
            Path.of(
                    "src/main/resources/targets/claude/skills/core/review/x-review");
    private static final Path FRAGMENTS_ROOT = X_REVIEW_ROOT.resolve("fragments");
    private static final Path PARENT_SKILL = X_REVIEW_ROOT.resolve("SKILL.md");

    // --- Parent SKILL.md contract ---

    @Test
    @DisplayName("x-review SKILL.md must declare fragment-slots with review-specialist")
    void parentSkill_declaresFragmentSlots() throws IOException {
        var content = Files.readString(PARENT_SKILL);
        assertThat(content)
                .as("x-review/SKILL.md must declare fragment-slots: [{slot: review-specialist")
                .contains("fragment-slots:")
                .contains("review-specialist");
    }

    @Test
    @DisplayName("x-review SKILL.md body must reference {{ #each fragments.review-specialist }}")
    void parentSkill_bodyReferencesFragmentSlot() throws IOException {
        var content = Files.readString(PARENT_SKILL);
        assertThat(content)
                .as("x-review SKILL.md body must reference the review-specialist slot")
                .containsAnyOf(
                        "{{ #each fragments.review-specialist }}",
                        "{{ slot: review-specialist }}");
    }

    // --- Fragment file existence ---

    @Test
    @DisplayName("All 8 review specialist fragments must exist")
    void fragments_allEightExist() {
        List<String> expected =
                List.of(
                        "qa.md",
                        "perf.md",
                        "security.md",
                        "devops.md",
                        "db.md",
                        "api.md",
                        "event.md",
                        "compliance.md");
        for (String fragment : expected) {
            assertThat(FRAGMENTS_ROOT.resolve(fragment))
                    .withFailMessage("Missing review fragment: fragments/" + fragment)
                    .exists();
        }
    }

    // --- Universal fragments (requires-capabilities: []) ---

    @Test
    @DisplayName("QA fragment must be universal (requires-capabilities: [])")
    void qaFragment_isUniversal() throws IOException {
        var content = Files.readString(FRAGMENTS_ROOT.resolve("qa.md"));
        assertThat(content).contains("requires-capabilities: []");
        assertThat(content).contains("fragment-slot:");
        assertThat(content).contains("review-specialist");
    }

    @Test
    @DisplayName("Performance fragment must be universal (requires-capabilities: [])")
    void perfFragment_isUniversal() throws IOException {
        var content = Files.readString(FRAGMENTS_ROOT.resolve("perf.md"));
        assertThat(content).contains("requires-capabilities: []");
        assertThat(content).contains("fragment-slot:");
    }

    @Test
    @DisplayName("Security fragment must be universal (requires-capabilities: [])")
    void securityFragment_isUniversal() throws IOException {
        var content = Files.readString(FRAGMENTS_ROOT.resolve("security.md"));
        assertThat(content).contains("requires-capabilities: []");
        assertThat(content).contains("fragment-slot:");
    }

    // --- Conditional fragments ---

    @Test
    @DisplayName("DevOps fragment must require infra capabilities")
    void devopsFragment_requiresInfra() throws IOException {
        var content = Files.readString(FRAGMENTS_ROOT.resolve("devops.md"));
        assertThat(content)
                .as("devops fragment must require infra.docker or infra.cicd")
                .containsAnyOf("infra.docker", "infra.cicd");
        assertThat(content).contains("fragment-slot:");
    }

    @Test
    @DisplayName("Database fragment must require data.database.* capability")
    void dbFragment_requiresDatabase() throws IOException {
        var content = Files.readString(FRAGMENTS_ROOT.resolve("db.md"));
        assertThat(content)
                .as("db fragment must require data.database.*")
                .contains("data.database.");
        assertThat(content).contains("fragment-slot:");
    }

    @Test
    @DisplayName("API fragment must require a web framework capability")
    void apiFragment_requiresWebFramework() throws IOException {
        var content = Files.readString(FRAGMENTS_ROOT.resolve("api.md"));
        assertThat(content)
                .as("api fragment must require a web capability")
                .containsAnyOf("web.spring", "web.quarkus", "web.micronaut", "web.helidon");
        assertThat(content).contains("fragment-slot:");
    }

    @Test
    @DisplayName("Event fragment must require a messaging capability")
    void eventFragment_requiresMessaging() throws IOException {
        var content = Files.readString(FRAGMENTS_ROOT.resolve("event.md"));
        assertThat(content)
                .as("event fragment must require messaging capability")
                .containsAnyOf("messaging.kafka", "messaging.rabbitmq");
        assertThat(content).contains("fragment-slot:");
    }

    @Test
    @DisplayName("Compliance fragment must require compliance.* capability")
    void complianceFragment_requiresCompliance() throws IOException {
        var content = Files.readString(FRAGMENTS_ROOT.resolve("compliance.md"));
        assertThat(content)
                .as("compliance fragment must require compliance.*")
                .contains("compliance.");
        assertThat(content).contains("fragment-slot:");
    }

    // --- Fragment ordering determinism (RULE-004) ---

    @Test
    @DisplayName("All fragments must declare fragment-order and values must be unique")
    void fragments_orderingIsDeterministicAndUnique() throws IOException {
        List<String> fragments =
                List.of(
                        "qa.md", "perf.md", "security.md", "devops.md",
                        "db.md", "api.md", "event.md", "compliance.md");
        var orders = new java.util.ArrayList<Integer>();
        for (String frag : fragments) {
            var content = Files.readString(FRAGMENTS_ROOT.resolve(frag));
            var matcher =
                    java.util.regex.Pattern.compile("fragment-order:\\s*(\\d+)")
                            .matcher(content);
            assertThat(matcher.find())
                    .withFailMessage(frag + " must declare fragment-order")
                    .isTrue();
            orders.add(Integer.parseInt(matcher.group(1)));
        }
        // All orders must be unique
        assertThat(orders)
                .as("fragment-order values must be unique across all 8 fragments")
                .doesNotHaveDuplicates();
        // Orders should be ascending (deterministic composition)
        var sorted = new java.util.ArrayList<>(orders);
        java.util.Collections.sort(sorted);
        assertThat(orders).as("fragments must be ordered ascending by fragment-order").isEqualTo(sorted);
    }
}

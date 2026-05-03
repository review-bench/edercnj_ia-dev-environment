package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Epic0075MemoryLayerSmokeIT — AI Memory Layer structural invariants")
class Epic0075MemoryLayerSmokeIT {

    private static final Path SKILL_FILE = Path.of(
            "src", "main", "resources", "targets", "claude", "skills",
            "core", "internal", "memory", "x-internal-epic-summary", "SKILL.md");

    private static final Path KNOWLEDGE_ROOT = Path.of(
            "src", "main", "resources", "targets", "claude", "knowledge",
            "governance", "ai-memory-playbook");

    private static final Path FIXTURE_DIR = Path.of(
            "src", "test", "resources", "fixtures", "memory");

    private String readSkill() throws IOException {
        assertThat(SKILL_FILE).as("SKILL.md must exist").exists();
        return Files.readString(SKILL_FILE, StandardCharsets.UTF_8);
    }

    // ── scenario 1: frontmatter contract (Rule 22 + Rule 28) ────────────────

    @Test
    @DisplayName("scenario1_skillFrontmatter_declaresInternalVisibility")
    void scenario1_skillFrontmatter_declaresInternalVisibility() throws IOException {
        String skill = readSkill();
        assertThat(skill).as("must declare visibility: internal").contains("visibility: internal");
        assertThat(skill).as("must declare user-invocable: false").contains("user-invocable: false");
        assertThat(skill).as("must declare model: haiku (Rule 23)").contains("model: haiku");
        assertThat(skill).as("must require governance.ai-memory capability")
                .contains("requires-capabilities: [governance.ai-memory]");
    }

    // ── scenario 2: INTERNAL SKILL body marker (Rule 22) ────────────────────

    @Test
    @DisplayName("scenario2_bodyMarker_present")
    void scenario2_bodyMarker_present() throws IOException {
        String skill = readSkill();
        assertThat(skill).as("must contain 🔒 INTERNAL SKILL marker")
                .contains("🔒 **INTERNAL SKILL**");
        assertThat(skill).as("must state not user-invocable")
                .contains("Not user-invocable");
    }

    // ── scenario 3: parameters table ────────────────────────────────────────

    @Test
    @DisplayName("scenario3_parametersTable_containsMandatoryFlags")
    void scenario3_parametersTable_containsMandatoryFlags() throws IOException {
        String skill = readSkill();
        assertThat(skill).as("must declare --epic-id flag").contains("--epic-id");
        assertThat(skill).as("must declare --dry-run flag").contains("--dry-run");
        assertThat(skill).as("must declare --allow-legacy-fallback flag")
                .contains("--allow-legacy-fallback");
    }

    // ── scenario 4: exit codes contract ─────────────────────────────────────

    @Test
    @DisplayName("scenario4_exitCodes_allSevenDeclared")
    void scenario4_exitCodes_allSevenDeclared() throws IOException {
        String skill = readSkill();
        assertThat(skill).as("exit 0 OK").contains("OK");
        assertThat(skill).as("exit 1 EPIC_NOT_FOUND").contains("EPIC_NOT_FOUND");
        assertThat(skill).as("exit 2 SCHEMA_VIOLATION").contains("SCHEMA_VIOLATION");
        assertThat(skill).as("exit 3 MEMORY_SUMMARY_TOO_LONG").contains("MEMORY_SUMMARY_TOO_LONG");
        assertThat(skill).as("exit 4 EXTRACTION_FAILED").contains("EXTRACTION_FAILED");
        assertThat(skill).as("exit 5 INDEX_LOCK_TIMEOUT").contains("INDEX_LOCK_TIMEOUT");
        assertThat(skill).as("exit 6 TEMPLATE_VERSION_MISMATCH").contains("TEMPLATE_VERSION_MISMATCH");
        assertThat(skill).as("exit 7 MANUAL_REFINEMENT_PRESENT").contains("MANUAL_REFINEMENT_PRESENT");
    }

    // ── scenario 5: extraction rules table ──────────────────────────────────

    @Test
    @DisplayName("scenario5_extractionRules_allSixSectionsMapped")
    void scenario5_extractionRules_allSixSectionsMapped() throws IOException {
        String skill = readSkill();
        assertThat(skill).as("Why this epic existed").contains("Why this epic existed");
        assertThat(skill).as("Hypothesis tested").contains("Hypothesis tested");
        assertThat(skill).as("Decisions taken").contains("Decisions taken");
        assertThat(skill).as("Alternatives rejected").contains("Alternatives rejected");
        assertThat(skill).as("Reusable patterns produced").contains("Reusable patterns produced");
        assertThat(skill).as("Anti-patterns observed").contains("Anti-patterns observed");
    }

    // ── scenario 6: determinism contract stated ──────────────────────────────

    @Test
    @DisplayName("scenario6_determinismContract_explicitlyStated")
    void scenario6_determinismContract_explicitlyStated() throws IOException {
        String skill = readSkill();
        assertThat(skill).as("must state determinism contract")
                .contains("Determinism contract");
        assertThat(skill).as("must reference bytewise-identical").contains("bytewise-identical");
    }

    // ── scenario 7: knowledge playbook present ───────────────────────────────

    @Test
    @DisplayName("scenario7_knowledgePlaybook_indexAndTagCatalogExist")
    void scenario7_knowledgePlaybook_indexAndTagCatalogExist() {
        Path index = KNOWLEDGE_ROOT.resolve("index.md");
        Path tagCatalog = KNOWLEDGE_ROOT.resolve("tags-catalog.md");
        assertThat(index).as("ai-memory-playbook/index.md must exist").exists();
        assertThat(tagCatalog).as("ai-memory-playbook/tags-catalog.md must exist").exists();
    }

    // ── scenario 8: fixture summaries well-formed ────────────────────────────

    @Test
    @DisplayName("scenario8_fixtureSummaries_containRequiredSections")
    void scenario8_fixtureSummaries_containRequiredSections() throws IOException {
        for (String fixtureName : new String[]{"EPIC-0064-summary.md", "EPIC-0067-summary.md"}) {
            Path fixture = FIXTURE_DIR.resolve(fixtureName);
            assertThat(fixture).as(fixtureName + " must exist").exists();
            String content = Files.readString(fixture, StandardCharsets.UTF_8);
            assertThat(content).as(fixtureName + ": Why this epic existed")
                    .contains("## Why this epic existed");
            assertThat(content).as(fixtureName + ": Hypothesis tested")
                    .contains("## Hypothesis tested");
            assertThat(content).as(fixtureName + ": Decisions taken")
                    .contains("## Decisions taken");
            assertThat(content).as(fixtureName + ": Alternatives rejected")
                    .contains("## Alternatives rejected");
            assertThat(content).as(fixtureName + ": Reusable patterns produced")
                    .contains("## Reusable patterns produced");
            assertThat(content).as(fixtureName + ": Anti-patterns observed")
                    .contains("## Anti-patterns observed");
            assertThat(content).as(fixtureName + ": Links").contains("## Links");
        }
    }

    // ── scenario 9: fixture frontmatter v3.0 fields ──────────────────────────

    @Test
    @DisplayName("scenario9_fixtureSummaries_frontmatterContainsMandatoryFields")
    void scenario9_fixtureSummaries_frontmatterContainsMandatoryFields() throws IOException {
        for (String fixtureName : new String[]{"EPIC-0064-summary.md", "EPIC-0067-summary.md"}) {
            Path fixture = FIXTURE_DIR.resolve(fixtureName);
            String content = Files.readString(fixture, StandardCharsets.UTF_8);
            assertThat(content).as(fixtureName + ": epic-id").contains("epic-id:");
            assertThat(content).as(fixtureName + ": summary-version").contains("summary-version:");
            assertThat(content).as(fixtureName + ": indexable").contains("indexable:");
            assertThat(content).as(fixtureName + ": archived").contains("archived:");
            assertThat(content).as(fixtureName + ": tags").contains("tags:");
        }
    }

    // ── scenario 10: integration note references Rule 33 ────────────────────

    @Test
    @DisplayName("scenario10_integrationNotes_referenceRule33")
    void scenario10_integrationNotes_referenceRule33() throws IOException {
        String skill = readSkill();
        assertThat(skill).as("Integration Notes must reference Rule 33").contains("Rule 33");
        assertThat(skill).as("Must state invoked by x-epic-implement")
                .contains("x-epic-implement");
    }
}

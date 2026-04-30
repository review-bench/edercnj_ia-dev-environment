package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * E2E smoke tests for EPIC-0070 (Value-Driven Templates v2).
 *
 * <p>Validates the 6 canonical scenarios introduced by stories 0070-0001 through 0070-0008:
 *
 * <ol>
 *   <li>x-internal-epic-create emits v2 value-driven template by default (story-0070-0005)
 *   <li>x-internal-story-create emits v2 value-driven template by default (story-0070-0005)
 *   <li>_TEMPLATE-ARCHITECTURE-SYSTEM.md is present and has §11 Decision Log (story-0070-0004)
 *   <li>x-arch-system-update skill is present with idempotency contract (story-0070-0006)
 *   <li>x-template-migrate skill is present with PARSER_ERROR and --dry-run support (story-0070-0007)
 *   <li>EPIC-0056 is formally marked SUPERSEDED (story-0070-0008)
 * </ol>
 */
@DisplayName("Epic0070ValueTemplatesSmokeIT — Value-Driven Templates v2 structural invariants")
class Epic0070ValueTemplatesSmokeIT {

    private static final Path SKILLS_ROOT =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "core");

    private static final Path TEMPLATES_ROOT =
            Path.of("src", "main", "resources", "shared", "templates");

    private static final Path EPIC_0056 =
            Path.of(
                    "ai",
                    "epics",
                    "epic-0056-ra9-planning-templates",
                    "epic-0056.md");

    // ─── Scenario 1: x-internal-epic-create emits v2 by default ──────────────

    @Test
    @DisplayName("scenario1_epicCreate_emitsV2ByDefault")
    void scenario1_epicCreate_emitsV2ByDefault() throws IOException {
        Path skillFile =
                SKILLS_ROOT
                        .resolve("internal/plan/x-internal-epic-create/SKILL.md")
                        .toAbsolutePath();

        assertThat(skillFile).exists();
        String content = Files.readString(skillFile, StandardCharsets.UTF_8);

        assertThat(content)
                .as("x-internal-epic-create MUST reference v2 value-driven template")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("v2 value-driven"),
                        c -> assertThat(c).contains("EPIC-0070"),
                        c -> assertThat(c).contains("v2 sections"));

        assertThat(content)
                .as("x-internal-epic-create MUST declare --legacy-template-v1 flag")
                .contains("--legacy-template-v1");

        assertThat(content)
                .as("x-internal-epic-create MUST have deprecation warning text")
                .contains("DEPRECATED");
    }

    // ─── Scenario 2: x-internal-story-create emits v2 by default ─────────────

    @Test
    @DisplayName("scenario2_storyCreate_emitsV2ByDefault")
    void scenario2_storyCreate_emitsV2ByDefault() throws IOException {
        Path skillFile =
                SKILLS_ROOT
                        .resolve("internal/plan/x-internal-story-create/SKILL.md")
                        .toAbsolutePath();

        assertThat(skillFile).exists();
        String content = Files.readString(skillFile, StandardCharsets.UTF_8);

        assertThat(content)
                .as("x-internal-story-create MUST reference v2 value-driven template")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("v2 value-driven"),
                        c -> assertThat(c).contains("EPIC-0070"),
                        c -> assertThat(c).contains("v2 sections"));

        assertThat(content)
                .as("x-internal-story-create MUST declare --legacy-template-v1 flag")
                .contains("--legacy-template-v1");
    }

    // ─── Scenario 3: _TEMPLATE-ARCHITECTURE-SYSTEM.md present with §11 ───────

    @Test
    @DisplayName("scenario3_systemArchTemplate_presentWithDecisionLog")
    void scenario3_systemArchTemplate_presentWithDecisionLog() throws IOException {
        Path template =
                TEMPLATES_ROOT.resolve("_TEMPLATE-ARCHITECTURE-SYSTEM.md").toAbsolutePath();

        assertThat(template)
                .as("_TEMPLATE-ARCHITECTURE-SYSTEM.md must exist (story-0070-0004)")
                .exists();

        String content = Files.readString(template, StandardCharsets.UTF_8);

        assertThat(content)
                .as("_TEMPLATE-ARCHITECTURE-SYSTEM.md MUST have §11 Decision Log (story-0070-0004)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("Decision Log do Sistema"),
                        c -> assertThat(c).contains("## 11."),
                        c -> assertThat(c).contains("Decision Log"));
    }

    // ─── Scenario 4: x-arch-system-update present with idempotency ───────────

    @Test
    @DisplayName("scenario4_archSystemUpdate_presentWithIdempotencyContract")
    void scenario4_archSystemUpdate_presentWithIdempotencyContract() throws IOException {
        Path skillFile =
                SKILLS_ROOT
                        .resolve("plan/x-arch-system-update/SKILL.md")
                        .toAbsolutePath();

        assertThat(skillFile)
                .as("x-arch-system-update SKILL.md must exist (story-0070-0006)")
                .exists();

        String content = Files.readString(skillFile, StandardCharsets.UTF_8);

        assertThat(content)
                .as("x-arch-system-update MUST document idempotency contract")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("Idempotency"),
                        c -> assertThat(c).contains("idempoten"));

        assertThat(content)
                .as("x-arch-system-update MUST document SYSTEM_MD_MISSING error")
                .contains("SYSTEM_MD_MISSING");
    }

    // ─── Scenario 5: x-template-migrate present with PARSER_ERROR + dry-run ──

    @Test
    @DisplayName("scenario5_templateMigrate_presentWithParserErrorAndDryRun")
    void scenario5_templateMigrate_presentWithParserErrorAndDryRun() throws IOException {
        Path skillFile =
                SKILLS_ROOT
                        .resolve("plan/x-template-migrate/SKILL.md")
                        .toAbsolutePath();

        assertThat(skillFile)
                .as("x-template-migrate SKILL.md must exist (story-0070-0007)")
                .exists();

        String content = Files.readString(skillFile, StandardCharsets.UTF_8);

        assertThat(content)
                .as("x-template-migrate MUST document PARSER_ERROR with atomic abort")
                .contains("PARSER_ERROR");

        assertThat(content)
                .as("x-template-migrate MUST support --dry-run mode")
                .contains("--dry-run");

        assertThat(content)
                .as("x-template-migrate MUST handle already-v2 epics (degenerate)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("already in v2"),
                        c -> assertThat(c).contains("already v2"));
    }

    // ─── Scenario 6: EPIC-0056 marked SUPERSEDED ──────────────────────────────

    @Test
    @DisplayName("scenario6_epic0056_markedSuperseded")
    void scenario6_epic0056_markedSuperseded() throws IOException {
        assertThat(EPIC_0056.toAbsolutePath())
                .as("epic-0056.md must exist (not deleted — history preserved)")
                .exists();

        String content = Files.readString(EPIC_0056.toAbsolutePath(), StandardCharsets.UTF_8);

        assertThat(content)
                .as("EPIC-0056 MUST be marked SUPERSEDED by EPIC-0070 (story-0070-0008)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("SUPERSEDED"),
                        c -> assertThat(c).contains("superseded"));

        assertThat(content)
                .as("EPIC-0056 MUST reference EPIC-0070 as the successor")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("EPIC-0070"),
                        c -> assertThat(c).contains("epic-0070"));
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private static Path repoRoot() {
        return Path.of("").toAbsolutePath();
    }
}

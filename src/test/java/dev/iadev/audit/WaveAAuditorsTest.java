package dev.iadev.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for Wave A auditors (5 markdown audits). Creates fixture files in @TempDir to avoid
 * classpath resource resolution issues.
 */
@DisplayName("Wave A Auditors")
class WaveAAuditorsTest {

    @TempDir Path tempDir;

    private Path writeSkill(Path dir, String filename, String content) throws IOException {
        Files.createDirectories(dir);
        Path file = dir.resolve(filename);
        Files.writeString(file, content);
        return file;
    }

    @Nested
    @DisplayName("ModelSelectionAuditor")
    class ModelSelectionAuditorTest {

        private final ModelSelectionAuditor auditor = new ModelSelectionAuditor();

        @Test
        void validSkillWithModel_returnsOk() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-implement\nmodel: sonnet\nuser-invocable: true\n---\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void invalidSkillMissingModel_returnsViolation() throws IOException {
            Path skillDir = tempDir.resolve("x-bad-skill");
            writeSkill(skillDir, "SKILL.md", "---\nname: x-bad-skill\nuser-invocable: true\n---\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.exitName()).isEqualTo("MODEL_SELECTION_VIOLATION");
            assertThat(result.violations()).isNotEmpty();
            assertThat(result.violations().get(0).rule()).isEqualTo("MISSING_MODEL");
        }

        @Test
        void notUserInvocable_notCheckedForModel() throws IOException {
            Path skillDir = tempDir.resolve("x-internal-foo");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-internal-foo\nvisibility: internal\nuser-invocable: false\n---\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void name_returnsModelSelection() {
            assertThat(auditor.name()).isEqualTo("model-selection");
        }

        @Test
        void bashEquivalentTemplate_pointsToScript() {
            assertThat(auditor.bashEquivalentTemplate().toString())
                    .contains("audit-model-selection.sh.tpl");
        }
    }

    @Nested
    @DisplayName("SkillVisibilityAuditor")
    class SkillVisibilityAuditorTest {

        private final SkillVisibilityAuditor auditor = new SkillVisibilityAuditor();

        @Test
        void validInternalSkill_returnsOk() throws IOException {
            Path skillDir = tempDir.resolve("x-internal-foo");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-internal-foo\nvisibility: internal\nuser-invocable: false\n---\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void internalSkillMissingVisibility_returnsViolation() throws IOException {
            Path skillDir = tempDir.resolve("x-internal-missing");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-internal-missing\nuser-invocable: false\n---\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("INTERNAL_MISSING_FRONTMATTER");
        }

        @Test
        void publicSkillWithoutVisibility_isOk() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-implement\nmodel: sonnet\nuser-invocable: true\n---\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void name_returnsSkillVisibility() {
            assertThat(auditor.name()).isEqualTo("skill-visibility");
        }
    }

    @Nested
    @DisplayName("BypassFlagsAuditor")
    class BypassFlagsAuditorTest {

        private final BypassFlagsAuditor auditor = new BypassFlagsAuditor();

        @Test
        void bypassFlagInRecovery_returnsOk() throws IOException {
            Path skillDir = tempDir.resolve("x-story-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-story-implement\n---\n## Phase 2\n\nNormal flow.\n\n## Recovery\n\nUse --no-ci-watch here.\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void bypassFlagOutsideRecovery_returnsViolation() throws IOException {
            Path skillDir = tempDir.resolve("x-bad-story");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-bad-story\n---\n## Phase 2\n\nUse --no-ci-watch in normal flow (VIOLATION)\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("BYPASS_FLAG_OUTSIDE_RECOVERY");
        }

        @Test
        void noBypassFlag_returnsOk() throws IOException {
            Path skillDir = tempDir.resolve("x-clean");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-clean\n---\n## Phase 2\n\nNormal execution only.\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void name_returnsBypassFlags() {
            assertThat(auditor.name()).isEqualTo("bypass-flags");
        }
    }

    @Nested
    @DisplayName("TaskHierarchyAuditor")
    class TaskHierarchyAuditorTest {

        private final TaskHierarchyAuditor auditor = new TaskHierarchyAuditor();

        @Test
        void orchestratorWithTaskCreate_returnsOk() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-implement\n---\n## Phase 1 - Plan\n\nTaskCreate(subject: \"Phase 1\")\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void orchestratorWithoutTaskCreate_returnsViolation() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-implement\n---\n## Phase 1 - Plan\n\nNo task create here.\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("MISSING_TASK_CREATE");
        }

        @Test
        void nonOrchestratorWithoutTaskCreate_isIgnored() throws IOException {
            Path skillDir = tempDir.resolve("x-review-qa");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-review-qa\n---\n## Phase 1\n\nNo TaskCreate — not an orchestrator.\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void name_returnsTaskHierarchy() {
            assertThat(auditor.name()).isEqualTo("task-hierarchy");
        }

        @Test
        void bashEquivalentTemplate_pointsToScript() {
            assertThat(auditor.bashEquivalentTemplate().toString())
                    .contains("audit-task-hierarchy.sh.tpl");
        }

        @Test
        void twoPhasesFirstLacksTaskCreate_recordsViolationAtTransition() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-implement\n---\n"
                            + "## Phase 1 - Plan\n\nNo TaskCreate here.\n"
                            + "## Phase 2 - Execute\n\nTaskCreate(subject: \"p2\")\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("MISSING_TASK_CREATE");
        }

        @Test
        void phaseWithExemptionMarker_noViolation() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-implement\n---\n"
                            + "<!-- phase-no-gate: read-only -->\n"
                            + "## Phase 0 - Context\n\nNo TaskCreate — exempt.\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void taskCreateBeforeFirstPhaseHeader_notCounted() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-implement\n---\n"
                            + "TaskCreate(subject: \"pre-phase\")\n"
                            + "## Phase 1 - Plan\n\nNo TaskCreate in phase body.\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            // TaskCreate before any phase header is not counted; phase lacks TaskCreate → violation
            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("MISSING_TASK_CREATE");
        }

        @Test
        void orchestratorWithXStoryImplement_isDetected() throws IOException {
            Path skillDir = tempDir.resolve("x-story-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-story-implement\n---\n"
                            + "## Phase 1 - Plan\n\nTaskCreate(subject: \"p1\")\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void unreadableFile_ignoredByIsOrchestrator() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement-unreadable");
            Path skillFile =
                    writeSkill(
                            skillDir,
                            "SKILL.md",
                            "## Phase 1\nTaskCreate(subject: \"p1\")\n");
            boolean changed = skillFile.toFile().setReadable(false);
            assumeTrue(changed, "Cannot make file unreadable on this platform");
            try {
                AuditCorpus corpus = new AuditCorpus(tempDir);
                AuditResult result = auditor.audit(corpus);
                assertThat(result.exitCode()).isEqualTo(0);
            } finally {
                skillFile.toFile().setReadable(true);
            }
        }
    }

    @Nested
    @DisplayName("PhaseGatesAuditor")
    class PhaseGatesAuditorTest {

        private final PhaseGatesAuditor auditor = new PhaseGatesAuditor();

        @Test
        void phaseWithGate_returnsOk() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-implement\n---\n## Phase 1 - Plan\n\nTaskCreate(subject: \"Phase 1\")\nx-internal-phase-gate --mode pre\nx-internal-phase-gate --mode post\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void phaseWithTaskButNoGate_returnsViolation() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-implement\n---\n## Phase 1 - Plan\n\nTaskCreate(subject: \"Phase 1\")\n# No gate here\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("MISSING_PHASE_GATE");
        }

        @Test
        void phaseWithNoGateExemption_returnsOk() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-exempt");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-exempt\n---\n<!-- phase-no-gate: prose only -->\n## Phase 1 - Prose\n\nTaskCreate(subject: \"Phase 1\")\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void multiplePhases_firstMissingGate_lastOk_returnsViolation() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-multi");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-multi\n---\n"
                            + "## Phase 1\n\nTaskCreate(subject: \"Phase 1\")\n# no gate\n"
                            + "## Phase 2\n\nTaskCreate(subject: \"Phase 2\")\nx-internal-phase-gate --mode post\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations()).hasSize(1);
        }

        @Test
        void emptyCorpus_returnsOk() {
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void skillWithTaskCreateButNoPhaseHeader_notOrchestrator() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-nophase");
            writeSkill(skillDir, "SKILL.md",
                    "---\nname: x-epic-nophase\n---\nTaskCreate(subject: \"no phase\")\nNo phase header here.\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void lastPhaseNoTaskCreate_isOk() throws IOException {
            // Last phase has no TaskCreate — not a violation (only phases WITH TaskCreate need a gate)
            Path skillDir = tempDir.resolve("x-epic-notask");
            writeSkill(skillDir, "SKILL.md",
                    "---\nname: x-epic-notask\n---\n"
                    + "TaskCreate(subject: \"pre-phase\")\n"
                    + "## Phase 1 - Prose\n\nNo task here, no gate needed.\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void twoPhases_firstNoTask_secondHasTaskAndGate_isOk() throws IOException {
            // Phase 1 has no TaskCreate (not a violation), Phase 2 has task + gate
            Path skillDir = tempDir.resolve("x-epic-firstnotask");
            writeSkill(skillDir, "SKILL.md",
                    "---\nname: x-epic-firstnotask\n---\n"
                    + "## Phase 1 - Intro\n\nNo task here.\n"
                    + "## Phase 2 - Main\n\nTaskCreate(subject: \"Phase 2\")\n"
                    + "x-internal-phase-gate --mode post\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void twoPhases_firstExempt_secondHasTaskAndGate_isOk() throws IOException {
            // Phase 1 has exemption + task (no gate needed), Phase 2 has task + gate
            Path skillDir = tempDir.resolve("x-epic-exemptfirst");
            writeSkill(skillDir, "SKILL.md",
                    "---\nname: x-epic-exemptfirst\n---\n"
                    + "<!-- phase-no-gate: prose only -->\n"
                    + "## Phase 1 - Exempt\n\nTaskCreate(subject: \"Phase 1\")\n"
                    + "## Phase 2 - Main\n\nTaskCreate(subject: \"Phase 2\")\n"
                    + "x-internal-phase-gate --mode post\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void name_returnsPhaseGates() {
            assertThat(auditor.name()).isEqualTo("phase-gates");
        }

        @Test
        void bashEquivalentTemplate_pointsToScript() {
            assertThat(auditor.bashEquivalentTemplate().toString())
                    .contains("audit-phase-gates.sh.tpl");
        }

        @Test
        void twoPhasesFirstHasTaskButNoGate_recordsViolationAtTransition() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-implement\n---\n"
                            + "## Phase 1 - Plan\n\nTaskCreate(subject: \"p1\")\n# no gate\n"
                            + "## Phase 2 - Execute\n\nTaskCreate(subject: \"p2\")\n"
                            + "x-internal-phase-gate --mode post\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            // Phase 1 lacks gate: violation recorded when Phase 2 header is encountered
            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations()).hasSize(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("MISSING_PHASE_GATE");
        }

        @Test
        void phaseWithExemptionMarker_noViolation() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-implement\n---\n"
                            + "<!-- phase-no-gate: read-only pre-check -->\n"
                            + "## Phase 0 - Context\n\nTaskCreate(subject: \"p0\")\n# no gate needed\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void twoPhasesFirstPhaseExempted_noIntermediateViolation() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-implement\n---\n"
                            + "<!-- phase-no-gate: pre-check -->\n"
                            + "## Phase 0 - Pre-check\n\nTaskCreate(subject: \"p0\")\n# exempt\n"
                            + "## Phase 1 - Plan\n\nTaskCreate(subject: \"p1\")\n"
                            + "x-internal-phase-gate --mode post\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            // Phase 0 is exempt — no violation on transition to Phase 1
            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void skillWithTaskCreateButNoPhaseHeader_filteredOut() throws IOException {
            Path skillDir = tempDir.resolve("x-fake-orchestrator");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-fake-orchestrator\n---\n\nTaskCreate(subject: \"no phase\")\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void unreadableFile_ignoredByIsOrchestrator() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-implement-unreadable");
            Path skillFile =
                    writeSkill(
                            skillDir,
                            "SKILL.md",
                            "## Phase 1\nTaskCreate(subject: \"p1\")\n");
            boolean changed = skillFile.toFile().setReadable(false);
            assumeTrue(changed, "Cannot make file unreadable on this platform");
            try {
                AuditCorpus corpus = new AuditCorpus(tempDir);
                AuditResult result = auditor.audit(corpus);
                assertThat(result.exitCode()).isEqualTo(0);
            } finally {
                skillFile.toFile().setReadable(true);
            }
        }
    }

    @Nested
    @DisplayName("TaskHierarchyAuditor — exemption and multi-phase")
    class TaskHierarchyAuditorExtraTest {

        private final TaskHierarchyAuditor auditor = new TaskHierarchyAuditor();

        @Test
        void phaseWithNoGateExemption_isNotViolation() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-th-exempt");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-th-exempt\n---\nx-epic-implement\n"
                            + "<!-- phase-no-gate: prose only -->\n## Phase 1 - Prose\n\nNo task here.\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(0);
        }

        @Test
        void multiplePhasesSecondMissingTaskCreate_returnsViolation() throws IOException {
            Path skillDir = tempDir.resolve("x-epic-th-multi");
            writeSkill(
                    skillDir,
                    "SKILL.md",
                    "---\nname: x-epic-th-multi\n---\nx-story-implement\n"
                            + "## Phase 1\n\nTaskCreate(subject: \"Phase 1\")\n"
                            + "## Phase 2\n\nNo task create here.\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("MISSING_TASK_CREATE");
        }

        @Test
        void xTaskImplementReference_isOrchestrator() throws IOException {
            // Skill referencing x-task-implement is treated as orchestrator
            Path skillDir = tempDir.resolve("x-epic-taskimpl");
            writeSkill(skillDir, "SKILL.md",
                    "---\nname: x-epic-taskimpl\n---\nx-task-implement\n"
                    + "## Phase 1\n\nTaskCreate(subject: \"Phase 1\")\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void taskCreateBeforePhase_doesNotCount_violationFired() throws IOException {
            // TaskCreate before any ## Phase is not counted → Phase 1 lacks TaskCreate → violation
            Path skillDir = tempDir.resolve("x-epic-pretask");
            writeSkill(skillDir, "SKILL.md",
                    "---\nname: x-epic-pretask\n---\nx-epic-implement\n"
                    + "TaskCreate(subject: \"Pre-phase\")\n"
                    + "## Phase 1\n\nNo task inside phase.\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("MISSING_TASK_CREATE");
        }

        @Test
        void firstPhaseNoTask_secondHasTask_firstIsViolation() throws IOException {
            // Phase 1 no task → violation detected at Phase 2 transition; Phase 2 has task → ok at end
            Path skillDir = tempDir.resolve("x-epic-firstnotask");
            writeSkill(skillDir, "SKILL.md",
                    "---\nname: x-epic-firstnotask\n---\nx-story-implement\n"
                    + "## Phase 1\n\nNo task here.\n"
                    + "## Phase 2\n\nTaskCreate(subject: \"Phase 2\")\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations()).hasSize(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("MISSING_TASK_CREATE");
        }

        @Test
        void orchestratorWithNoPhaseHeaders_isOk() throws IOException {
            // File matches isOrchestrator but has no ## Phase headers — no violations
            Path skillDir = tempDir.resolve("x-epic-nophase");
            writeSkill(skillDir, "SKILL.md",
                    "---\nname: x-epic-nophase\n---\nx-epic-implement\nSome content without phase headers.\n");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }
    }
}

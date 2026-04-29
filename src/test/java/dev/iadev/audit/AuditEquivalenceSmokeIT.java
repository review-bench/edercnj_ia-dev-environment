package dev.iadev.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Structural equivalence smoke test (RULE-004 — Bash↔Java Equivalência).
 *
 * <p>Validates that each Java auditor:
 *
 * <ol>
 *   <li>Has a matching bash template file at its declared {@link Auditor#bashEquivalentTemplate()}
 *       path.
 *   <li>Returns exit code 0 on clean fixtures and exit code 1 on violation fixtures.
 *   <li>Has a non-null, non-empty {@link Auditor#name()}.
 * </ol>
 *
 * <p>Note: Full bash-execution equivalence is deferred until story-0061-0005 removes the templates
 * from the root scripts/ directory and makes them directly executable. The structural check here
 * verifies the contract shape (RULE-004 spirit).
 */
@DisplayName("AuditEquivalenceSmokeIT — bash↔Java structural parity")
class AuditEquivalenceSmokeIT {

    @TempDir Path tempDir;

    private static final Path REPO_ROOT = Path.of(System.getProperty("user.dir"));

    static List<Auditor> allAuditors() {
        return List.of(
                new ModelSelectionAuditor(),
                new SkillVisibilityAuditor(),
                new BypassFlagsAuditor(),
                new TaskHierarchyAuditor(),
                new PhaseGatesAuditor(),
                new FlowVersionAuditor(),
                new EpicBranchesAuditor(),
                new ExecutionIntegrityAuditor());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allAuditors")
    @DisplayName("each auditor has a matching bash template file")
    void bashTemplateExists(Auditor auditor) {
        Path templatePath = REPO_ROOT.resolve(auditor.bashEquivalentTemplate());

        assertThat(templatePath.toFile())
                .as(
                        "Bash template for auditor '%s' should exist at: %s",
                        auditor.name(), templatePath)
                .exists();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allAuditors")
    @DisplayName("each auditor has a non-empty name")
    void auditorHasName(Auditor auditor) {
        assertThat(auditor.name()).as("Auditor name must not be blank").isNotBlank();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allAuditors")
    @DisplayName("each auditor returns OK on empty corpus")
    void emptyCorpusReturnsOk(Auditor auditor) {
        AuditCorpus emptyCorpus = new AuditCorpus(tempDir);

        AuditResult result = auditor.audit(emptyCorpus);

        assertThat(result.exitCode())
                .as("Auditor '%s' should return 0 (OK) on empty corpus", auditor.name())
                .isEqualTo(0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allAuditors")
    @DisplayName("each auditor returns violation on pre-built bad fixture")
    void badFixtureReturnsViolation(Auditor auditor) throws IOException {
        Path badFixture = buildBadFixtureFor(auditor.name());
        AuditCorpus corpus = new AuditCorpus(badFixture);

        AuditResult result = auditor.audit(corpus);

        assertThat(result.exitCode())
                .as(
                        "Auditor '%s' should return 1 (violation) on bad fixture, got: %s",
                        auditor.name(), result)
                .isEqualTo(1);
        assertThat(result.violations())
                .as("Auditor '%s' should report at least one violation", auditor.name())
                .isNotEmpty();
    }

    private Path buildBadFixtureFor(String auditName) throws IOException {
        Path fixtureRoot = tempDir.resolve(auditName + "-bad");
        Files.createDirectories(fixtureRoot);
        switch (auditName) {
            case "model-selection" -> {
                Path d = fixtureRoot.resolve("x-bad-skill");
                Files.createDirectories(d);
                Files.writeString(
                        d.resolve("SKILL.md"), "---\nname: x-bad\nuser-invocable: true\n---\n");
            }
            case "skill-visibility" -> {
                Path d = fixtureRoot.resolve("x-internal-no-vis");
                Files.createDirectories(d);
                Files.writeString(
                        d.resolve("SKILL.md"),
                        "---\nname: x-internal-no-vis\nuser-invocable: false\n---\n");
            }
            case "bypass-flags" -> {
                Path d = fixtureRoot.resolve("x-bad-skill");
                Files.createDirectories(d);
                Files.writeString(
                        d.resolve("SKILL.md"),
                        "---\nname: x-bad\n---\n## Phase 2\n\nUse --no-ci-watch here.\n");
            }
            case "task-hierarchy" -> {
                Path d = fixtureRoot.resolve("x-epic-implement");
                Files.createDirectories(d);
                Files.writeString(
                        d.resolve("SKILL.md"),
                        "---\nname: x-epic-implement\n---\n## Phase 1 - Plan\n\nNo TaskCreate.\n");
            }
            case "phase-gates" -> {
                Path d = fixtureRoot.resolve("x-epic-implement");
                Files.createDirectories(d);
                Files.writeString(
                        d.resolve("SKILL.md"),
                        "---\nname: x-epic-implement\n---\n## Phase 1\n\nTaskCreate(subject: \"x\")\n# no gate\n");
            }
            case "flow-version" -> {
                Path d = fixtureRoot.resolve("ai/epics/epic-0099/");
                Files.createDirectories(d);
                Files.writeString(
                        d.resolve("execution-state.json"),
                        "{\"flowVersion\": \"3\", \"epicId\": \"EPIC-0099\"}");
            }
            case "epic-branches" -> {
                Path d = fixtureRoot.resolve("ai/epics/epic-9999/");
                Files.createDirectories(d);
                Files.writeString(
                        d.resolve("execution-state.json"),
                        "{\"flowVersion\": \"2\", \"epicId\": \"EPIC-9999\"}");
            }
            case "execution-integrity" -> {
                Path plans = fixtureRoot.resolve("ai/epics/epic-0099/");
                Files.createDirectories(plans);
                Files.writeString(
                        plans.resolve("execution-state.json"),
                        "{\"flowVersion\": \"2\", \"epicId\": \"EPIC-0099\","
                                + "\"storyStatuses\":{\"story-0099-0001\":{\"status\":\"COMPLETE\","
                                + "\"prMergeStatus\":\"MERGED\"}}}");
            }
            default -> throw new IllegalArgumentException("Unknown audit: " + auditName);
        }
        return fixtureRoot;
    }
}

package dev.iadevkit.command;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadevkit.IaDevKitApplication;
import dev.iadevkit.audit.AuditPolicy;
import dev.iadevkit.audit.AuditRule;
import dev.iadevkit.audit.AuditRunner;
import dev.iadevkit.audit.AuditTarget;
import dev.iadevkit.audit.AuditViolation;
import dev.iadevkit.audit.ContainsAllRule;
import dev.iadevkit.audit.ContainsNoneRule;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class RefinementContractAuditTest {

    private static final String[] EPIC_REQUIRED_SIGNALS = {
        "## 2",
        "persona",
        "OKR/KPI",
        "measurement method",
        "alternatives",
        "risks",
        "validation error"
    };

    private static final String[] STORY_REQUIRED_SIGNALS = {
        "## 1",
        "## 2",
        "persona",
        "scenario",
        "Gherkin",
        "performance/SLA",
        "typed contracts",
        "validation error"
    };

    private static final String[] FORBIDDEN_OMISSION_SIGNALS = {
        "omit sections `2`, `4`, and `8`",
        "Sections 2/4/8 are omitted",
        "Do not render** sections `## 2`, `## 4`, `## 8`",
        "Do not render** `## 2. Persona & Cenário`, `## 4. AC",
        "intentionally omit `## 2. Persona & Cenário`",
        "intentionally omitting the generic sections `4` and `8`"
    };

    private final AuditRunner auditRunner = new AuditRunner();

    @Test
    void sourceSkills_keepEpicCreationAlignedWithRefinementGate() throws IOException {
        List<AuditViolation> violations =
                auditRunner.run(
                        List.of(
                                resourceTarget(
                                        "epic-public-skill",
                                        "claude/skills/x-epic-create/SKILL.md"),
                                resourceTarget(
                                        "epic-internal-skill",
                                        "claude/skills/x-internal-create-epic/SKILL.md")),
                        refinementContractPolicy());

        assertNoViolations(violations);
    }

    @Test
    void sourceSkills_keepStoryCreationAlignedWithRefinementGate() throws IOException {
        List<AuditViolation> violations =
                auditRunner.run(
                        List.of(
                                resourceTarget(
                                        "story-public-skill",
                                        "claude/skills/x-story-create/SKILL.md"),
                                resourceTarget(
                                        "story-internal-skill",
                                        "claude/skills/x-internal-create-story/SKILL.md"),
                                resourceTarget(
                                        "story-full-protocol",
                                        "claude/skills/x-internal-create-story/references/full-protocol.md")),
                        refinementContractPolicy());

        assertNoViolations(violations);
    }

    @Test
    void epicTemplate_exposesMeasurementMethodForOkrs() throws IOException {
        List<AuditViolation> violations =
                auditRunner.run(
                        List.of(
                                resourceTarget(
                                        "epic-template",
                                        "claude/templates/_TEMPLATE-EPIC.md")),
                        refinementContractPolicy());

        assertNoViolations(violations);
    }

    @Test
    void generate_command_preservesRefinementContractInOutput(@TempDir Path tmpDir)
            throws IOException {
        int exit =
                new CommandLine(new IaDevKitApplication())
                        .execute("generate", "--output", tmpDir.toString(), "--force");

        assertThat(exit).isZero();

        List<AuditViolation> violations =
                auditRunner.run(
                        List.of(
                                fileTarget(
                                        "epic-public-skill",
                                        tmpDir.resolve(".claude/skills/x-epic-create/SKILL.md")),
                                fileTarget(
                                        "story-public-skill",
                                        tmpDir.resolve(".claude/skills/x-story-create/SKILL.md")),
                                fileTarget(
                                        "epic-template",
                                        tmpDir.resolve(".claude/templates/_TEMPLATE-EPIC.md"))),
                        refinementContractPolicy());

        assertNoViolations(violations);
    }

    private AuditPolicy refinementContractPolicy() {
        return new AuditPolicy(
                Map.of(
                        "epic-public-skill",
                        List.of(
                                requireAll(
                                        "EPIC_PUBLIC_REQUIRED_SIGNALS",
                                        "Epic public wrapper must require refinement-critical content",
                                        EPIC_REQUIRED_SIGNALS),
                                forbidAny(
                                        "EPIC_PUBLIC_FORBIDDEN_OMISSION",
                                        "Epic public wrapper must not reintroduce omitted sections",
                                        FORBIDDEN_OMISSION_SIGNALS)),
                        "epic-internal-skill",
                        List.of(
                                requireAll(
                                        "EPIC_INTERNAL_REQUIRED_SIGNALS",
                                        "Epic internal generator must define evidence derivation and completeness gates",
                                        new String[] {
                                            "Feature-derived epics MUST still render all refinement-critical sections",
                                            "validate the generated epic against",
                                            "measurement method",
                                            "priority order",
                                            "RNFs never replace persona"
                                        }),
                                forbidAny(
                                        "EPIC_INTERNAL_FORBIDDEN_OMISSION",
                                        "Epic internal generator must not reintroduce omitted sections",
                                        FORBIDDEN_OMISSION_SIGNALS)),
                        "story-public-skill",
                        List.of(
                                requireAll(
                                        "STORY_PUBLIC_REQUIRED_SIGNALS",
                                        "Story public wrapper must require refinement-critical content",
                                        STORY_REQUIRED_SIGNALS),
                                forbidAny(
                                        "STORY_PUBLIC_FORBIDDEN_OMISSION",
                                        "Story public wrapper must not reintroduce omitted sections",
                                        FORBIDDEN_OMISSION_SIGNALS)),
                        "story-internal-skill",
                        List.of(
                                requireAll(
                                        "STORY_INTERNAL_REQUIRED_SIGNALS",
                                        "Story internal generator must define evidence derivation and completeness gates",
                                        new String[] {
                                            "must still render those sections with concrete content",
                                            "Refinement-critical section missing during generation",
                                            "priority order",
                                            "RNFs never replace persona"
                                        }),
                                forbidAny(
                                        "STORY_INTERNAL_FORBIDDEN_OMISSION",
                                        "Story internal generator must not reintroduce omitted sections",
                                        FORBIDDEN_OMISSION_SIGNALS)),
                        "story-full-protocol",
                        List.of(
                                requireAll(
                                        "STORY_PROTOCOL_REQUIRED_SIGNALS",
                                        "Story full protocol must preserve evidence derivation guidance",
                                        new String[] {
                                            "still populate `## 2. Persona & Cenário`, `## 4. AC (...)`, and `## 8. Decision Rationale`",
                                            "validate the refinement-critical dimensions",
                                            "Evidence derivation order",
                                            "Never use RNFs as a substitute"
                                        }),
                                forbidAny(
                                        "STORY_PROTOCOL_FORBIDDEN_OMISSION",
                                        "Story full protocol must not reintroduce omitted sections",
                                        FORBIDDEN_OMISSION_SIGNALS)),
                        "epic-template",
                        List.of(
                                requireAll(
                                        "EPIC_TEMPLATE_REQUIRED_SIGNALS",
                                        "Epic template must expose measurement method for OKRs",
                                        new String[] {
                                            "método de medição",
                                            "| Objetivo | Key Result | Métrica | Método de Medição | Valor Atual | Meta | Prazo |"
                                        }))));
    }

    private AuditRule requireAll(String code, String description, String[] signals) {
        return new ContainsAllRule(code, description, List.of(signals));
    }

    private AuditRule forbidAny(String code, String description, String[] signals) {
        return new ContainsNoneRule(code, description, List.of(signals));
    }

    private AuditTarget resourceTarget(String id, String resourcePath) throws IOException {
        return new AuditTarget(id, readResource(resourcePath));
    }

    private AuditTarget fileTarget(String id, Path filePath) throws IOException {
        return new AuditTarget(id, Files.readString(filePath, StandardCharsets.UTF_8));
    }

    private void assertNoViolations(List<AuditViolation> violations) {
        assertThat(violations)
                .withFailMessage(() -> formatViolations(violations))
                .isEmpty();
    }

    private String formatViolations(List<AuditViolation> violations) {
        StringBuilder builder = new StringBuilder("Refinement contract violations:");
        for (AuditViolation violation : violations) {
            builder.append(System.lineSeparator())
                    .append("- [")
                    .append(violation.ruleCode())
                    .append("] ")
                    .append(violation.targetId())
                    .append(": ")
                    .append(violation.message());
        }
        return builder.toString();
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream input =
                getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(input).as("resource %s", resourcePath).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

package dev.iadevkit.command;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadevkit.IaDevKitApplication;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void sourceSkills_keepEpicCreationAlignedWithRefinementGate() throws IOException {
        String publicSkill = readResource("claude/skills/x-epic-create/SKILL.md");
        String internalSkill = readResource("claude/skills/x-internal-create-epic/SKILL.md");

        assertContainsAll(publicSkill, EPIC_REQUIRED_SIGNALS);
        assertContainsAll(
                internalSkill,
                new String[] {
                    "Feature-derived epics MUST still render all refinement-critical sections",
                    "validate the generated epic against",
                    "measurement method",
                    "priority order",
                    "RNFs never replace persona"
                });
        assertContainsNone(publicSkill, FORBIDDEN_OMISSION_SIGNALS);
        assertContainsNone(internalSkill, FORBIDDEN_OMISSION_SIGNALS);
    }

    @Test
    void sourceSkills_keepStoryCreationAlignedWithRefinementGate() throws IOException {
        String publicSkill = readResource("claude/skills/x-story-create/SKILL.md");
        String internalSkill = readResource("claude/skills/x-internal-create-story/SKILL.md");
        String fullProtocol =
                readResource("claude/skills/x-internal-create-story/references/full-protocol.md");

        assertContainsAll(publicSkill, STORY_REQUIRED_SIGNALS);
        assertContainsAll(
                internalSkill,
                new String[] {
                    "must still render those sections with concrete content",
                    "Refinement-critical section missing during generation",
                    "priority order",
                    "RNFs never replace persona"
                });
        assertContainsAll(
                fullProtocol,
                new String[] {
                    "still populate `## 2. Persona & Cenário`, `## 4. AC (...)`, and `## 8. Decision Rationale`",
                    "validate the refinement-critical dimensions",
                    "Evidence derivation order",
                    "Never use RNFs as a substitute"
                });
        assertContainsNone(publicSkill, FORBIDDEN_OMISSION_SIGNALS);
        assertContainsNone(internalSkill, FORBIDDEN_OMISSION_SIGNALS);
        assertContainsNone(fullProtocol, FORBIDDEN_OMISSION_SIGNALS);
    }

    @Test
    void epicTemplate_exposesMeasurementMethodForOkrs() throws IOException {
        String template = readResource("claude/templates/_TEMPLATE-EPIC.md");

        assertThat(template)
                .contains("método de medição")
                .contains("| Objetivo | Key Result | Métrica | Método de Medição | Valor Atual | Meta | Prazo |");
    }

    @Test
    void generate_command_preservesRefinementContractInOutput(@TempDir Path tmpDir)
            throws IOException {
        int exit =
                new CommandLine(new IaDevKitApplication())
                        .execute("generate", "--output", tmpDir.toString(), "--force");

        assertThat(exit).isZero();

        String generatedEpicSkill =
                Files.readString(
                        tmpDir.resolve(".claude/skills/x-epic-create/SKILL.md"),
                        StandardCharsets.UTF_8);
        String generatedStorySkill =
                Files.readString(
                        tmpDir.resolve(".claude/skills/x-story-create/SKILL.md"),
                        StandardCharsets.UTF_8);
        String generatedTemplate =
                Files.readString(
                        tmpDir.resolve(".claude/templates/_TEMPLATE-EPIC.md"),
                        StandardCharsets.UTF_8);

        assertContainsAll(generatedEpicSkill, EPIC_REQUIRED_SIGNALS);
        assertContainsAll(generatedStorySkill, STORY_REQUIRED_SIGNALS);
        assertContainsNone(generatedEpicSkill, FORBIDDEN_OMISSION_SIGNALS);
        assertContainsNone(generatedStorySkill, FORBIDDEN_OMISSION_SIGNALS);
        assertThat(generatedTemplate).contains("Método de Medição");
    }

    private void assertContainsAll(String content, String[] requiredSignals) {
        for (String signal : requiredSignals) {
            assertThat(content).contains(signal);
        }
    }

    private void assertContainsNone(String content, String[] forbiddenSignals) {
        for (String signal : forbiddenSignals) {
            assertThat(content).doesNotContain(signal);
        }
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream input =
                getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(input).as("resource %s", resourcePath).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

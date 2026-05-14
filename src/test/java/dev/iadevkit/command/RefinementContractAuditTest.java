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

    @Test
    void sourceSkills_keepEpicCreationAlignedWithRefinementGate() throws IOException {
        String publicSkill = readResource("claude/skills/x-epic-create/SKILL.md");
        String internalSkill = readResource("claude/skills/x-internal-create-epic/SKILL.md");

        assertThat(publicSkill)
                .contains("`## 1` through `## 9`")
                .contains("measurement method")
                .contains("validation error")
                .doesNotContain("Do not render** sections `## 2`, `## 4`, `## 8`")
                .doesNotContain("intentionally omitting sections `2`, `4`, and `8`");

        assertThat(internalSkill)
                .contains("Feature-derived epics MUST still render all refinement-critical sections")
                .contains("validate the generated epic against")
                .contains("measurement method")
                .doesNotContain("intentionally omit sections `2`, `4`, and `8`");
    }

    @Test
    void sourceSkills_keepStoryCreationAlignedWithRefinementGate() throws IOException {
        String publicSkill = readResource("claude/skills/x-story-create/SKILL.md");
        String internalSkill = readResource("claude/skills/x-internal-create-story/SKILL.md");
        String fullProtocol =
                readResource("claude/skills/x-internal-create-story/references/full-protocol.md");

        assertThat(publicSkill)
                .contains("`## 1` through `## 9`")
                .contains("validation error")
                .contains("performance/SLA")
                .doesNotContain("Do not render** `## 2. Persona & Cenário`, `## 4. AC")
                .doesNotContain("intentionally omitting the generic sections `4` and `8`");

        assertThat(internalSkill)
                .contains("must still render those sections with concrete content")
                .contains("Refinement-critical section missing during generation")
                .doesNotContain("Sections 2/4/8 are omitted");

        assertThat(fullProtocol)
                .contains("still populate `## 2. Persona & Cenário`, `## 4. AC (...)",
                        "and `## 8. Decision Rationale`")
                .contains("validate the refinement-critical dimensions")
                .doesNotContain("intentionally omit `## 2. Persona & Cenário`");
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

        assertThat(generatedEpicSkill)
                .contains("`## 1` through `## 9`")
                .contains("validation error");
        assertThat(generatedStorySkill)
                .contains("`## 1` through `## 9`")
                .contains("performance/SLA")
                .contains("validation error");
        assertThat(generatedTemplate).contains("Método de Medição");
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream input =
                getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(input).as("resource %s", resourcePath).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

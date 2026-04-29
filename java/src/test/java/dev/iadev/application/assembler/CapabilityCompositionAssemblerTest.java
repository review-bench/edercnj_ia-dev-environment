package dev.iadev.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("CapabilityCompositionAssembler")
class CapabilityCompositionAssemblerTest {

    private final CapabilityCompositionAssembler defaultAssembler = new CapabilityCompositionAssembler();

    @Nested
    @DisplayName("assemble() — no targets root")
    class NoTargetsRoot {

        @Test
        @DisplayName("returns empty list when targets root directory does not exist")
        void returnsEmptyWhenNoTargetsRoot(@TempDir Path output) {
            Path nonExistent = output.resolve("non-existent-targets");
            CapabilityCompositionAssembler assembler = new CapabilityCompositionAssembler(nonExistent);
            ProjectConfig config = mock(ProjectConfig.class);
            TemplateEngine engine = mock(TemplateEngine.class);
            List<String> result = assembler.assemble(config, engine, output);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("assemble() — with targets root")
    class WithTargetsRoot {

        @Test
        @DisplayName("returns paths for included universal artifacts")
        void returnsPathsForIncluded(@TempDir Path targets, @TempDir Path output) throws IOException {
            Path skill = targets.resolve("skills");
            Files.createDirectories(skill);
            Files.writeString(skill.resolve("SKILL.md"),
                    "---\nname: test\nrequires-capabilities: []\n---\n# Content\n");

            CapabilityCompositionAssembler assembler = new CapabilityCompositionAssembler(targets);
            List<String> result = assembler.assemble(mock(ProjectConfig.class), mock(TemplateEngine.class), output);
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).endsWith("SKILL.md");
        }

        @Test
        @DisplayName("returns empty list when no artifacts match active set")
        void returnsEmptyForNonMatchingArtifacts(@TempDir Path targets, @TempDir Path output) throws IOException {
            Path skill = targets.resolve("skills");
            Files.createDirectories(skill);
            Files.writeString(skill.resolve("SKILL.md"),
                    "---\nname: spring-only\nrequires-capabilities:\n  - framework.spring-boot.mvc\n---\n");

            CapabilityCompositionAssembler assembler = new CapabilityCompositionAssembler(targets);
            // buildActiveSet returns empty → only universals included → spring-only is excluded
            List<String> result = assembler.assemble(mock(ProjectConfig.class), mock(TemplateEngine.class), output);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty for empty targets directory")
        void emptyTargetsReturnsEmpty(@TempDir Path targets, @TempDir Path output) {
            CapabilityCompositionAssembler assembler = new CapabilityCompositionAssembler(targets);
            List<String> result = assembler.assemble(mock(ProjectConfig.class), mock(TemplateEngine.class), output);
            assertThat(result).isEmpty();
        }
    }
}

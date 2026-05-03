package dev.iadev.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.model.AiMemoryConfig;
import dev.iadev.domain.model.BranchingModel;
import dev.iadev.domain.model.CoreStack;
import dev.iadev.domain.model.DependencyPolicyConfig;
import dev.iadev.domain.model.DocumentationConfig;
import dev.iadev.domain.model.Governance;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.QualityConfig;
import dev.iadev.domain.model.TechStack;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("DocsAssembler — initializeMemoryDirectory")
class DocsAssemblerMemoryInitTest {

    private final DocsAssembler assembler = new DocsAssembler();
    private final TemplateEngine engine = new TemplateEngine();

    private static ProjectConfig configWithAiMemory(boolean enabled) {
        ProjectConfig base = TestConfigBuilder.minimal();
        Governance gov =
                new Governance(
                        base.compliance(),
                        base.platforms(),
                        BranchingModel.GITFLOW,
                        base.telemetryEnabled(),
                        DocumentationConfig.DEFAULT,
                        QualityConfig.DEFAULT,
                        DependencyPolicyConfig.DEFAULT,
                        new AiMemoryConfig(enabled));
        return new ProjectConfig(base.core(), base.tech(), gov);
    }

    @Nested
    @DisplayName("when ai-memory is enabled")
    class WhenEnabled {

        @Test
        @DisplayName("creates ai/memory/_index.yaml")
        void initializeMemoryDirectory_whenEnabled_createsIndexFile(@TempDir Path tempDir) {
            ProjectConfig config = configWithAiMemory(true);

            assembler.initializeMemoryDirectory(config, engine, tempDir);

            assertThat(tempDir.resolve(DocsAssembler.MEMORY_INDEX_OUTPUT)).exists();
        }

        @Test
        @DisplayName("creates ai/memory/README.md")
        void initializeMemoryDirectory_whenEnabled_createsReadmeFile(@TempDir Path tempDir) {
            ProjectConfig config = configWithAiMemory(true);

            assembler.initializeMemoryDirectory(config, engine, tempDir);

            assertThat(tempDir.resolve(DocsAssembler.MEMORY_README_OUTPUT)).exists();
        }

        @Test
        @DisplayName("returns list of two written paths on first run")
        void initializeMemoryDirectory_whenEnabled_returnsTwoPaths(@TempDir Path tempDir) {
            ProjectConfig config = configWithAiMemory(true);

            List<String> result = assembler.initializeMemoryDirectory(config, engine, tempDir);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("is idempotent — returns empty list when files already exist")
        void initializeMemoryDirectory_whenEnabled_idempotentOnSecondRun(@TempDir Path tempDir) {
            ProjectConfig config = configWithAiMemory(true);

            assembler.initializeMemoryDirectory(config, engine, tempDir);
            List<String> secondResult = assembler.initializeMemoryDirectory(config, engine, tempDir);

            assertThat(secondResult).isEmpty();
        }

        @Test
        @DisplayName("is idempotent — does not overwrite existing _index.yaml content")
        void initializeMemoryDirectory_whenEnabled_doesNotOverwriteExistingIndex(
                @TempDir Path tempDir) throws Exception {
            ProjectConfig config = configWithAiMemory(true);
            assembler.initializeMemoryDirectory(config, engine, tempDir);
            Path indexFile = tempDir.resolve(DocsAssembler.MEMORY_INDEX_OUTPUT);
            String sentinel = "sentinel-content-must-survive-second-run";
            Files.writeString(indexFile, sentinel);

            assembler.initializeMemoryDirectory(config, engine, tempDir);

            assertThat(Files.readString(indexFile)).isEqualTo(sentinel);
        }

        @Test
        @DisplayName("_index.yaml contains schemaVersion and empty entries")
        void initializeMemoryDirectory_whenEnabled_indexContainsSchema(@TempDir Path tempDir)
                throws Exception {
            ProjectConfig config = configWithAiMemory(true);

            assembler.initializeMemoryDirectory(config, engine, tempDir);

            String content =
                    Files.readString(tempDir.resolve(DocsAssembler.MEMORY_INDEX_OUTPUT));
            assertThat(content).contains("schemaVersion: \"1.0\"");
            assertThat(content).contains("entries: []");
        }
    }

    @Nested
    @DisplayName("when ai-memory is disabled")
    class WhenDisabled {

        @Test
        @DisplayName("returns empty list")
        void initializeMemoryDirectory_whenDisabled_returnsEmptyList(@TempDir Path tempDir) {
            ProjectConfig config = configWithAiMemory(false);

            List<String> result = assembler.initializeMemoryDirectory(config, engine, tempDir);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("does not create ai/memory directory")
        void initializeMemoryDirectory_whenDisabled_createsNoDirectory(@TempDir Path tempDir) {
            ProjectConfig config = configWithAiMemory(false);

            assembler.initializeMemoryDirectory(config, engine, tempDir);

            assertThat(tempDir.resolve("ai/memory")).doesNotExist();
        }
    }
}

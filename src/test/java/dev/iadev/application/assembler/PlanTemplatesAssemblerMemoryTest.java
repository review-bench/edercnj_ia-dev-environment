package dev.iadev.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.model.AiMemoryConfig;
import dev.iadev.domain.model.BranchingModel;
import dev.iadev.domain.model.DependencyPolicyConfig;
import dev.iadev.domain.model.DocumentationConfig;
import dev.iadev.domain.model.Governance;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.QualityConfig;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("PlanTemplatesAssembler — ai-memory conditional template")
class PlanTemplatesAssemblerMemoryTest {

    private static final String MEMORY_TEMPLATE = "_TEMPLATE-EPIC-MEMORY-SUMMARY.md";

    private final PlanTemplatesAssembler assembler = new PlanTemplatesAssembler();
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
        @DisplayName("copies memory summary template to .claude/templates/")
        void assemble_whenEnabled_copiesMemoryTemplate(@TempDir Path tempDir) {
            ProjectConfig config = configWithAiMemory(true);

            List<String> files = assembler.assemble(config, engine, tempDir);

            assertThat(files)
                    .anyMatch(p -> p.endsWith(MEMORY_TEMPLATE));
        }

        @Test
        @DisplayName("MEMORY_TEMPLATE_SECTIONS contains _TEMPLATE-EPIC-MEMORY-SUMMARY.md")
        void memoryTemplateSections_containsMemorySummaryKey() {
            assertThat(PlanTemplateDefinitions.MEMORY_TEMPLATE_SECTIONS)
                    .containsKey(MEMORY_TEMPLATE);
        }

        @Test
        @DisplayName("memory template file exists in output directory")
        void assemble_whenEnabled_templateFileExistsOnDisk(@TempDir Path tempDir) {
            ProjectConfig config = configWithAiMemory(true);

            assembler.assemble(config, engine, tempDir);

            assertThat(tempDir.resolve(".claude/templates/" + MEMORY_TEMPLATE)).exists();
        }
    }

    @Nested
    @DisplayName("when ai-memory is disabled")
    class WhenDisabled {

        @Test
        @DisplayName("does not copy memory summary template")
        void assemble_whenDisabled_omitsMemoryTemplate(@TempDir Path tempDir) {
            ProjectConfig config = configWithAiMemory(false);

            List<String> files = assembler.assemble(config, engine, tempDir);

            assertThat(files)
                    .noneMatch(p -> p.endsWith(MEMORY_TEMPLATE));
        }

        @Test
        @DisplayName("memory template file does not exist in output directory")
        void assemble_whenDisabled_templateFileAbsent(@TempDir Path tempDir) {
            ProjectConfig config = configWithAiMemory(false);

            assembler.assemble(config, engine, tempDir);

            assertThat(tempDir.resolve(".claude/templates/" + MEMORY_TEMPLATE)).doesNotExist();
        }
    }
}

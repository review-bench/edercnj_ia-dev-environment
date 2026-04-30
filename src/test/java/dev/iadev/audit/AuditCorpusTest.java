package dev.iadev.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("AuditCorpus")
class AuditCorpusTest {

    @TempDir Path tempDir;

    @Nested
    @DisplayName("walkSkills")
    class WalkSkills {

        @Test
        void emptySkillsDir_returnsEmptyStream() {
            AuditCorpus corpus = new AuditCorpus(tempDir);

            List<Path> skills = corpus.walkSkills().toList();

            assertThat(skills).isEmpty();
        }

        @Test
        void skillsMdFiles_returnedOnlySkillMd() throws IOException {
            Path skillsDir =
                    tempDir.resolve(
                            "src/main/resources/targets/claude/skills/core/dev/x-epic-implement");
            Files.createDirectories(skillsDir);
            Files.writeString(skillsDir.resolve("SKILL.md"), "---\nname: x-epic-implement\n---\n");
            Files.writeString(skillsDir.resolve("README.md"), "# readme");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            List<Path> skills = corpus.walkSkills().toList();

            assertThat(skills).hasSize(1);
            assertThat(skills.get(0).getFileName().toString()).isEqualTo("SKILL.md");
        }
    }

    @Nested
    @DisplayName("listAuditTemplates")
    class ListAuditTemplates {

        @Test
        void nonExistentStack_returnsEmpty() {
            AuditCorpus corpus = new AuditCorpus(tempDir);

            List<Path> templates = corpus.listAuditTemplates("nonexistent");

            assertThat(templates).isEmpty();
        }

        @Test
        void javaMavenStack_returnsShTplFiles() throws IOException {
            Path scriptsDir =
                    tempDir.resolve("src/main/resources/targets/claude/scripts/java-maven");
            Files.createDirectories(scriptsDir);
            Files.writeString(
                    scriptsDir.resolve("audit-model-selection.sh.tpl"), "#!/usr/bin/env bash");
            Files.writeString(scriptsDir.resolve("audit-all.sh.tpl"), "#!/usr/bin/env bash");
            Files.writeString(scriptsDir.resolve("readme.txt"), "ignored");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            List<Path> templates = corpus.listAuditTemplates("java-maven");

            assertThat(templates).hasSize(2);
            assertThat(templates).allMatch(p -> p.getFileName().toString().endsWith(".sh.tpl"));
        }
    }

    @Nested
    @DisplayName("AuditResult")
    class AuditResultTest {

        @Test
        void ok_hasExitCode0() {
            AuditResult result = AuditResult.ok();

            assertThat(result.exitCode()).isEqualTo(0);
            assertThat(result.exitName()).isEqualTo("OK");
            assertThat(result.violations()).isEmpty();
        }

        @Test
        void violation_hasExitCode1() {
            AuditViolation v = new AuditViolation(Path.of("test.md"), 5, "RULE", "msg");
            AuditResult result = AuditResult.violation("TEST_VIOLATION", List.of(v));

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.exitName()).isEqualTo("TEST_VIOLATION");
            assertThat(result.violations()).hasSize(1);
        }

        @Test
        void operationalError_hasExitCode2() {
            AuditResult result = AuditResult.operationalError("something missing");

            assertThat(result.exitCode()).isEqualTo(2);
            assertThat(result.exitName()).isEqualTo("OPERATIONAL_ERROR");
        }
    }
}

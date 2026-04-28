package dev.iadev.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Wave B Runtime Auditors")
class WaveBRuntimeAuditorsTest {

    @TempDir
    Path tempDir;

    private Path writeExecutionState(Path dir, String content) throws IOException {
        Files.createDirectories(dir);
        Path file = dir.resolve("execution-state.json");
        Files.writeString(file, content);
        return file;
    }

    @Nested
    @DisplayName("FlowVersionAuditor")
    class FlowVersionAuditorTest {

        private final FlowVersionAuditor auditor = new FlowVersionAuditor();

        @Test
        void validFlowVersion2_returnsOk() throws IOException {
            Path dir = tempDir.resolve("ai/epics/epic-0061/");
            writeExecutionState(dir, "{\"flowVersion\": \"2\", \"epicId\": \"EPIC-0061\"}");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void validFlowVersion4_returnsOk() throws IOException {
            Path dir = tempDir.resolve("ai/epics/epic-0061/");
            writeExecutionState(dir, "{\"flowVersion\": \"4\", \"epicId\": \"EPIC-0061\"}");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void invalidFlowVersion3_returnsViolation() throws IOException {
            Path dir = tempDir.resolve("ai/epics/epic-0099/");
            writeExecutionState(dir, "{\"flowVersion\": \"3\", \"epicId\": \"EPIC-0099\"}");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("INVALID_FLOW_VERSION");
        }

        @Test
        void missingFlowVersion_returnsViolation() throws IOException {
            Path dir = tempDir.resolve("ai/epics/epic-0099/");
            writeExecutionState(dir, "{\"epicId\": \"EPIC-0099\"}");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("MISSING_FLOW_VERSION");
        }

        @Test
        void emptyCorpus_returnsOk() {
            assertThat(auditor.audit(new AuditCorpus(tempDir)).exitCode()).isEqualTo(0);
        }

        @Test
        void name_returnsFlowVersion() {
            assertThat(auditor.name()).isEqualTo("flow-version");
        }
    }

    @Nested
    @DisplayName("EpicBranchesAuditor")
    class EpicBranchesAuditorTest {

        private final EpicBranchesAuditor auditor = new EpicBranchesAuditor();

        @Test
        void flowVersion2WithEpicBranch_returnsOk() throws IOException {
            Path dir = tempDir.resolve("ai/epics/epic-0061/");
            writeExecutionState(dir,
                    "{\"flowVersion\": \"2\", \"epicId\": \"EPIC-0061\", \"epicBranch\": \"epic/0061\"}");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void flowVersion2MissingEpicBranch_returnsViolation() throws IOException {
            Path dir = tempDir.resolve("ai/epics/epic-9999/");
            writeExecutionState(dir,
                    "{\"flowVersion\": \"2\", \"epicId\": \"EPIC-9999\"}");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("MISSING_EPIC_BRANCH");
        }

        @Test
        void flowVersion1_notChecked() throws IOException {
            Path dir = tempDir.resolve("ai/epics/epic-0001/");
            writeExecutionState(dir, "{\"flowVersion\": \"1\", \"epicId\": \"EPIC-0001\"}");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void name_returnsEpicBranches() {
            assertThat(auditor.name()).isEqualTo("epic-branches");
        }
    }

    @Nested
    @DisplayName("ExecutionIntegrityAuditor")
    class ExecutionIntegrityAuditorTest {

        private final ExecutionIntegrityAuditor auditor = new ExecutionIntegrityAuditor();

        @Test
        void completedStoryWithReport_returnsOk() throws IOException {
            Path epicDir = tempDir.resolve("ai/epics/epic-0061-slug/");
            writeExecutionState(epicDir,
                    "{\"flowVersion\": \"2\", \"epicId\": \"EPIC-0061\","
                    + "\"storyStatuses\":{\"story-0061-0001\":{\"status\":\"COMPLETE\","
                    + "\"prMergeStatus\":\"MERGED\"}}}");
            Path reportsDir = epicDir.resolve("reports");
            Files.createDirectories(reportsDir);
            Files.writeString(reportsDir.resolve("story-completion-report-story-0061-0001.md"), "# Report");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void completedStoryMissingReport_returnsViolation() throws IOException {
            Path epicDir = tempDir.resolve("ai/epics/epic-0099-slug/");
            writeExecutionState(epicDir,
                    "{\"flowVersion\": \"2\", \"epicId\": \"EPIC-0099\","
                    + "\"storyStatuses\":{\"story-0099-0001\":{\"status\":\"COMPLETE\","
                    + "\"prMergeStatus\":\"MERGED\"}}}");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            AuditResult result = auditor.audit(corpus);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.violations().get(0).rule()).isEqualTo("EIE_EVIDENCE_MISSING");
        }

        @Test
        void pendingStory_notChecked() throws IOException {
            Path epicDir = tempDir.resolve("ai/epics/epic-0061-slug/");
            writeExecutionState(epicDir,
                    "{\"flowVersion\": \"2\", \"epicId\": \"EPIC-0061\","
                    + "\"storyStatuses\":{\"story-0061-0001\":{\"status\":\"PENDING\"}}}");
            AuditCorpus corpus = new AuditCorpus(tempDir);

            assertThat(auditor.audit(corpus).exitCode()).isEqualTo(0);
        }

        @Test
        void emptyCorpus_returnsOk() {
            assertThat(auditor.audit(new AuditCorpus(tempDir)).exitCode()).isEqualTo(0);
        }

        @Test
        void name_returnsExecutionIntegrity() {
            assertThat(auditor.name()).isEqualTo("execution-integrity");
        }
    }
}

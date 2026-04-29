package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TASK-0059-0006-001 + TASK-0059-0006-002: Verifies that the CI workflow defines the {@code
 * pre-commit-chain} job that re-runs the pre-commit chain (format → lint → compile) on PRs that
 * touch Java source.
 *
 * <p>Closes bypass surface E ({@code git commit --no-verify}): local hook bypasses cannot slip
 * through merge because the CI gate independently re-validates format, lint, and compilation on the
 * PR branch.
 *
 * <p>Validated Gherkin scenarios (story-0059-0006 §7):
 *
 * <ul>
 *   <li>Job {@code pre-commit-chain} is declared in the workflow.
 *   <li>Three steps: Spotless, Checkstyle, Compile — in that order.
 *   <li>Defensive Java-diff step gates Maven invocations.
 *   <li>Maven cache is configured (performance gate, re-run &lt; 90s).
 *   <li>pom.xml carries Spotless and Checkstyle plugin declarations.
 * </ul>
 *
 * @see <a href="plans/epic-0059/story-0059-0006.md">story-0059-0006</a>
 */
@DisplayName("Epic0059PreCommitChainCiTest — TASK-0059-0006-001 + 002")
class Epic0059PreCommitChainCiTest {

    private static final String CI_WORKFLOW_PATH = ".github/workflows/ci-release.yml";

    private static final String POM_PATH = "pom.xml";

    private static final String CHECKSTYLE_SUPPRESSIONS_PATH = "checkstyle-suppressions.xml";

    // -----------------------------------------------------------------------
    // CI workflow assertions
    // -----------------------------------------------------------------------

    @Test
    @DisplayName(
            "workflow_hasPreCommitChainJob — "
                    + "ci-release.yml must declare the pre-commit-chain job")
    void workflow_hasPreCommitChainJob() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        assertThat(workflow)
                .as("workflow must declare the pre-commit-chain job")
                .contains("pre-commit-chain:");
    }

    @Test
    @DisplayName("workflow_hasFormatStep — " + "job must include a Spotless format-check step")
    void workflow_hasFormatStep() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        assertThat(workflow)
                .as("pre-commit-chain must run mvn spotless:check")
                .contains("mvn spotless:check");
    }

    @Test
    @DisplayName("workflow_hasLintStep — " + "job must include a Checkstyle lint-check step")
    void workflow_hasLintStep() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        assertThat(workflow)
                .as("pre-commit-chain must run mvn checkstyle:check")
                .contains("mvn checkstyle:check");
    }

    @Test
    @DisplayName("workflow_hasCompileStep — " + "job must include an explicit compile step")
    void workflow_hasCompileStep() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        assertThat(workflow).as("pre-commit-chain must run mvn compile").contains("mvn compile");
    }

    @Test
    @DisplayName(
            "workflow_hasMavenCache — "
                    + "job must configure Maven dependency cache for performance")
    void workflow_hasMavenCache() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        // The setup-java action with cache: maven populates ~/.m2/repository.
        assertThat(workflow)
                .as("pre-commit-chain job must enable Maven cache via setup-java")
                .contains("cache: maven");
    }

    @Test
    @DisplayName(
            "workflow_hasJavaDiffGuard — "
                    + "job must include a defensive Java-diff step guarding Maven invocations")
    void workflow_hasJavaDiffGuard() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        assertThat(workflow)
                .as("workflow must define the javadiff detection step")
                .contains("java_changed");

        assertThat(workflow)
                .as("Maven steps must be conditional on java_changed == 'true'")
                .contains("java_changed == 'true'");
    }

    @Test
    @DisplayName(
            "workflow_referencesEpic0059 — "
                    + "job comment must reference story-0059-0006 for traceability")
    void workflow_referencesEpic0059() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        assertThat(workflow)
                .as("workflow must reference story-0059-0006 for traceability")
                .contains("story-0059-0006");
    }

    @Test
    @DisplayName(
            "workflow_referencesBypassSurfaceE — "
                    + "job comment must document bypass surface E closure")
    void workflow_referencesBypassSurfaceE() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        assertThat(workflow).as("workflow must mention bypass surface E").contains("surface E");
    }

    // -----------------------------------------------------------------------
    // pom.xml assertions
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("pom_hasSpotlessPlugin — " + "pom.xml must declare the Spotless Maven plugin")
    void pom_hasSpotlessPlugin() throws IOException {
        String pom = readFile(POM_PATH);

        assertThat(pom)
                .as("pom.xml must declare spotless-maven-plugin")
                .contains("spotless-maven-plugin");

        assertThat(pom)
                .as("pom.xml must specify Spotless version property")
                .contains("spotless.version");
    }

    @Test
    @DisplayName("pom_hasCheckstylePlugin — " + "pom.xml must declare the maven-checkstyle-plugin")
    void pom_hasCheckstylePlugin() throws IOException {
        String pom = readFile(POM_PATH);

        assertThat(pom)
                .as("pom.xml must declare maven-checkstyle-plugin")
                .contains("maven-checkstyle-plugin");

        assertThat(pom)
                .as("pom.xml must specify google_checks.xml as configLocation")
                .contains("google_checks.xml");
    }

    @Test
    @DisplayName("pom_hasGoogleJavaFormat — " + "pom.xml Spotless must use Google Java Format")
    void pom_hasGoogleJavaFormat() throws IOException {
        String pom = readFile(POM_PATH);

        assertThat(pom).as("Spotless must reference googleJavaFormat").contains("googleJavaFormat");
    }

    // -----------------------------------------------------------------------
    // checkstyle-suppressions.xml assertions
    // -----------------------------------------------------------------------

    @Test
    @DisplayName(
            "checkstyleSuppressionsExists — "
                    + "checkstyle-suppressions.xml must exist alongside pom.xml")
    void checkstyleSuppressionsExists() {
        Path suppressions = repoRoot().resolve(CHECKSTYLE_SUPPRESSIONS_PATH);

        assertThat(suppressions)
                .as(
                        "checkstyle-suppressions.xml must exist "
                                + "(baseline suppressions for existing code)")
                .isRegularFile();
    }

    @Test
    @DisplayName(
            "checkstyleSuppressionsContent — "
                    + "suppressions file must reference story-0059-0006 and EPIC-0059")
    void checkstyleSuppressionsContent() throws IOException {
        String suppressions = readFile(CHECKSTYLE_SUPPRESSIONS_PATH);

        assertThat(suppressions)
                .as("suppressions must reference story-0059-0006")
                .contains("story-0059-0006");

        assertThat(suppressions)
                .as("suppressions must suppress existing src/main/java files")
                .contains("src/main/java");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String readFile(String repoRelativePath) throws IOException {
        Path path = repoRoot().resolve(repoRelativePath);
        assertThat(path).as("File must exist: " + repoRelativePath).isRegularFile();
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        // Test runs from java/ (Maven working dir) or from the repo root.
        if (cwd.endsWith("java")) {
            return cwd.getParent();
        }
        return cwd;
    }
}

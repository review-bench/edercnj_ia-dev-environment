package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TASK-0059-0006-002: Validates that the pre-commit-chain CI job is correctly configured for Maven
 * cache usage and performance.
 *
 * <p>Performance gate from story-0059-0006 §7 Gherkin:
 *
 * <blockquote>
 *
 * DADO que o job já executou uma vez com sucesso, QUANDO o job é re-executado com mesmo pom.xml,
 * ENTÃO o passo de cache restore é bem-sucedido E o job completa em &lt; 90s (sem download de
 * dependências)
 *
 * </blockquote>
 *
 * <p>The CI cache mechanism relies on {@code actions/setup-java@v5} with {@code cache: maven} —
 * this uses {@code ~/.m2/repository} as the cache directory and derives the cache key from the hash
 * of pom.xml. Tests verify that the workflow declares all cache-related markers correctly so the
 * GitHub Actions cache protocol can honour the 90-second re-run target.
 *
 * <p>Also validates the path-filter correctness: PRs that only touch planning, hooks, or script
 * files must NOT trigger Maven steps.
 *
 * @see <a href="plans/epic-0059/story-0059-0006.md">story-0059-0006</a>
 */
@DisplayName("Epic0059CiCacheValidationTest — TASK-0059-0006-002")
class Epic0059CiCacheValidationTest {

    private static final String CI_WORKFLOW_PATH = ".github/workflows/ci-release.yml";

    // -----------------------------------------------------------------------
    // Cache configuration assertions
    // -----------------------------------------------------------------------

    @Test
    @DisplayName(
            "cache_usesSetupJavaAction — "
                    + "pre-commit-chain job must use actions/setup-java for caching")
    void cache_usesSetupJavaAction() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        // Verify actions/setup-java is present (provides ~/.m2 cache via GitHub)
        assertThat(workflow)
                .as("pre-commit-chain job must use actions/setup-java")
                .contains("actions/setup-java");
    }

    @Test
    @DisplayName(
            "cache_maveCacheEnabled — "
                    + "setup-java in pre-commit-chain must configure maven cache")
    void cache_mavenCacheEnabled() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        // cache: maven in setup-java is the GitHub-managed ~/.m2 cache.
        // This is what enables cache hits in re-runs (< 90s target).
        assertThat(workflow)
                .as("pre-commit-chain job must declare 'cache: maven'")
                .containsPattern("(?s)pre-commit-chain.*?cache: maven");
    }

    @Test
    @DisplayName(
            "cache_fullDepthCheckout — "
                    + "checkout must use fetch-depth: 0 for proper diff computation")
    void cache_fullDepthCheckout() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        // fetch-depth: 0 is required so `git diff origin/base...HEAD`
        // has both sides of the diff available for the javadiff step.
        assertThat(workflow)
                .as("pre-commit-chain checkout must specify fetch-depth: 0")
                .containsPattern("(?s)pre-commit-chain.*?fetch-depth: 0");
    }

    // -----------------------------------------------------------------------
    // Conditional execution assertions (path filtering)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName(
            "conditional_stepGatedOnJavaDiff — "
                    + "Spotless/Checkstyle/Compile steps must be gated on java_changed")
    void conditional_stepGatedOnJavaDiff() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        // Every mvn step must be conditional to prevent CI waste on non-Java PRs.
        long conditionalMvnCount =
                workflow.lines().filter(line -> line.contains("java_changed == 'true'")).count();

        assertThat(conditionalMvnCount)
                .as(
                        "at least 3 steps must be conditional on java_changed == 'true' "
                                + "(spotless, checkstyle, compile)")
                .isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName(
            "conditional_pullRequestOnly — "
                    + "pre-commit-chain job must only run on pull_request events")
    void conditional_pullRequestOnly() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        // The job condition prevents it from running on push-to-branch events.
        assertThat(workflow)
                .as("pre-commit-chain must have pull_request event guard")
                .containsPattern(
                        "(?s)pre-commit-chain:.*?if:.*?github\\.event_name\\s*==\\s*'pull_request'");
    }

    @Test
    @DisplayName(
            "conditional_srcJavaPathInDiffCheck — "
                    + "javadiff step must check for src/main/java/ path changes")
    void conditional_srcJavaPathInDiffCheck() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        assertThat(workflow)
                .as("javadiff step must detect src/main/java/ changes")
                .contains("src/main/java/");
    }

    @Test
    @DisplayName(
            "conditional_pomXmlInDiffCheck — "
                    + "javadiff step must also check for pom.xml changes")
    void conditional_pomXmlInDiffCheck() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        assertThat(workflow).as("javadiff step must detect pom.xml changes").contains("pom\\.xml");
    }

    // -----------------------------------------------------------------------
    // Order assertion (format → lint → compile)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName(
            "order_formatBeforeLintBeforeCompile — "
                    + "steps must appear in correct pre-commit chain order")
    void order_formatBeforeLintBeforeCompile() throws IOException {
        String workflow = readFile(CI_WORKFLOW_PATH);

        int spotlessPos = workflow.indexOf("mvn spotless:check");
        int checkstylePos = workflow.indexOf("mvn checkstyle:check");
        int compilePos = workflow.indexOf("mvn compile");

        assertThat(spotlessPos)
                .as("spotless:check must appear before checkstyle:check")
                .isLessThan(checkstylePos);

        assertThat(checkstylePos)
                .as("checkstyle:check must appear before compile")
                .isLessThan(compilePos);
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
        if (cwd.endsWith("java")) {
            return cwd.getParent();
        }
        return cwd;
    }
}

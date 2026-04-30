package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;

/**
 * Abstract base class for all smoke tests.
 *
 * <p>Provides shared infrastructure for running the assembler pipeline against bundled profiles and
 * validating output. Subclasses inherit:
 *
 * <ul>
 *   <li>A {@code @TempDir} for isolated output
 *   <li>{@link #runPipeline(String)} to execute the pipeline for a given profile
 *   <li>{@link #getOutputDir(String)} to resolve a profile-specific output directory
 * </ul>
 *
 * <p>RULE-006: All output is written to temporary directories via {@code @TempDir}.
 *
 * @see SmokeTestValidators
 * @see SmokeProfiles
 */
public abstract class SmokeTestBase {

    @TempDir protected Path tempDir;

    /**
     * Runs the assembler pipeline for the given profile, writing output to a profile-specific
     * subdirectory under {@link #tempDir}.
     *
     * @param profile the bundled profile name
     * @return the pipeline execution result
     */
    protected PipelineResult runPipeline(String profile) {
        Path outputDir = getOutputDir(profile);
        SmokeTestValidators.createDirectoryQuietly(outputDir);

        ProjectConfig config = ConfigProfiles.getStack(profile);

        AssemblerPipeline pipeline = new AssemblerPipeline(AssemblerPipeline.buildAssemblers());
        PipelineOptions options = new PipelineOptions(false, true, false, null);

        PipelineResult result = pipeline.runPipeline(config, outputDir, options);
        assertThat(result.success()).as("Pipeline must succeed for profile: %s", profile).isTrue();

        return result;
    }

    /**
     * Returns the profile-specific output directory under {@link #tempDir}.
     *
     * @param profile the bundled profile name
     * @return the output directory path
     */
    protected Path getOutputDir(String profile) {
        return tempDir.resolve(profile);
    }
}

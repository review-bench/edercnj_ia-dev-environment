package dev.iadev.cli;

import dev.iadev.adapter.inbound.cli.XArchPlanCapabilityCommand;
import dev.iadev.adapter.inbound.cli.XArchPlanFeatureCommand;
import dev.iadev.adapter.inbound.cli.XArchPlanProductCommand;
import dev.iadev.adapter.inbound.cli.XCreateCapabilityCommand;
import dev.iadev.adapter.inbound.cli.XCreateFeatureCommand;
import dev.iadev.adapter.inbound.cli.XCreateProductCommand;
import dev.iadev.adapter.inbound.cli.XEpicCreateCommand;
import dev.iadev.adapter.inbound.cli.XInternalRnfValidateCommand;
import dev.iadev.adapter.inbound.cli.XPromoteIdeationCommand;
import dev.iadev.adapter.inbound.cli.XStoryCreateCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Main entry point for the ia-dev-env CLI application.
 *
 * <p>Uses Picocli to define the root command with {@code generate} and {@code validate}
 * subcommands. The application generates Claude Code configuration under {@code .claude/} along
 * with shared artifacts (CI/CD workflows in {@code .github/workflows/}, Dockerfile, ADRs, and
 * documentation) for AI-assisted development environments.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * java -jar ia-dev-env.jar --help
 * java -jar ia-dev-env.jar --version
 * java -jar ia-dev-env.jar generate -c config.yaml
 * java -jar ia-dev-env.jar validate -c config.yaml
 * }</pre>
 */
@Command(
        name = "ia-dev-env",
        description =
                "Generates Claude Code configuration "
                        + "and shared DevEx artifacts for "
                        + "AI-assisted development environments.",
        mixinStandardHelpOptions = true,
        versionProvider = CliVersionProvider.class,
        subcommands = {
            GenerateCommand.class,
            ValidateCommand.class,
            XCreateProductCommand.class,
            XCreateCapabilityCommand.class,
            XCreateFeatureCommand.class,
            XPromoteIdeationCommand.class,
            XEpicCreateCommand.class,
            XStoryCreateCommand.class,
            XArchPlanProductCommand.class,
            XArchPlanCapabilityCommand.class,
            XArchPlanFeatureCommand.class,
            XInternalRnfValidateCommand.class
        })
public class IaDevEnvApplication implements Runnable {

    @Spec CommandSpec spec;

    /**
     * Executed when the root command is invoked without a subcommand. Prints usage help to the
     * configured output stream.
     */
    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    /**
     * Application entry point. Delegates argument parsing and execution to Picocli's {@link
     * CommandLine#execute(String...)}.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new IaDevEnvApplication()).execute(args);
        System.exit(exitCode);
    }
}

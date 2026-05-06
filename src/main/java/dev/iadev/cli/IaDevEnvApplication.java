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
 * <p>Uses Picocli to define the root command with the following subcommands:
 *
 * <ul>
 *   <li>{@code generate} — generates {@code .claude/} configuration from a YAML project file
 *   <li>{@code validate} — validates a YAML project file without generating output
 *   <li>{@code x-create-product} — creates a Product artifact from an ideation file
 *   <li>{@code x-create-capability} — decomposes a Product into Capability artifacts
 *   <li>{@code x-create-feature} — decomposes a Capability into Feature artifacts
 *   <li>{@code x-promote-ideation} — persists a transient ideation to {@code ai/ideations/}
 *   <li>{@code x-epic-create} — creates an Epic artifact from a Feature
 *   <li>{@code x-story-create} — creates Story artifacts from a Feature or Epic
 *   <li>{@code x-arch-plan-product} — generates C4 diagrams for a Product
 *   <li>{@code x-arch-plan-capability} — generates C4 diagrams for a Capability
 *   <li>{@code x-arch-plan-feature} — generates C4 diagrams for a Feature
 *   <li>{@code x-internal-rnf-validate} — validates RNF no-relax markers
 * </ul>
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * ia-dev-env --help
 * ia-dev-env generate -c config.yaml
 * ia-dev-env validate -c config.yaml
 * ia-dev-env x-create-product --ideation-file ai/ideations/ideation-0001.md
 * ia-dev-env x-arch-plan-product --product-id product-0001
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

package dev.iadevkit;

import dev.iadevkit.command.GenerateCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "ia-dev-kit",
        subcommands = {GenerateCommand.class, CommandLine.HelpCommand.class},
        description = "Generates .claude/ configuration for Claude Code projects",
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class)
public class IaDevKitApplication {

    public static void main(String[] args) {
        System.exit(new CommandLine(new IaDevKitApplication()).execute(args));
    }
}

package dev.iadev.adapter.inbound.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "x-promote-ideation",
        mixinStandardHelpOptions = true,
        description =
                "Promote transient x-feature-ideate output to a persistent ideation artifact.")
public class XPromoteIdeationCommand implements Callable<Integer> {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_VALIDATION = 1;
    static final int EXIT_EXECUTION = 2;

    @Spec CommandSpec spec;

    @Option(
            names = {"--from-file"},
            description = "Path to temporary ideation file produced by x-feature-ideate")
    String fromFile;

    @Option(
            names = {"--from-stdin"},
            description = "Read ideation content from stdin")
    boolean fromStdin;

    @Option(
            names = {"--ideation-id"},
            description =
                    "Explicit ideation identifier (e.g. ideation-0001); auto-assigned if absent")
    String ideationId;

    @Option(
            names = {"--validate"},
            description = "Validate only; do not write artifact")
    boolean validateOnly;

    @Override
    public Integer call() {
        if (!fromStdin && (fromFile == null || fromFile.isBlank())) {
            spec.commandLine()
                    .getErr()
                    .println("Error: either --from-file or --from-stdin must be specified");
            return EXIT_VALIDATION;
        }
        if (ideationId != null && !ideationId.matches("ideation-\\d{4}")) {
            spec.commandLine()
                    .getErr()
                    .println(
                            "Error: --ideation-id must match pattern ideation-NNNN (e.g. ideation-0001)");
            return EXIT_VALIDATION;
        }
        String source = fromStdin ? "stdin" : fromFile;
        String mode = validateOnly ? "validate-only" : "persist";
        spec.commandLine()
                .getOut()
                .println(
                        "x-promote-ideation: source="
                                + source
                                + ", mode="
                                + mode
                                + (ideationId != null ? ", id=" + ideationId : ""));
        return EXIT_SUCCESS;
    }
}

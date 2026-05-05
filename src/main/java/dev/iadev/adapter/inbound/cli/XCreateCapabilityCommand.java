package dev.iadev.adapter.inbound.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "x-create-capability",
        mixinStandardHelpOptions = true,
        description = "Decompose a Product into Capabilities with explicit RNF inheritance.")
public class XCreateCapabilityCommand implements Callable<Integer> {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_VALIDATION = 1;
    static final int EXIT_EXECUTION = 2;

    @Spec CommandSpec spec;

    @Option(
            names = {"--product-id"},
            required = true,
            description = "Product identifier (e.g. product-0001)")
    String productId;

    @Option(
            names = {"--output-dir"},
            description = "Directory for output artifacts (default: ai/products)")
    String outputDirPath;

    @Option(
            names = {"--capabilities"},
            description =
                    "JSON array of capability names to create (e.g. '[\"ingest\",\"query\"]')")
    String capabilitiesJson;

    @Option(
            names = {"--auto-decompose"},
            description = "Auto-decompose product into capabilities using heuristic")
    boolean autoDecompose;

    @Override
    public Integer call() {
        if (!autoDecompose && (capabilitiesJson == null || capabilitiesJson.isBlank())) {
            spec.commandLine()
                    .getErr()
                    .println("Error: either --auto-decompose or --capabilities must be specified");
            return EXIT_VALIDATION;
        }
        if (!autoDecompose) {
            try {
                CapabilityInteractiveInputParser.parseCapabilities(capabilitiesJson);
            } catch (IllegalArgumentException e) {
                spec.commandLine().getErr().println("Error: " + e.getMessage());
                return EXIT_VALIDATION;
            }
        }
        spec.commandLine()
                .getOut()
                .println(
                        "x-create-capability: product-id="
                                + productId
                                + ", auto-decompose="
                                + autoDecompose);
        return EXIT_SUCCESS;
    }
}

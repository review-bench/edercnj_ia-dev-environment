package dev.iadev.adapter.inbound.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "x-create-feature",
        mixinStandardHelpOptions = true,
        description = "Decompose a Capability into Features with Gherkin Acceptance Criteria.")
public class XCreateFeatureCommand implements Callable<Integer> {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_VALIDATION = 1;
    static final int EXIT_EXECUTION = 2;

    @Spec CommandSpec spec;

    @Option(
            names = {"--capability-id"},
            required = true,
            description = "Capability identifier (e.g. capability-c1)")
    String capabilityId;

    @Option(
            names = {"--features"},
            description = "JSON array of feature objects: [{\"name\":\"...\"},...] (4-8 entries)")
    String featuresJson;

    @Option(
            names = {"--auto-decompose"},
            description = "Auto-decompose capability into features using heuristic")
    boolean autoDecompose;

    @Option(
            names = {"--output-dir"},
            description = "Directory for output artifacts (default: ai/features)")
    String outputDirPath;

    @Override
    public Integer call() {
        if (!autoDecompose && (featuresJson == null || featuresJson.isBlank())) {
            spec.commandLine()
                    .getErr()
                    .println("Error: either --auto-decompose or --features must be specified");
            return EXIT_VALIDATION;
        }
        if (!autoDecompose) {
            try {
                FeatureInputParser.parseFeatures(featuresJson);
            } catch (IllegalArgumentException e) {
                spec.commandLine().getErr().println("Error: " + e.getMessage());
                return EXIT_VALIDATION;
            }
        }
        spec.commandLine()
                .getOut()
                .println(
                        "x-create-feature: capability-id="
                                + capabilityId
                                + ", auto-decompose="
                                + autoDecompose);
        return EXIT_SUCCESS;
    }
}

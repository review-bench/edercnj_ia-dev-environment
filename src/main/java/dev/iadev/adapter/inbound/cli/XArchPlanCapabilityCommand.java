package dev.iadev.adapter.inbound.cli;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.architecture.CapabilityC4Planner;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(
        name = "x-arch-plan-capability",
        mixinStandardHelpOptions = true,
        description = "Generate C4 Container + Component diagrams for a capability.")
public class XArchPlanCapabilityCommand implements Callable<Integer> {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_VALIDATION = 1;
    static final int EXIT_EXECUTION = 2;

    @Spec
    CommandSpec spec;

    @Option(
            names = {"--capability-id"},
            required = true,
            description = "Capability identifier (e.g. capability-0001).")
    String capabilityId;

    @Option(
            names = {"--output-format"},
            description = "Output format: mermaid (default), plantuml.")
    String outputFormat;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();

        C4OutputFormat format;
        try {
            format = C4OutputFormat.fromString(outputFormat);
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
            return EXIT_VALIDATION;
        }

        if (capabilityId == null || capabilityId.isBlank()) {
            out.println("Error: --capability-id must not be blank");
            return EXIT_VALIDATION;
        }

        try {
            CapabilityC4Planner planner = new CapabilityC4Planner();
            C4Diagram container = planner.planContainer(capabilityId, format);
            C4Diagram component = planner.planComponent(capabilityId, format);
            out.println("C4 Container diagram generated for: " + capabilityId);
            out.println(container.content());
            out.println("C4 Component diagram generated for: " + capabilityId);
            out.println(component.content());
            return EXIT_SUCCESS;
        } catch (Exception e) {
            out.println("Error: " + e.getMessage());
            return EXIT_EXECUTION;
        }
    }
}

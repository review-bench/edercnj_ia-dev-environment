package dev.iadev.adapter.inbound.cli;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.architecture.ProductC4Planner;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(
        name = "x-arch-plan-product",
        mixinStandardHelpOptions = true,
        description = "Generate C4 Context + Container diagrams for a product.")
public class XArchPlanProductCommand implements Callable<Integer> {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_VALIDATION = 1;
    static final int EXIT_EXECUTION = 2;

    @Spec
    CommandSpec spec;

    @Option(
            names = {"--product-id"},
            required = true,
            description = "Product identifier (e.g. product-0001).")
    String productId;

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

        if (productId == null || productId.isBlank()) {
            out.println("Error: --product-id must not be blank");
            return EXIT_VALIDATION;
        }

        try {
            ProductC4Planner planner = new ProductC4Planner();
            C4Diagram context = planner.planContext(productId, format);
            C4Diagram container = planner.planContainer(productId, format);
            out.println("C4 Context diagram generated for: " + productId);
            out.println(context.content());
            out.println("C4 Container diagram generated for: " + productId);
            out.println(container.content());
            return EXIT_SUCCESS;
        } catch (Exception e) {
            out.println("Error: " + e.getMessage());
            return EXIT_EXECUTION;
        }
    }
}

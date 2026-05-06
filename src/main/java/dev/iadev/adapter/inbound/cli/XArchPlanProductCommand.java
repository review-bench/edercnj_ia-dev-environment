package dev.iadev.adapter.inbound.cli;

import dev.iadev.adapter.outbound.documentation.C4DiagramGenerator;
import dev.iadev.application.architecture.ArchitectureRefactoringUseCase;
import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4LevelValidator;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.architecture.ProductC4Model;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "x-plan-arch-product",
        mixinStandardHelpOptions = true,
        description = "Generate C4 Context + Container diagrams for a product.")
public class XArchPlanProductCommand implements Callable<Integer> {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_VALIDATION = 1;
    static final int EXIT_EXECUTION = 2;

    @Spec CommandSpec spec;

    @Option(
            names = {"--product-id"},
            required = true,
            description = "Product identifier (e.g. product-0001).")
    String productId;

    @Option(
            names = {"--output-format"},
            description = "Output format: mermaid (default), plantuml.")
    String outputFormat;

    private final ArchitectureRefactoringUseCase useCase =
            new ArchitectureRefactoringUseCase(new C4DiagramGenerator(), new C4LevelValidator());

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        try {
            ProductC4Model model = useCase.planProduct(productId, resolveFormat());
            writeSummary(out, model);
            return EXIT_SUCCESS;
        } catch (IllegalArgumentException e) {
            out.println("Validation error: " + e.getMessage());
            return EXIT_VALIDATION;
        } catch (Exception e) {
            out.println("Error: " + e.getMessage());
            return EXIT_EXECUTION;
        }
    }

    private C4OutputFormat resolveFormat() {
        return C4OutputFormat.fromString(outputFormat);
    }

    private void writeSummary(PrintWriter out, ProductC4Model model) {
        out.println("C4 diagrams generated for " + model.productId() + ":");
        writeLine(out, model.contextDiagram(), model.isPlaceholder("CONTEXT"));
        writeLine(out, model.containerDiagram(), model.isPlaceholder("CONTAINER"));
        writeLine(out, model.componentDiagram(), model.isPlaceholder("COMPONENT"));
    }

    private void writeLine(PrintWriter out, C4Diagram diagram, boolean placeholder) {
        out.printf(
                "  %-10s: %s  [%s]%n",
                diagram.level(), diagram.title(), placeholder ? "placeholder" : "OK");
    }
}

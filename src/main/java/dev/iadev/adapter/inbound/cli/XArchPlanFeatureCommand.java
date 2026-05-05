package dev.iadev.adapter.inbound.cli;

import dev.iadev.adapter.outbound.documentation.C4DiagramGenerator;
import dev.iadev.application.architecture.ArchitectureRefactoringUseCase;
import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4LevelValidator;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.architecture.FeatureC4Model;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "x-arch-plan-feature",
        mixinStandardHelpOptions = true,
        description = "Generate C4 Context + Container diagrams for a feature.")
public class XArchPlanFeatureCommand implements Callable<Integer> {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_VALIDATION = 1;
    static final int EXIT_EXECUTION = 2;

    @Spec CommandSpec spec;

    @Option(
            names = {"--feature-id"},
            required = true,
            description = "Feature identifier (e.g. feature-oauth2 or oauth2-integration).")
    String featureId;

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
            FeatureC4Model model = useCase.planFeature(featureId, resolveFormat());
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

    private void writeSummary(PrintWriter out, FeatureC4Model model) {
        out.println("C4 diagrams generated for " + model.featureId() + ":");
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

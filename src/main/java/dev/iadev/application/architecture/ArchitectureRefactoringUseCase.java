package dev.iadev.application.architecture;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4LevelValidator;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.architecture.CapabilityC4Model;
import dev.iadev.domain.architecture.FeatureC4Model;
import dev.iadev.domain.architecture.ProductC4Model;
import dev.iadev.domain.port.output.C4DiagramPort;
import java.util.List;

public final class ArchitectureRefactoringUseCase {

    private final C4DiagramPort diagramPort;
    private final C4LevelValidator validator;

    public ArchitectureRefactoringUseCase(C4DiagramPort diagramPort, C4LevelValidator validator) {
        this.diagramPort = diagramPort;
        this.validator = validator;
    }

    public ProductC4Model planProduct(String productId, C4OutputFormat format) {
        C4Diagram context = diagramPort.generate(C4Level.CONTEXT, productId, format);
        C4Diagram container = diagramPort.generate(C4Level.CONTAINER, productId, format);
        C4Diagram component = diagramPort.generatePlaceholder(C4Level.COMPONENT, productId, format);
        validate(List.of(context, container, component));
        return new ProductC4Model(
                productId,
                format,
                context,
                container,
                component,
                List.of(C4Level.COMPONENT.name()));
    }

    public CapabilityC4Model planCapability(String capabilityId, C4OutputFormat format) {
        C4Diagram context = diagramPort.generatePlaceholder(C4Level.CONTEXT, capabilityId, format);
        C4Diagram container = diagramPort.generate(C4Level.CONTAINER, capabilityId, format);
        C4Diagram component = diagramPort.generate(C4Level.COMPONENT, capabilityId, format);
        validate(List.of(context, container, component));
        return new CapabilityC4Model(
                capabilityId,
                format,
                context,
                container,
                component,
                List.of(C4Level.CONTEXT.name()));
    }

    public FeatureC4Model planFeature(String featureId, C4OutputFormat format) {
        String normalizedFeatureId = normalizeFeatureId(featureId);
        C4Diagram context = diagramPort.generate(C4Level.CONTEXT, normalizedFeatureId, format);
        C4Diagram container = diagramPort.generate(C4Level.CONTAINER, normalizedFeatureId, format);
        C4Diagram component =
                diagramPort.generatePlaceholder(C4Level.COMPONENT, normalizedFeatureId, format);
        validate(List.of(context, container, component));
        return new FeatureC4Model(
                featureId,
                format,
                context,
                container,
                component,
                List.of(C4Level.COMPONENT.name()));
    }

    private String normalizeFeatureId(String featureId) {
        return featureId.startsWith("feature-") ? featureId : "feature-" + featureId;
    }

    private void validate(List<C4Diagram> diagrams) {
        var result = validator.validate(diagrams);
        if (!result.valid()) {
            throw new IllegalArgumentException(result.message());
        }
    }
}

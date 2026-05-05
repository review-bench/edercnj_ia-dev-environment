package dev.iadev.adapter.outbound.documentation;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.architecture.CapabilityC4Planner;
import dev.iadev.domain.architecture.FeatureC4Planner;
import dev.iadev.domain.architecture.ProductC4Planner;
import dev.iadev.domain.port.output.C4DiagramPort;

public final class C4DiagramGenerator implements C4DiagramPort {

    private final ProductC4Planner productPlanner = new ProductC4Planner();
    private final CapabilityC4Planner capabilityPlanner = new CapabilityC4Planner();
    private final FeatureC4Planner featurePlanner = new FeatureC4Planner();
    private final C4PlaceholderGenerator placeholderGenerator = new C4PlaceholderGenerator();

    @Override
    public C4Diagram generate(C4Level level, String entityId, C4OutputFormat format) {
        if (entityId.startsWith("product-")) {
            return generateProduct(level, entityId, format);
        }
        if (entityId.startsWith("capability-")) {
            return generateCapability(level, entityId, format);
        }
        if (entityId.startsWith("feature-")) {
            return generateFeature(level, entityId, format);
        }
        throw new IllegalArgumentException("entityId must start with product-, capability-, or feature-");
    }

    @Override
    public C4Diagram generatePlaceholder(C4Level level, String entityId, C4OutputFormat format) {
        return placeholderGenerator.generate(level, entityId, format);
    }

    private C4Diagram generateProduct(C4Level level, String entityId, C4OutputFormat format) {
        return switch (level) {
            case CONTEXT -> productPlanner.planContext(entityId, format);
            case CONTAINER -> productPlanner.planContainer(entityId, format);
            default -> throw new IllegalArgumentException("product diagrams support only CONTEXT and CONTAINER");
        };
    }

    private C4Diagram generateCapability(C4Level level, String entityId, C4OutputFormat format) {
        return switch (level) {
            case CONTAINER -> capabilityPlanner.planContainer(entityId, format);
            case COMPONENT -> capabilityPlanner.planComponent(entityId, format);
            default -> throw new IllegalArgumentException("capability diagrams support only CONTAINER and COMPONENT");
        };
    }

    private C4Diagram generateFeature(C4Level level, String entityId, C4OutputFormat format) {
        return switch (level) {
            case CONTEXT -> featurePlanner.planContext(entityId, format);
            case CONTAINER -> featurePlanner.planContainer(entityId, format);
            default -> throw new IllegalArgumentException("feature diagrams support only CONTEXT and CONTAINER");
        };
    }
}

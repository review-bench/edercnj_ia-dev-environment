package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.architecture.CapabilityC4Planner;
import dev.iadev.domain.architecture.FeatureC4Planner;
import dev.iadev.domain.architecture.ProductC4Planner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("XArchPlan C4 Smoke")
class XArchPlanC4SmokeTest {

    private final ProductC4Planner productPlanner = new ProductC4Planner();
    private final CapabilityC4Planner capabilityPlanner = new CapabilityC4Planner();
    private final FeatureC4Planner featurePlanner = new FeatureC4Planner();

    @Test
    void product_mermaid_generatesContextAndContainerDiagrams() {
        C4Diagram context = productPlanner.planContext("product-0001", C4OutputFormat.MERMAID);
        C4Diagram container = productPlanner.planContainer("product-0001", C4OutputFormat.MERMAID);

        assertThat(context.level()).isEqualTo(C4Level.CONTEXT);
        assertThat(container.level()).isEqualTo(C4Level.CONTAINER);
        assertThat(context.content()).contains("C4Context");
        assertThat(container.content()).contains("C4Container");
    }

    @Test
    void capability_mermaid_generatesContainerAndComponentDiagrams() {
        C4Diagram container = capabilityPlanner.planContainer("capability-auth", C4OutputFormat.MERMAID);
        C4Diagram component = capabilityPlanner.planComponent("capability-auth", C4OutputFormat.MERMAID);

        assertThat(container.level()).isEqualTo(C4Level.CONTAINER);
        assertThat(component.level()).isEqualTo(C4Level.COMPONENT);
        assertThat(container.content()).contains("C4Container");
        assertThat(component.content()).contains("C4Component");
    }

    @Test
    void feature_mermaid_generatesContextAndContainerDiagrams() {
        C4Diagram context = featurePlanner.planContext("feature-oauth2", C4OutputFormat.MERMAID);
        C4Diagram container = featurePlanner.planContainer("feature-oauth2", C4OutputFormat.MERMAID);

        assertThat(context.level()).isEqualTo(C4Level.CONTEXT);
        assertThat(container.level()).isEqualTo(C4Level.CONTAINER);
        assertThat(context.content()).contains("OAuth2 Provider");
        assertThat(container.content()).contains("auth-service");
    }

    @Test
    void defaultFormat_isMermaid() {
        C4Diagram diagram = productPlanner.planContext("product-0001", C4OutputFormat.fromString(null));

        assertThat(diagram.format()).isEqualTo(C4OutputFormat.MERMAID);
    }

    @Test
    void plantumlFormat_producesDifferentContent() {
        C4Diagram mermaid = productPlanner.planContext("product-0001", C4OutputFormat.MERMAID);
        C4Diagram plantuml = productPlanner.planContext("product-0001", C4OutputFormat.PLANTUML);

        assertThat(mermaid.content()).isNotEqualTo(plantuml.content());
        assertThat(plantuml.content()).startsWith("@startuml");
        assertThat(mermaid.content()).startsWith("C4Context");
    }

    @Test
    void product_htmlEscapingInDiagrams() {
        C4Diagram diagram = productPlanner.planContext("product-<test>", C4OutputFormat.MERMAID);

        assertThat(diagram.content()).contains("&lt;test&gt;");
        assertThat(diagram.content()).doesNotContain("<test>");
    }

    @Test
    void capability_htmlEscapingInDiagrams() {
        C4Diagram diagram = capabilityPlanner.planContainer("capability-<xss>", C4OutputFormat.MERMAID);

        assertThat(diagram.content()).contains("&lt;xss&gt;");
        assertThat(diagram.content()).doesNotContain("<xss>");
    }
}

package dev.iadev.domain.architecture;

import dev.iadev.domain.architecture.C4Diagram.C4Level;

public final class CapabilityC4Planner {

    public C4Diagram planContainer(String capabilityId, C4OutputFormat format) {
        validate(capabilityId);
        String content = buildContainer(capabilityId, format);
        return new C4Diagram(capabilityId + " — C4 Container", C4Level.CONTAINER, format, content);
    }

    public C4Diagram planComponent(String capabilityId, C4OutputFormat format) {
        validate(capabilityId);
        String content = buildComponent(capabilityId, format);
        return new C4Diagram(capabilityId + " — C4 Component", C4Level.COMPONENT, format, content);
    }

    private void validate(String capabilityId) {
        if (capabilityId == null || capabilityId.isBlank()) {
            throw new IllegalArgumentException("capabilityId must not be blank");
        }
    }

    private String buildContainer(String capabilityId, C4OutputFormat format) {
        if (format == C4OutputFormat.PLANTUML) {
            return "@startuml\n"
                    + "!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml\n"
                    + "title Container — " + escape(capabilityId) + "\n"
                    + "System_Boundary(cap, \"" + escape(capabilityId) + "\") {\n"
                    + "  Container(handler, \"Handler\", \"Java\", \"Entry point\")\n"
                    + "  Container(service, \"Service\", \"Java\", \"Business logic\")\n"
                    + "  ContainerDb(store, \"Store\", \"PostgreSQL\", \"Persistence\")\n"
                    + "}\n"
                    + "@enduml";
        }
        return "C4Container\n"
                + "  title Container — " + escape(capabilityId) + "\n"
                + "  System_Boundary(cap, \"" + escape(capabilityId) + "\") {\n"
                + "    Container(handler, \"Handler\", \"Java\", \"Entry point\")\n"
                + "    Container(service, \"Service\", \"Java\", \"Business logic\")\n"
                + "    ContainerDb(store, \"Store\", \"PostgreSQL\", \"Persistence\")\n"
                + "  }";
    }

    private String buildComponent(String capabilityId, C4OutputFormat format) {
        if (format == C4OutputFormat.PLANTUML) {
            return "@startuml\n"
                    + "!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml\n"
                    + "title Component — " + escape(capabilityId) + "\n"
                    + "Container_Boundary(service, \"Service\") {\n"
                    + "  Component(domain, \"Domain\", \"Java\", \"Entities and rules\")\n"
                    + "  Component(app, \"Application\", \"Java\", \"Use cases\")\n"
                    + "  Component(adapter, \"Adapter\", \"Java\", \"Inbound/Outbound\")\n"
                    + "}\n"
                    + "@enduml";
        }
        return "C4Component\n"
                + "  title Component — " + escape(capabilityId) + "\n"
                + "  Container_Boundary(service, \"Service\") {\n"
                + "    Component(domain, \"Domain\", \"Java\", \"Entities and rules\")\n"
                + "    Component(app, \"Application\", \"Java\", \"Use cases\")\n"
                + "    Component(adapter, \"Adapter\", \"Java\", \"Inbound/Outbound\")\n"
                + "  }";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

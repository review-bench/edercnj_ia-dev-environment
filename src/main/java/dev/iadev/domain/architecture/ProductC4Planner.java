package dev.iadev.domain.architecture;

import dev.iadev.domain.architecture.C4Diagram.C4Level;

public final class ProductC4Planner {

    public C4Diagram planContext(String productId, C4OutputFormat format) {
        validate(productId);
        String content = buildContext(productId, format);
        return new C4Diagram(productId + " — C4 Context", C4Level.CONTEXT, format, content);
    }

    public C4Diagram planContainer(String productId, C4OutputFormat format) {
        validate(productId);
        String content = buildContainer(productId, format);
        return new C4Diagram(productId + " — C4 Container", C4Level.CONTAINER, format, content);
    }

    private void validate(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
    }

    private String buildContext(String productId, C4OutputFormat format) {
        if (format == C4OutputFormat.PLANTUML) {
            return "@startuml\n"
                    + "!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Context.puml\n"
                    + "title System Context — " + escape(productId) + "\n"
                    + "Person(user, \"User\", \"Product user\")\n"
                    + "System(product, \"" + escape(productId) + "\", \"Product system\")\n"
                    + "System_Ext(ext, \"External System\", \"Third-party integration\")\n"
                    + "Rel(user, product, \"Uses\")\n"
                    + "Rel(product, ext, \"Calls\")\n"
                    + "@enduml";
        }
        return "C4Context\n"
                + "  title System Context — " + escape(productId) + "\n"
                + "  Person(user, \"User\", \"Product user\")\n"
                + "  System(product, \"" + escape(productId) + "\", \"Product system\")\n"
                + "  System_Ext(ext, \"External System\", \"Third-party integration\")\n"
                + "  Rel(user, product, \"Uses\")\n"
                + "  Rel(product, ext, \"Calls\")";
    }

    private String buildContainer(String productId, C4OutputFormat format) {
        if (format == C4OutputFormat.PLANTUML) {
            return "@startuml\n"
                    + "!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml\n"
                    + "title Container — " + escape(productId) + "\n"
                    + "System_Boundary(product, \"" + escape(productId) + "\") {\n"
                    + "  Container(api, \"API\", \"Java\", \"Handles requests\")\n"
                    + "  ContainerDb(db, \"Database\", \"PostgreSQL\", \"Stores data\")\n"
                    + "}\n"
                    + "@enduml";
        }
        return "C4Container\n"
                + "  title Container — " + escape(productId) + "\n"
                + "  System_Boundary(product, \"" + escape(productId) + "\") {\n"
                + "    Container(api, \"API\", \"Java\", \"Handles requests\")\n"
                + "    ContainerDb(db, \"Database\", \"PostgreSQL\", \"Stores data\")\n"
                + "  }";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

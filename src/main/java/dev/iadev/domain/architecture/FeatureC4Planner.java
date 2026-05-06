package dev.iadev.domain.architecture;

import dev.iadev.domain.architecture.C4Diagram.C4Level;

public final class FeatureC4Planner {

    public C4Diagram planContext(String featureId, C4OutputFormat format) {
        validate(featureId);
        return new C4Diagram(
                featureId + " — C4 Context",
                C4Level.CONTEXT,
                format,
                buildContext(featureId, format));
    }

    public C4Diagram planContainer(String featureId, C4OutputFormat format) {
        validate(featureId);
        return new C4Diagram(
                featureId + " — C4 Container",
                C4Level.CONTAINER,
                format,
                buildContainer(featureId, format));
    }

    private void validate(String featureId) {
        if (featureId == null || featureId.isBlank()) {
            throw new IllegalArgumentException("featureId must not be blank");
        }
    }

    private String buildContext(String featureId, C4OutputFormat format) {
        if (format == C4OutputFormat.PLANTUML) {
            return "@startuml\n"
                    + "!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Context.puml\n"
                    + "title Feature Context — "
                    + escape(featureId)
                    + "\n"
                    + "Person(user, \"Feature user\", \"Consumes the feature flow\")\n"
                    + "System(feature, \""
                    + escape(featureId)
                    + "\", \"Feature boundary\")\n"
                    + "System_Ext(provider, \"OAuth2 Provider\", \"External identity provider\")\n"
                    + "Rel(user, feature, \"Uses\")\n"
                    + "Rel(feature, provider, \"Delegates authentication\")\n"
                    + "@enduml";
        }
        return "C4Context\n"
                + "  title Feature Context — "
                + escape(featureId)
                + "\n"
                + "  Person(user, \"Feature user\", \"Consumes the feature flow\")\n"
                + "  System(feature, \""
                + escape(featureId)
                + "\", \"Feature boundary\")\n"
                + "  System_Ext(provider, \"OAuth2 Provider\", \"External identity provider\")\n"
                + "  Rel(user, feature, \"Uses\")\n"
                + "  Rel(feature, provider, \"Delegates authentication\")";
    }

    private String buildContainer(String featureId, C4OutputFormat format) {
        if (format == C4OutputFormat.PLANTUML) {
            return "@startuml\n"
                    + "!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml\n"
                    + "title Feature Container — "
                    + escape(featureId)
                    + "\n"
                    + "System_Boundary(feature, \""
                    + escape(featureId)
                    + "\") {\n"
                    + "  Container(authService, \"auth-service\", \"Java\", \"Processes feature orchestration\")\n"
                    + "  ContainerDb(db, \"db\", \"PostgreSQL\", \"Persists identities\")\n"
                    + "  Container(cache, \"cache\", \"Redis\", \"Caches provider/session data\")\n"
                    + "}\n"
                    + "@enduml";
        }
        return "C4Container\n"
                + "  title Feature Container — "
                + escape(featureId)
                + "\n"
                + "  System_Boundary(feature, \""
                + escape(featureId)
                + "\") {\n"
                + "    Container(authService, \"auth-service\", \"Java\", \"Processes feature orchestration\")\n"
                + "    ContainerDb(db, \"db\", \"PostgreSQL\", \"Persists identities\")\n"
                + "    Container(cache, \"cache\", \"Redis\", \"Caches provider/session data\")\n"
                + "  }";
    }

    private static String escape(String text) {
        return C4TextSanitizer.sanitize(text);
    }
}

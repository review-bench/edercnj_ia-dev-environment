package dev.iadev.adapter.outbound.feature;

import dev.iadev.application.feature.FeatureEpicSource;
import dev.iadev.application.feature.InheritedRnfLine;
import dev.iadev.application.feature.StoryProposal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class StoryFromFeatureArtifactWriter {

    private StoryFromFeatureArtifactWriter() {}

    public static List<Path> write(
            String epicId, FeatureEpicSource source, List<StoryProposal> stories, Path outputDir)
            throws IOException {
        Path epicDir = outputDir.resolve("epic-" + epicId + "-" + slugify(source.title()));
        Files.createDirectories(epicDir);
        List<Path> written = new ArrayList<>();
        for (int i = 0; i < stories.size(); i++) {
            String storyId = "story-" + epicId + "-" + "%04d".formatted(i + 1);
            Path target = epicDir.resolve(storyId + ".md");
            Files.writeString(target, render(storyId, epicId, source, stories.get(i)));
            written.add(target);
        }
        return List.copyOf(written);
    }

    static String render(
            String storyId, String epicId, FeatureEpicSource source, StoryProposal proposal) {
        StringBuilder sb = new StringBuilder();
        appendHeader(sb, storyId, epicId, source);
        appendVision(sb, proposal);
        appendRnfs(sb, source.inheritedRnfs());
        appendValue(sb, proposal);
        appendContracts(sb, proposal);
        appendC4Sections(sb, source, proposal);
        appendTasks(sb, epicId, proposal);
        appendDependencies(sb);
        appendRefinementVerdict(sb);
        return sb.toString();
    }

    private static void appendHeader(
            StringBuilder sb, String storyId, String epicId, FeatureEpicSource source) {
        sb.append("# História: ")
                .append(source.title())
                .append(" — ")
                .append(proposalTitleSuffix(source.featureId()))
                .append("\n\n");
        sb.append("**ID:** ").append(storyId).append("\n");
        sb.append("**Chave Jira:** —\n");
        sb.append("**Status:** Pendente\n");
        sb.append("**Epic ID:** EPIC-").append(epicId).append("\n");
        sb.append("**Source Feature:** ").append(source.featureId()).append("\n");
        sb.append("**Source Feature Link:** ")
                .append(source.sourceFeatureLink())
                .append("\n\n---\n\n");
    }

    private static void appendVision(StringBuilder sb, StoryProposal proposal) {
        sb.append("## 1. Visão\n\n");
        sb.append("Como **")
                .append(proposal.actor())
                .append("**, eu quero ")
                .append(proposal.title())
                .append(
                        ", para que o fluxo derivado da feature seja executável em nível de story.\n\n---\n\n");
    }

    private static void appendRnfs(StringBuilder sb, List<InheritedRnfLine> rnfs) {
        sb.append("## 2. RNFs Herdadas\n\n");
        sb.append(
                "| Categoria | RNF Original (Produto) | no-relax? | Override Value | Justificação | Approval Status | Approver |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n");
        for (InheritedRnfLine rnf : rnfs) {
            String category = normalizeCategory(rnf.id());
            sb.append("| ")
                    .append(category)
                    .append(" | ")
                    .append(rnf.requirement())
                    .append(" | ")
                    .append(rnf.waivable() ? "false" : "true")
                    .append(" | — | — | — | — |\n");
        }
        sb.append("\n---\n\n");
    }

    private static void appendValue(StringBuilder sb, StoryProposal proposal) {
        sb.append("## 3. Entrega de Valor\n\n");
        sb.append(
                "- **Valor Principal:** Story executável derivada de uma capability já refinada\n");
        sb.append("- **Métrica de Sucesso:** Backlog da feature avança sem perder ACs herdados\n");
        sb.append("- **Impacto no Negócio:** ")
                .append(proposal.title())
                .append(" pode seguir para implementação com rastreabilidade\n\n");
        sb.append("---\n\n");
    }

    private static void appendContracts(StringBuilder sb, StoryProposal proposal) {
        sb.append("## 5. Contratos\n\n### 5.1 Request\n\n");
        sb.append("| Campo | Tipo | M/O | Validações | Exemplo |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- |\n");
        sb.append("| `featureId` | `String` | M | match source feature | `derived-request` |\n\n");
        sb.append("### 5.2 Response\n\n");
        sb.append("| Campo | Tipo | Sempre presente | Descrição |\n");
        sb.append("| :--- | :--- | :--- | :--- |\n");
        sb.append("| `storyId` | `String` | Sim | ID derivado desta story |\n\n");
        sb.append("### 5.3 Error Codes\n\n");
        sb.append("| HTTP Status | Error Code | Condição |\n");
        sb.append("| :--- | :--- | :--- |\n");
        sb.append("| `400` | `FEATURE_DERIVATION_INVALID` | artefato pai inconsistente |\n\n");
        if (!proposal.inheritedAcs().isEmpty()) {
            sb.append("### 5.4 Acceptance Criteria Herdados\n\n");
            for (int i = 0; i < proposal.inheritedAcs().size(); i++) {
                sb.append(i + 1)
                        .append(". ")
                        .append(proposal.inheritedAcs().get(i).scenario())
                        .append("\n");
            }
            sb.append("\n");
        }
        sb.append("---\n\n");
    }

    private static void appendC4Sections(
            StringBuilder sb, FeatureEpicSource source, StoryProposal proposal) {
        appendC4Section(
                sb,
                "Context",
                "C4Context",
                proposal.title(),
                "System(story, \""
                        + source.featureId()
                        + "\", \"Story slice of the derived feature\")");
        appendC4Section(
                sb,
                "Container",
                "C4Container",
                proposal.title(),
                "Container(app, \"story-app\", \"Java\", \"Executes the derived workflow\")");
        appendC4Section(
                sb,
                "Component",
                "C4Component",
                proposal.title(),
                "Component(handler, \"story-handler\", \"Application\", \"Implements the derived use case\")");
        appendC4Section(
                sb,
                "Code",
                "classDiagram",
                proposal.title(),
                "class DerivedStory\nclass DerivedTask\nDerivedStory --> DerivedTask");
    }

    private static void appendC4Section(
            StringBuilder sb, String label, String diagramType, String title, String body) {
        sb.append("## C4 ").append(label).append("\n\n");
        sb.append("```mermaid\n");
        sb.append(diagramType).append("\n");
        sb.append("  title ").append(title).append("\n");
        sb.append("  ").append(body).append("\n");
        sb.append("```\n\n");
    }

    private static void appendTasks(StringBuilder sb, String epicId, StoryProposal proposal) {
        sb.append("## 6. Tasks\n\n");
        appendTask(sb, epicId, "001", "Modelar fluxo derivado", "—");
        appendTask(
                sb,
                epicId,
                "002",
                "Implementar contratos e adapters",
                "TASK-" + epicId + "-" + epicId + "-001");
        appendTask(
                sb,
                epicId,
                "003",
                "Cobrir cenários herdados",
                "TASK-" + epicId + "-" + epicId + "-002");
        sb.append("---\n\n");
    }

    private static void appendTask(
            StringBuilder sb, String epicId, String suffix, String title, String dependency) {
        String taskId = "TASK-" + epicId + "-" + epicId + "-" + suffix;
        sb.append("#### ").append(taskId).append(": ").append(title).append("\n\n");
        sb.append("- **Layer:** Application\n");
        sb.append("- **Test Type:** Unit\n");
        sb.append("- **Size:** M\n");
        sb.append("- **Dependencies:** ").append(dependency).append("\n");
        sb.append("- **Branch:** `feat/task-")
                .append(epicId)
                .append("-")
                .append(suffix)
                .append("-derived-story`\n");
        sb.append("- **Files:**\n");
        sb.append("  - `src/main/java/dev/iadev/...`\n");
        sb.append("- **Acceptance Criteria:**\n");
        sb.append("  - [ ] backlog derivado preservado\n\n");
    }

    private static void appendDependencies(StringBuilder sb) {
        sb.append("## 7. Dependências\n\n");
        sb.append("| Blocked By | Blocks |\n");
        sb.append("| :--- | :--- |\n");
        sb.append("| — | — |\n\n---\n\n");
    }

    private static void appendRefinementVerdict(StringBuilder sb) {
        sb.append("## 9. Refinement Verdict\n\n");
        sb.append("> _Slot reservado para `/x-refine-story`. Não editar manualmente._\n");
        sb.append(">\n");
        sb.append("> **Status:** TBD — execute `/x-refine-story <story-id>` para preencher.\n");
    }

    private static String normalizeCategory(String id) {
        String category = id.contains("-") ? id.substring(id.indexOf('-') + 1) : id;
        return category.replace("PROD-", "").replace("CAP-", "");
    }

    private static String proposalTitleSuffix(String featureId) {
        return "derivada de " + featureId;
    }

    private static String slugify(String value) {
        String sanitized =
                value.toLowerCase()
                        .replaceAll("[^a-z0-9-]+", "-")
                        .replaceAll("-{2,}", "-")
                        .replaceAll("^-|-$", "");
        return sanitized.isEmpty() ? "untitled" : sanitized;
    }
}

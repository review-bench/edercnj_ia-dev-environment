package dev.iadev.adapter.outbound.feature;

import dev.iadev.application.feature.FeatureEpicSource;
import dev.iadev.application.feature.InheritedRnfLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public final class EpicFromFeatureArtifactWriter {

    private EpicFromFeatureArtifactWriter() {}

    public static Path write(String epicId, FeatureEpicSource source, Path outputDir)
            throws IOException {
        Path epicDir = outputDir.resolve("epic-" + epicId + "-" + slugify(source.title()));
        Files.createDirectories(epicDir);
        Path epicFile = epicDir.resolve("epic-" + epicId + ".md");
        Files.writeString(epicFile, render(epicId, source));
        return epicFile;
    }

    static String render(String epicId, FeatureEpicSource source) {
        StringBuilder sb = new StringBuilder();
        appendHeader(sb, source);
        appendDependencies(sb);
        appendInheritedRnfs(sb, source.inheritedRnfs());
        appendVision(sb, source);
        appendHypothesis(sb, source);
        appendScope(sb, source);
        appendC4Sections(sb, source);
        appendRisks(sb);
        appendStoryIndex(sb, epicId, source.storyTitles());
        appendReferences(sb, source.references());
        appendRefinementVerdict(sb);
        return sb.toString();
    }

    private static void appendHeader(StringBuilder sb, FeatureEpicSource source) {
        sb.append("# Épico: ").append(source.title()).append("\n\n");
        sb.append("**Autor:** x-epic-create --from-feature\n");
        sb.append("**Data:** ").append(LocalDate.now()).append("\n");
        sb.append("**Versão:** 5.0\n");
        sb.append("**Status:** Pendente\n");
        sb.append("**Source Feature:** ").append(source.featureId()).append("\n");
        sb.append("**Source Feature Link:** ")
                .append(source.sourceFeatureLink())
                .append("\n\n---\n\n");
    }

    private static void appendDependencies(StringBuilder sb) {
        sb.append("## 0.5 Cross-Epic Dependencies\n\n");
        sb.append(
                "### Blocked By Epics\n\n| Epic ID | Title | Expected Status @ Start | Reason / Surface Touched |\n");
        sb.append(
                "| --------- | -------------------------- | ----------------------- | ------------------------------------------------------ |\n");
        sb.append("| (none) | — | — | — |\n\n");
        sb.append(
                "### Blocks (informational, derived)\n\n| Epic ID |\n| --------- |\n| (none) |\n\n---\n\n");
    }

    private static void appendInheritedRnfs(StringBuilder sb, List<InheritedRnfLine> rnfs) {
        sb.append("## 0.6 Inherited RNFs\n\n");
        sb.append("| RNF ID | Source Level | Requirement | Waivable? |\n");
        sb.append("| :--- | :--- | :--- | :--- |\n");
        for (InheritedRnfLine rnf : rnfs) {
            sb.append("| ")
                    .append(rnf.id())
                    .append(" | ")
                    .append(rnf.sourceLevel())
                    .append(" | ")
                    .append(rnf.requirement())
                    .append(" | ")
                    .append(rnf.waivable() ? "Yes" : "No")
                    .append(" |\n");
        }
        sb.append("\n---\n\n");
    }

    private static void appendVision(StringBuilder sb, FeatureEpicSource source) {
        sb.append("## 1. Visão & Problema\n\n");
        sb.append("**Chave Jira:** —\n\n");
        sb.append("Este épico deriva diretamente da feature `")
                .append(source.featureId())
                .append("` e preserva sua linhagem Product → Capability → Feature → Epic.\n\n");
        sb.append("O objetivo é transformar a feature `")
                .append(source.title())
                .append(
                        "` em backlog executável de épico, sem reescrever contexto já estabilizado em níveis anteriores.\n\n");
        sb.append("### 1.1 Referências e Contexto\n\n");
        for (String reference : source.references()) {
            sb.append("- ").append(reference).append("\n");
        }
        sb.append("\n---\n\n");
    }

    private static void appendHypothesis(StringBuilder sb, FeatureEpicSource source) {
        sb.append("## 3. Hipótese & OKRs\n\n");
        sb.append("### Hipótese de Valor\n\n");
        sb.append("**Se** convertermos a feature `")
                .append(source.featureId())
                .append("` em um épico rastreável,\n");
        sb.append(
                "**então** a execução posterior de stories preservará contexto, RNFs herdadas e origem funcional,\n");
        sb.append(
                "**porque** a cadeia Product-First reduz retranscrição manual e perda de contexto entre níveis.\n\n");
        sb.append("### OKRs\n\n");
        sb.append("| Objetivo | Key Result | Métrica | Valor Atual | Meta | Prazo |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n");
        sb.append(
                "| Materializar backlog da feature | Épico criado com sourceFeature e RNFs herdadas | artefato gerado | 0 | 1 | próximo ciclo |\n\n");
        sb.append("---\n\n");
    }

    private static void appendScope(StringBuilder sb, FeatureEpicSource source) {
        sb.append("## 5. Escopo\n\n### In-Scope\n\n");
        appendList(sb, source.inScope(), "- backlog derivado da feature");
        sb.append("\n### Out-of-Scope (explícito)\n\n");
        appendList(sb, source.outOfScope(), "- alterações fora da feature de origem");
        sb.append("\n### Dependências Técnicas\n\n");
        sb.append("| Dependência | Tipo | Épico/Ticket | Status |\n");
        sb.append("| :--- | :--- | :--- | :--- |\n");
        sb.append("| ")
                .append(source.capabilityId())
                .append(" | Bloqueante | ")
                .append(source.sourceFeatureLink())
                .append(" | known |\n\n---\n\n");
    }

    private static void appendC4Sections(StringBuilder sb, FeatureEpicSource source) {
        appendC4Section(
                sb,
                "Context",
                "C4Context",
                "Epic Context — " + source.featureId(),
                "System(epic, \"" + source.featureId() + "\", \"Derived epic boundary\")");
        appendC4Section(
                sb,
                "Container",
                "C4Container",
                "Epic Container — " + source.featureId(),
                "Container(orchestrator, \"planning-orchestrator\", \"Markdown\", \"Coordinates derived backlog\")");
        appendC4Section(
                sb,
                "Component",
                "C4Component",
                "Epic Component — " + source.featureId(),
                "Component(mapping, \"story-index-mapper\", \"Planner\", \"Maps feature UCs into stories\")");
        appendC4Section(
                sb,
                "Code",
                "classDiagram",
                "Epic Code — " + source.featureId(),
                "class DerivedEpic\nclass DerivedStoryIndex\nDerivedEpic --> DerivedStoryIndex");
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

    private static void appendRisks(StringBuilder sb) {
        sb.append("## 6. Riscos\n\n");
        sb.append("| # | Risco | Tipo | Probabilidade | Impacto | Mitigação |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n");
        sb.append(
                "| R1 | Divergência entre artefatos pai e épico derivado | Produto | Média | Alto | Regenerar a partir da source feature quando a origem mudar |\n");
        sb.append(
                "| R2 | RNF herdada não refletida no backlog | Técnico | Média | Alto | Gatear validação nas stories filhas com base nesta herança |\n\n");
        sb.append("---\n\n");
    }

    private static void appendStoryIndex(
            StringBuilder sb, String epicId, List<String> storyTitles) {
        sb.append("## 7. Índice de Histórias\n\n");
        sb.append(
                "| ID | Título | Dependências (Blocked By) | Entrega de Valor | Planejamento |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- |\n");
        if (storyTitles.isEmpty()) {
            sb.append("| [story-")
                    .append(epicId)
                    .append("-0001](./story-")
                    .append(epicId)
                    .append(
                            "-0001.md) | Backlog derivado da feature | — | Rastreabilidade completa da feature | Pendente |\n");
        } else {
            appendStoryRows(sb, epicId, storyTitles);
        }
        sb.append("\n---\n\n");
    }

    private static void appendStoryRows(StringBuilder sb, String epicId, List<String> storyTitles) {
        for (int i = 0; i < storyTitles.size(); i++) {
            String storyId = "story-" + epicId + "-" + "%04d".formatted(i + 1);
            String dependency = i == 0 ? "—" : "story-" + epicId + "-" + "%04d".formatted(i);
            sb.append("| [")
                    .append(storyId)
                    .append("](./")
                    .append(storyId)
                    .append(".md) | ")
                    .append(storyTitles.get(i))
                    .append(" | ")
                    .append(dependency)
                    .append(" | Entregar fatia executável da feature derivada | Pendente |\n");
        }
    }

    private static void appendReferences(StringBuilder sb, List<String> references) {
        sb.append("## 9. Origem & Referências\n\n");
        for (String reference : references) {
            sb.append("- ").append(reference).append("\n");
        }
        sb.append("\n---\n\n");
    }

    private static void appendRefinementVerdict(StringBuilder sb) {
        sb.append("## Refinement Verdict\n\n");
        sb.append("> _Slot reservado para `/x-refine-epic`. Não editar manualmente._\n");
        sb.append(">\n");
        sb.append("> **Status:** TBD — execute `/x-refine-epic <epic-id>` para preencher.\n");
    }

    private static void appendList(StringBuilder sb, List<String> items, String fallback) {
        if (items.isEmpty()) {
            sb.append(fallback).append("\n");
            return;
        }
        for (String item : items) {
            sb.append("- ").append(item).append("\n");
        }
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

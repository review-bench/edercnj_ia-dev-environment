package dev.iadev.application.feature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FeatureEpicSourceLoader {

    private static final Pattern FEATURE_TITLE = Pattern.compile("^# Feature:\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern FEATURE_ID = Pattern.compile("^\\*\\*Feature ID:\\*\\*\\s+([^\\s]+)", Pattern.MULTILINE);
    private static final Pattern CAPABILITY_ID =
            Pattern.compile("^\\*\\*Capability:\\*\\*\\s+([^\\s]+)", Pattern.MULTILINE);
    private static final Pattern PRODUCT_RNF_ROW =
            Pattern.compile("^\\|\\s*([A-Z_]+)\\s*\\|\\s*([^|]+)\\|\\s*([^|]+)\\|\\s*(Sim|Não)\\s*\\|",
                    Pattern.MULTILINE);
    private static final Pattern CAPABILITY_RNF_ROW =
            Pattern.compile("^\\|\\s*([A-Z_]+)\\s*\\|\\s*([^|]+)\\|\\s*(true|false)\\s*\\|\\s*([^|]+)\\|",
                    Pattern.MULTILINE);

    public FeatureEpicSource load(Path featureFile, Path capabilityFile, Path productFile) throws IOException {
        String featureContent = Files.readString(featureFile);
        String capabilityContent = capabilityFile == null ? "" : Files.readString(capabilityFile);
        String productContent = productFile == null ? "" : Files.readString(productFile);
        return new FeatureEpicSource(
                required(FEATURE_TITLE, featureContent, "feature title"),
                required(FEATURE_ID, featureContent, "feature id"),
                required(CAPABILITY_ID, featureContent, "capability id"),
                featureFile.toUri().toString(),
                extractBulletList(featureContent, "1.2 Escopo", "**In-scope:**"),
                extractBulletList(featureContent, "1.2 Escopo", "**Out-of-scope:**"),
                extractStoryTitles(featureContent),
                buildReferences(featureFile, capabilityFile, productFile),
                mergeRnfs(productContent, capabilityContent));
    }

    private static String required(Pattern pattern, String content, String label) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        throw new IllegalArgumentException(label + " not found in feature artifact");
    }

    private static List<String> extractBulletList(String content, String sectionMarker, String listMarker) {
        int sectionIndex = content.indexOf(sectionMarker);
        int listIndex = content.indexOf(listMarker, Math.max(sectionIndex, 0));
        if (listIndex < 0) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        for (String line : content.substring(listIndex).split("\n")) {
            if (line.startsWith("**") && !line.startsWith(listMarker)) {
                break;
            }
            if (line.startsWith("- ")) {
                items.add(line.substring(2).trim());
            }
        }
        return items;
    }

    private static List<String> extractStoryTitles(String featureContent) {
        List<String> storyTitles = new ArrayList<>();
        for (String line : featureContent.split("\n")) {
            if (line.startsWith("### UC-")) {
                storyTitles.add(line.substring(line.indexOf(':') + 1).trim());
            }
        }
        return storyTitles;
    }

    private static List<String> buildReferences(Path featureFile, Path capabilityFile, Path productFile) {
        List<String> references = new ArrayList<>();
        references.add(featureFile.toUri().toString());
        if (capabilityFile != null) {
            references.add(capabilityFile.toUri().toString());
        }
        if (productFile != null) {
            references.add(productFile.toUri().toString());
        }
        return references;
    }

    private static List<InheritedRnfLine> mergeRnfs(String productContent, String capabilityContent) {
        Map<String, InheritedRnfLine> merged = new LinkedHashMap<>();
        appendProductRnfs(merged, productContent);
        appendCapabilityRnfs(merged, capabilityContent);
        if (merged.isEmpty()) {
            return List.of(new InheritedRnfLine("RNF-N/A", "—", "—", false));
        }
        return List.copyOf(new LinkedHashSet<>(merged.values()));
    }

    private static void appendProductRnfs(Map<String, InheritedRnfLine> merged, String productContent) {
        Matcher matcher = PRODUCT_RNF_ROW.matcher(productContent);
        while (matcher.find()) {
            String category = matcher.group(1).trim();
            String requirement = category + " — " + matcher.group(2).trim() + " (" + matcher.group(3).trim() + ")";
            boolean waivable = "Não".equalsIgnoreCase(matcher.group(4).trim());
            merged.put("PROD-" + category, new InheritedRnfLine("PROD-" + category, "Product", requirement, waivable));
        }
    }

    private static void appendCapabilityRnfs(Map<String, InheritedRnfLine> merged, String capabilityContent) {
        Matcher matcher = CAPABILITY_RNF_ROW.matcher(capabilityContent);
        while (matcher.find()) {
            String category = matcher.group(1).trim();
            String original = matcher.group(2).trim();
            String override = matcher.group(4).trim();
            String effective = "—".equals(override) ? original : override;
            boolean waivable = !Boolean.parseBoolean(matcher.group(3).trim());
            merged.put(
                    "CAP-" + category,
                    new InheritedRnfLine("CAP-" + category, "Capability", category + " — " + effective, waivable));
        }
    }
}

package dev.iadev.application.feature;

import dev.iadev.domain.feature.AcceptanceCriterion;
import dev.iadev.domain.feature.Feature;
import dev.iadev.domain.feature.UseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FeatureMarkdownParser {

    private static final Pattern FEATURE_ID =
            Pattern.compile("^\\*\\*Feature ID:\\*\\*\\s+([^\\s]+)", Pattern.MULTILINE);
    private static final Pattern CAPABILITY_ID =
            Pattern.compile("^\\*\\*Capability:\\*\\*\\s+([^\\s]+)", Pattern.MULTILINE);
    private static final Pattern USE_CASE_BLOCK =
            Pattern.compile(
                    "### UC-[^\\n]+\\n\\n\\| Campo \\| Valor \\|\\n\\| :--- \\| :--- \\|\\n\\| \\*\\*Ator\\*\\* \\| ([^|]+)\\|\\n\\| \\*\\*Ação\\*\\* \\| ([^|]+)\\|\\n\\| \\*\\*Benefício\\*\\* \\| ([^|]+)\\|",
                    Pattern.MULTILINE);
    private static final Pattern SCENARIO = Pattern.compile("Cenário:\\s+(.+)$", Pattern.MULTILINE);

    public Feature parse(String content) {
        return new Feature(
                required(FEATURE_ID, content, "feature id"),
                required(CAPABILITY_ID, content, "capability id"),
                parseUseCases(content),
                parseAcceptanceCriteria(content));
    }

    private static String required(Pattern pattern, String content, String label) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        throw new IllegalArgumentException(label + " not found in feature artifact");
    }

    private static List<UseCase> parseUseCases(String content) {
        List<UseCase> useCases = new ArrayList<>();
        Matcher matcher = USE_CASE_BLOCK.matcher(content);
        while (matcher.find()) {
            useCases.add(
                    new UseCase(
                            matcher.group(1).trim(),
                            matcher.group(2).trim(),
                            matcher.group(3).trim()));
        }
        return useCases;
    }

    private static List<AcceptanceCriterion> parseAcceptanceCriteria(String content) {
        List<AcceptanceCriterion> criteria = new ArrayList<>();
        Matcher matcher = SCENARIO.matcher(content);
        while (matcher.find()) {
            criteria.add(new AcceptanceCriterion(matcher.group(1).trim()));
        }
        return criteria;
    }
}

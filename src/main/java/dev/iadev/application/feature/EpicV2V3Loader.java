package dev.iadev.application.feature;

import dev.iadev.domain.feature.SourceFeatureReference;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EpicV2V3Loader {

    private static final Pattern SOURCE_FEATURE_PATTERN =
            Pattern.compile(
                    "^\\*\\*Source Feature:\\*\\*\\s+(.+?)\\s*(?:_.*)?$", Pattern.MULTILINE);
    private static final Pattern SOURCE_FEATURE_LINK_PATTERN =
            Pattern.compile("^\\*\\*Source Feature Link:\\*\\*\\s+(.+?)\\s*$", Pattern.MULTILINE);

    public SourceFeatureReference parseSourceFeature(String epicMarkdownContent) {
        Optional<String> featureId = extract(SOURCE_FEATURE_PATTERN, epicMarkdownContent);
        if (featureId.isEmpty()) {
            return SourceFeatureReference.notApplicable();
        }
        String id = featureId.get();
        String link = extract(SOURCE_FEATURE_LINK_PATTERN, epicMarkdownContent).orElse("—");
        return SourceFeatureReference.of(id, link);
    }

    private Optional<String> extract(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return Optional.of(matcher.group(1).trim());
        }
        return Optional.empty();
    }
}

package dev.iadev.application.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a structured markdown epic document into named sections.
 *
 * <p>Section headers are detected by {@code ## } (H2) or {@code ### } (H3) prefix. Each section
 * accumulates its body lines until the next header of the same or higher level. Parsing is
 * deterministic: the same input always produces the same section map.
 */
public final class EpicMarkdownParser {

    private EpicMarkdownParser() {}

    /**
     * Parses {@code content} into an ordered map of {@code header → body lines}.
     *
     * <p>Headers are stored without the leading {@code #} characters or surrounding whitespace.
     * Body lines preserve leading whitespace but strip trailing blank lines per section.
     */
    public static Map<String, List<String>> parse(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String currentHeader = null;
        List<String> currentBody = new ArrayList<>();

        for (String line : content.split("\n", -1)) {
            if (line.startsWith("## ") || line.startsWith("### ")) {
                if (currentHeader != null) {
                    sections.put(currentHeader, trimTrailingBlanks(currentBody));
                    currentBody = new ArrayList<>();
                }
                currentHeader = line.replaceFirst("^#{2,3}\\s+", "").strip();
            } else if (currentHeader != null) {
                currentBody.add(line);
            }
        }
        if (currentHeader != null) {
            sections.put(currentHeader, trimTrailingBlanks(currentBody));
        }
        return Collections.unmodifiableMap(sections);
    }

    private static List<String> trimTrailingBlanks(List<String> lines) {
        int last = lines.size() - 1;
        while (last >= 0 && lines.get(last).isBlank()) {
            last--;
        }
        return Collections.unmodifiableList(new ArrayList<>(lines.subList(0, last + 1)));
    }
}

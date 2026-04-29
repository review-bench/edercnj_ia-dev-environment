package dev.iadev.application.composition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves slot/each fragment markers in parent artifact bodies — runs before Pebble (RULE-005).
 *
 * <p>Supported markers:
 * <ul>
 *   <li>{@code {{ slot: name }}} — replaced by concatenated fragment bodies matching that slot
 *   <li>{@code {{ #each fragments.name }} ... {{ /each }}} — iterates fragments for that slot
 * </ul>
 *
 * <p>LLM placeholders ({@code {{UPPER_SNAKE}}}) are preserved unchanged.
 * Fragments ordered by {@code fragment-order} ascending, then alphabetical by fragment-id (RULE-004).
 */
public final class CompositionEngine {

    private static final Pattern SLOT_PATTERN =
            Pattern.compile("\\{\\{\\s*slot:\\s*([\\w-]+)\\s*\\}\\}");
    private static final Pattern EACH_PATTERN =
            Pattern.compile("\\{\\{\\s*#each\\s+fragments\\.([\\w-]+)\\s*\\}\\}(.*?)\\{\\{\\s*/each\\s*\\}\\}",
                    Pattern.DOTALL);

    public record Fragment(String slotName, String fragmentId, int fragmentOrder, String body) {}

    public String render(String parentBody, List<Fragment> fragments) {
        Objects.requireNonNull(parentBody, "parentBody must not be null");
        if (fragments == null || fragments.isEmpty()) return resolveEmptySlots(parentBody);

        Map<String, List<Fragment>> bySlot = groupBySlot(fragments);
        String result = resolveEachBlocks(parentBody, bySlot);
        result = resolveSlots(result, bySlot);
        return result;
    }

    private String resolveSlots(String body, Map<String, List<Fragment>> bySlot) {
        Matcher m = SLOT_PATTERN.matcher(body);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String slotName = m.group(1);
            List<Fragment> slotFragments = bySlot.getOrDefault(slotName, List.of());
            String replacement = buildSlotContent(slotFragments);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String resolveEachBlocks(String body, Map<String, List<Fragment>> bySlot) {
        Matcher m = EACH_PATTERN.matcher(body);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String slotName = m.group(1);
            String template = m.group(2);
            List<Fragment> slotFragments = bySlot.getOrDefault(slotName, List.of());
            String expanded = expandEach(slotFragments, template);
            m.appendReplacement(sb, Matcher.quoteReplacement(expanded));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String expandEach(List<Fragment> fragments, String template) {
        if (fragments.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Fragment f : fragments) {
            String item = template
                    .replace("{{ fragment-id }}", f.fragmentId())
                    .replace("{{ description }}", f.body().lines().findFirst().orElse(""))
                    .replace("{{ body }}", f.body());
            sb.append(item);
        }
        return sb.toString();
    }

    private String buildSlotContent(List<Fragment> fragments) {
        if (fragments.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Fragment f : fragments) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(f.body());
        }
        return sb.toString();
    }

    private String resolveEmptySlots(String body) {
        String noSlots = SLOT_PATTERN.matcher(body).replaceAll("");
        return EACH_PATTERN.matcher(noSlots).replaceAll("");
    }

    private Map<String, List<Fragment>> groupBySlot(List<Fragment> fragments) {
        Map<String, List<Fragment>> map = new java.util.LinkedHashMap<>();
        for (Fragment f : fragments) {
            map.computeIfAbsent(f.slotName(), k -> new ArrayList<>()).add(f);
        }
        Comparator<Fragment> order = Comparator.comparingInt(Fragment::fragmentOrder)
                .thenComparing(Fragment::fragmentId);
        map.values().forEach(list -> list.sort(order));
        return map;
    }
}

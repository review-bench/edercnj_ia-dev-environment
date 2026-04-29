package dev.iadev.application.composition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompositionEngine")
class CompositionEngineTest {

    private final CompositionEngine engine = new CompositionEngine();

    private static CompositionEngine.Fragment frag(String slot, String id, int order, String body) {
        return new CompositionEngine.Fragment(slot, id, order, body);
    }

    @Nested
    @DisplayName("slot resolution")
    class SlotResolution {

        @Test
        @DisplayName("empty slot (no fragments) resolves to empty string (degenerate)")
        void emptySlotResolves() {
            String result = engine.render("before {{ slot: missing-slot }} after", List.of());
            assertThat(result).isEqualTo("before  after");
        }

        @Test
        @DisplayName("slot resolved by matching fragment (happy)")
        void slotResolvedByFragment() {
            var frag = frag("review-specialist", "db", 10, "## DB Review\nContent here.");
            String result = engine.render("Header\n{{ slot: review-specialist }}\nFooter",
                    List.of(frag));
            assertThat(result).contains("## DB Review");
            assertThat(result).startsWith("Header");
            assertThat(result).endsWith("Footer");
        }

        @Test
        @DisplayName("LLM placeholders {{UPPER_SNAKE}} are preserved unchanged (RULE-005)")
        void llmPlaceholdersPreserved() {
            String result = engine.render("{{PLACEHOLDER}} {{ slot: empty-slot }}", List.of());
            assertThat(result).contains("{{PLACEHOLDER}}");
        }
    }

    @Nested
    @DisplayName("#each resolution")
    class EachResolution {

        @Test
        @DisplayName("#each iterates fragments in order (RULE-004 determinism)")
        void eachIteratesInOrder() {
            var frag1 = frag("specialist", "db", 30, "DB Specialist");
            var frag2 = frag("specialist", "qa", 10, "QA Specialist");
            var frag3 = frag("specialist", "perf", 20, "Perf Specialist");

            String template = "{{ #each fragments.specialist }}\n- {{ fragment-id }}\n{{ /each }}";
            String result = engine.render(template, List.of(frag1, frag2, frag3));
            int qaIdx = result.indexOf("qa");
            int perfIdx = result.indexOf("perf");
            int dbIdx = result.indexOf("db");
            assertThat(qaIdx).isLessThan(perfIdx).isLessThan(dbIdx);
        }
    }

    @Nested
    @DisplayName("multiple slots in one body")
    class MultipleSlots {

        @Test
        @DisplayName("two slots in same body both resolved")
        void twoSlotsResolved() {
            var f1 = frag("slot-a", "id1", 10, "Content A");
            var f2 = frag("slot-b", "id2", 10, "Content B");
            String result = engine.render("{{slot: slot-a}} and {{slot: slot-b}}", List.of(f1, f2));
            assertThat(result).contains("Content A").contains("Content B");
        }

        @Test
        @DisplayName("each block with no matching fragments renders empty string")
        void eachWithNoFragmentsEmpty() {
            String result = engine.render("{{ #each fragments.absent-slot }}item{{ /each }}", List.of());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("each block expands each fragment with body template")
        void eachExpandsBody() {
            var f1 = frag("spec", "qa", 10, "QA Body");
            var f2 = frag("spec", "perf", 20, "Perf Body");
            String result = engine.render(
                    "{{ #each fragments.spec }}\n{{ body }}\n{{ /each }}", List.of(f1, f2));
            assertThat(result).contains("QA Body").contains("Perf Body");
        }
    }

    @Nested
    @DisplayName("no markers")
    class NoMarkers {

        @Test
        @DisplayName("multiple fragments concatenated with newline separator")
        void multipleFragmentsConcatenated() {
            var f1 = frag("slot-x", "a", 10, "Part A");
            var f2 = frag("slot-x", "b", 20, "Part B");
            String result = engine.render("{{ slot: slot-x }}", List.of(f1, f2));
            assertThat(result).contains("Part A").contains("Part B");
        }

        @Test
        @DisplayName("body without markers passes through unchanged")
        void noMarkersPassThrough() {
            String body = "# Title\nSome content with no markers.";
            assertThat(engine.render(body, List.of())).isEqualTo(body);
        }
    }
}

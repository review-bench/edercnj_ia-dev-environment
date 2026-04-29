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
    @DisplayName("no markers")
    class NoMarkers {

        @Test
        @DisplayName("body without markers passes through unchanged")
        void noMarkersPassThrough() {
            String body = "# Title\nSome content with no markers.";
            assertThat(engine.render(body, List.of())).isEqualTo(body);
        }
    }
}

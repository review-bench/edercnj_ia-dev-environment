package dev.iadev.application.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MemoryFrontmatterAssemblerTest {

    private static final String DATE = "2026-05-01";

    @Test
    void assemble_staticFields_setCorrectly() {
        MemoryFrontmatter fm = assemble("", List.of(), List.of());

        assertThat(fm.epicId()).isEqualTo("EPIC-0067");
        assertThat(fm.slug()).isEqualTo("review-yaml-frontmatter");
        assertThat(fm.summaryVersion()).isEqualTo("1.0");
        assertThat(fm.created()).isEqualTo(DATE);
        assertThat(fm.lastUpdated()).isEqualTo(DATE);
        assertThat(fm.indexable()).isTrue();
        assertThat(fm.archived()).isFalse();
        assertThat(fm.supersededBy()).isNull();
    }

    @Test
    void assemble_tagsAndCapabilitiesAffected_alwaysEmpty() {
        MemoryFrontmatter fm = assemble("", List.of(), List.of());

        assertThat(fm.tags()).isEmpty();
        assertThat(fm.capabilitiesAffected()).isEmpty();
    }

    @Test
    void assemble_extractsRulesFromContent() {
        String content = "Refers to Rule 28 and also Rule 24 here.";

        MemoryFrontmatter fm = assemble(content, List.of(), List.of());

        assertThat(fm.rulesAffected()).containsExactlyInAnyOrder("Rule 28", "Rule 24");
    }

    @Test
    void assemble_deduplicatesRules() {
        String content = "See Rule 28. Also Rule 28 again.";

        MemoryFrontmatter fm = assemble(content, List.of(), List.of());

        assertThat(fm.rulesAffected()).containsExactly("Rule 28");
    }

    @Test
    void assemble_extractsAdrsFromContent() {
        String content = "Decision in ADR-0016 and cross-ref ADR-0024.";

        MemoryFrontmatter fm = assemble(content, List.of(), List.of());

        assertThat(fm.adrsReferenced()).containsExactlyInAnyOrder("ADR-0016", "ADR-0024");
    }

    @Test
    void assemble_deduplicatesAdrs() {
        String content = "ADR-0016 referenced twice, see ADR-0016.";

        MemoryFrontmatter fm = assemble(content, List.of(), List.of());

        assertThat(fm.adrsReferenced()).containsExactly("ADR-0016");
    }

    @Test
    void assemble_extractsDependsOn() {
        String content = "Depende de: EPIC-0064\nOther text.";

        MemoryFrontmatter fm = assemble(content, List.of(), List.of());

        assertThat(fm.dependenciesOf()).containsExactly("EPIC-0064");
    }

    @Test
    void assemble_extractsBlocks() {
        String content = "Blocks: EPIC-0070";

        MemoryFrontmatter fm = assemble(content, List.of(), List.of());

        assertThat(fm.dependenciesFor()).containsExactly("EPIC-0070");
    }

    @Test
    void assemble_noDependencies_emptyLists() {
        MemoryFrontmatter fm = assemble("No deps here.", List.of(), List.of());

        assertThat(fm.dependenciesOf()).isEmpty();
        assertThat(fm.dependenciesFor()).isEmpty();
    }

    @Test
    void assemble_patternsPassedThrough() {
        List<String> patterns = List.of("capability-aware-skill-via-frontmatter");
        List<String> antiPatterns = List.of("inline-validation-instead-of-hook");

        MemoryFrontmatter fm = assemble("", patterns, antiPatterns);

        assertThat(fm.patternsIntroduced())
                .containsExactly("capability-aware-skill-via-frontmatter");
        assertThat(fm.antipatternsRejected()).containsExactly("inline-validation-instead-of-hook");
    }

    @Test
    void assemble_deterministic_sameInputSameOutput() {
        String content = "Rule 28 Rule 24 ADR-0016";

        MemoryFrontmatter first = assemble(content, List.of(), List.of());
        MemoryFrontmatter second = assemble(content, List.of(), List.of());

        assertThat(first).isEqualTo(second);
        assertThat(first.rulesAffected()).isEqualTo(second.rulesAffected());
        assertThat(first.adrsReferenced()).isEqualTo(second.adrsReferenced());
    }

    @Test
    void assemble_listsAreUnmodifiable() {
        MemoryFrontmatter fm = assemble("Rule 5", List.of("p"), List.of("a"));

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> fm.rulesAffected().add("x"));
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> fm.patternsIntroduced().add("x"));
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> fm.antipatternsRejected().add("x"));
    }

    @Test
    void assemble_noRulesOrAdrs_emptyLists() {
        MemoryFrontmatter fm = assemble("plain text no special refs", List.of(), List.of());

        assertThat(fm.rulesAffected()).isEmpty();
        assertThat(fm.adrsReferenced()).isEmpty();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static MemoryFrontmatter assemble(
            String content, List<String> patterns, List<String> antiPatterns) {
        return MemoryFrontmatterAssembler.assemble(
                "EPIC-0067", "review-yaml-frontmatter", content, patterns, antiPatterns, DATE);
    }
}

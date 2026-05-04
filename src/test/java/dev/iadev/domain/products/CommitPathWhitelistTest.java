package dev.iadev.domain.products;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CommitPathWhitelist")
class CommitPathWhitelistTest {

    @Nested
    @DisplayName("standard()")
    class StandardTests {

        @Test
        void standard_containsPlansPrefix() {
            assertThat(CommitPathWhitelist.standard().isAllowed("plans/arch.md")).isTrue();
        }

        @Test
        void standard_containsClaudeTemplatesPrefix() {
            assertThat(CommitPathWhitelist.standard().isAllowed(".claude/templates/FOO.md")).isTrue();
        }

        @Test
        void standard_containsAiEpicsPrefix() {
            assertThat(CommitPathWhitelist.standard().isAllowed("ai/epics/epic-0077/plans/arch.md")).isTrue();
        }

        @Test
        void standard_containsAiProductsPrefix() {
            assertThat(CommitPathWhitelist.standard().isAllowed("ai/products/product-0001/capabilities/cap.md")).isTrue();
        }

        @Test
        void standard_containsAiMemoryPrefix() {
            assertThat(CommitPathWhitelist.standard().isAllowed("ai/memory/epic-0077-summary.md")).isTrue();
        }

        @Test
        void standard_containsAiReleasesPrefix() {
            assertThat(CommitPathWhitelist.standard().isAllowed("ai/releases/release-state-5.2.0.json")).isTrue();
        }
    }

    @Nested
    @DisplayName("isAllowed()")
    class IsAllowedTests {

        @Test
        void isAllowed_srcMainPath_returnsFalse() {
            assertThat(CommitPathWhitelist.standard().isAllowed("src/main/java/Foo.java")).isFalse();
        }

        @Test
        void isAllowed_docsPath_returnsFalse() {
            assertThat(CommitPathWhitelist.standard().isAllowed("docs/adr/ADR-0031.md")).isFalse();
        }

        @Test
        void isAllowed_emptyPath_returnsFalse() {
            assertThat(CommitPathWhitelist.standard().isAllowed("")).isFalse();
        }

        @Test
        void isAllowed_nullPath_throwsIllegalArgument() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> CommitPathWhitelist.standard().isAllowed(null));
        }

        @Test
        void isAllowed_exactPrefixMatch_returnsTrue() {
            var wl = CommitPathWhitelist.of(Set.of("ai/products/"));
            assertThat(wl.isAllowed("ai/products/product-0001/foo.md")).isTrue();
        }
    }

    @Nested
    @DisplayName("of()")
    class OfTests {

        @Test
        void of_nullPrefixes_throwsIllegalArgument() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> CommitPathWhitelist.of(null));
        }

        @Test
        void of_defensiveCopy_mutationDoesNotAffect() {
            var source = new HashSet<String>();
            source.add("ai/products/");
            var wl = CommitPathWhitelist.of(source);
            source.add("src/");
            assertThat(wl.isAllowed("src/main/Foo.java")).isFalse();
        }
    }

    @Nested
    @DisplayName("equality")
    class EqualityTests {

        @Test
        void equality_samePrefixes_equal() {
            var a = CommitPathWhitelist.of(Set.of("plans/"));
            var b = CommitPathWhitelist.of(Set.of("plans/"));
            assertThat(a).isEqualTo(b);
        }

        @Test
        void equality_differentPrefixes_notEqual() {
            var a = CommitPathWhitelist.of(Set.of("plans/"));
            var b = CommitPathWhitelist.of(Set.of("ai/products/"));
            assertThat(a).isNotEqualTo(b);
        }
    }
}

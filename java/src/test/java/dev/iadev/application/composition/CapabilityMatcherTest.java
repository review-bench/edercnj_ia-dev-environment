package dev.iadev.application.composition;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CapabilityMatcher")
class CapabilityMatcherTest {

    private final CapabilityMatcher matcher = new CapabilityMatcher();

    private static ResolvedCapabilitySet activeSet(List<String> ids) {
        return new ResolvedCapabilitySet("test",
                ids.stream().map(CapabilityId::of).toList(), Map.of(), List.of());
    }

    @Nested
    @DisplayName("empty requires — universal")
    class Universal {

        @Test
        @DisplayName("empty requires-capabilities always included")
        void emptyRequiresIncluded() {
            var result = matcher.matches(List.of(), activeSet(List.of()));
            assertThat(result.included()).isTrue();
            assertThat(result.excludeReason()).isNull();
        }
    }

    @Nested
    @DisplayName("non-empty requires")
    class NonEmpty {

        @Test
        @DisplayName("matching canonical capability includes artifact")
        void matchingCapabilityIncludes() {
            var result = matcher.matches(
                    List.of("data.database.postgres"),
                    activeSet(List.of("data.database.postgres")));
            assertThat(result.included()).isTrue();
        }

        @Test
        @DisplayName("non-matching capability excludes artifact with reason")
        void nonMatchingExcludesWithReason() {
            var result = matcher.matches(
                    List.of("data.database.postgres"),
                    activeSet(List.of("framework.spring-boot.mvc")));
            assertThat(result.included()).isFalse();
            assertThat(result.excludeReason()).isNotNull().contains("no matching capability");
        }

        @Test
        @DisplayName("glob requires matches active atomics")
        void globRequiresMatchesAtomics() {
            var result = matcher.matches(
                    List.of("data.database.*"),
                    activeSet(List.of("data.database.postgres")));
            assertThat(result.included()).isTrue();
        }

        @Test
        @DisplayName("glob requires does not match different subcategory")
        void globRequiresDoesNotMatchOtherSubcategory() {
            var result = matcher.matches(
                    List.of("data.database.*"),
                    activeSet(List.of("data.cache.redis")));
            assertThat(result.included()).isFalse();
        }

        @Test
        @DisplayName("invalid capability ID in requires is silently skipped (graceful fallback)")
        void invalidCapabilityIdSkipped() {
            var result = matcher.matches(
                    List.of("invalid-id-no-dots"),
                    activeSet(List.of("data.database.postgres")));
            assertThat(result.included()).isFalse();
        }

        @Test
        @DisplayName("any one of multiple requires matching is sufficient")
        void anyOneRequiresMatchSuffices() {
            var result = matcher.matches(
                    List.of("data.database.mysql", "data.database.postgres"),
                    activeSet(List.of("data.database.postgres")));
            assertThat(result.included()).isTrue();
        }
    }
}

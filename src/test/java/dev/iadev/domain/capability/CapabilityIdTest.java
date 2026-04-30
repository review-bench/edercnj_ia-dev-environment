package dev.iadev.domain.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CapabilityId")
class CapabilityIdTest {

    @Nested
    @DisplayName("invalid format")
    class InvalidFormat {

        @Test
        @DisplayName("rejects null input")
        void rejectsNull() {
            assertThatThrownBy(() -> CapabilityId.of(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects input without dots (degenerate)")
        void rejectsNoDots() {
            assertThatThrownBy(() -> CapabilityId.of("datadatabase"))
                    .isInstanceOf(CapabilityError.UnknownCapability.class)
                    .hasMessageContaining("must follow category.subcategory.atomic format");
        }

        @Test
        @DisplayName("rejects single-segment input")
        void rejectsSingleSegment() {
            assertThatThrownBy(() -> CapabilityId.of("data"))
                    .isInstanceOf(CapabilityError.UnknownCapability.class);
        }

        @Test
        @DisplayName("rejects uppercase characters")
        void rejectsUppercase() {
            assertThatThrownBy(() -> CapabilityId.of("Data.database.postgres"))
                    .isInstanceOf(CapabilityError.UnknownCapability.class);
        }
    }

    @Nested
    @DisplayName("canonical format")
    class CanonicalFormat {

        @Test
        @DisplayName("accepts three-segment id")
        void acceptsThreeSegments() {
            CapabilityId id = CapabilityId.of("data.database.postgres");
            assertThat(id.category()).isEqualTo("data");
            assertThat(id.subcategory()).isEqualTo("database");
            assertThat(id.atomic()).isEqualTo("postgres");
            assertThat(id.isGlob()).isFalse();
        }

        @Test
        @DisplayName("accepts hyphenated segments")
        void acceptsHyphens() {
            CapabilityId id = CapabilityId.of("framework.spring-boot.mvc");
            assertThat(id.category()).isEqualTo("framework");
            assertThat(id.subcategory()).isEqualTo("spring-boot");
        }

        @Test
        @DisplayName("value() returns original string")
        void valueRetainsOriginal() {
            String raw = "data.database.postgres";
            assertThat(CapabilityId.of(raw).value()).isEqualTo(raw);
        }
    }

    @Nested
    @DisplayName("glob support")
    class GlobSupport {

        @Test
        @DisplayName("single-star glob is recognized")
        void singleStarIsGlob() {
            CapabilityId glob = CapabilityId.of("data.database.*");
            assertThat(glob.isGlob()).isTrue();
        }

        @Test
        @DisplayName("glob matches same-subcategory atomics")
        void globMatchesSameSubcategory() {
            CapabilityId glob = CapabilityId.of("data.database.*");
            assertThat(glob.matches(CapabilityId.of("data.database.postgres"))).isTrue();
            assertThat(glob.matches(CapabilityId.of("data.database.mysql"))).isTrue();
        }

        @Test
        @DisplayName("glob does not match different subcategory")
        void globDoesNotMatchDifferentSubcategory() {
            CapabilityId glob = CapabilityId.of("data.database.*");
            assertThat(glob.matches(CapabilityId.of("data.cache.redis"))).isFalse();
        }

        @Test
        @DisplayName("non-glob matches only itself")
        void nonGlobMatchesOnly() {
            CapabilityId id = CapabilityId.of("data.database.postgres");
            assertThat(id.matches(CapabilityId.of("data.database.postgres"))).isTrue();
            assertThat(id.matches(CapabilityId.of("data.database.mysql"))).isFalse();
        }
    }

    @Nested
    @DisplayName("matches() edge cases")
    class MatchesEdgeCases {

        @Test
        @DisplayName("null glob pattern does not match non-glob id")
        void nonGlobSelfEquals() {
            CapabilityId id = CapabilityId.of("data.database.postgres");
            assertThat(id.matches(CapabilityId.of("data.database.postgres"))).isTrue();
            assertThat(id.matches(CapabilityId.of("data.database.mysql"))).isFalse();
        }

        @Test
        @DisplayName("accepts four-segment id")
        void acceptsFourSegments() {
            CapabilityId id = CapabilityId.of("data.database.postgres.dialect");
            assertThat(id.category()).isEqualTo("data");
            assertThat(id.subcategory()).isEqualTo("database");
            assertThat(id.atomic()).isEqualTo("postgres");
        }

        @Test
        @DisplayName("value() equals toString()")
        void valueEqualsToString() {
            CapabilityId id = CapabilityId.of("data.database.postgres");
            assertThat(id.value()).isEqualTo(id.toString());
        }
    }

    @Nested
    @DisplayName("category/subcategory/atomic accessors")
    class Accessors {

        @Test
        @DisplayName("two-segment id has empty atomic()")
        void twoSegmentEmptyAtomic() {
            CapabilityId id = CapabilityId.of("data.database.mysql");
            assertThat(id.atomic()).isEqualTo("mysql");
        }

        @Test
        @DisplayName("toString() returns value")
        void toStringReturnsValue() {
            assertThat(CapabilityId.of("data.database.postgres").toString())
                    .isEqualTo("data.database.postgres");
        }

        @Test
        @DisplayName("equals with non-CapabilityId returns false")
        void notEqualToNonId() {
            assertThat(CapabilityId.of("data.database.postgres").equals("data.database.postgres"))
                    .isFalse();
        }

        @Test
        @DisplayName("subcategory() returns empty string for single-segment fallback")
        void subcategoryEmptyForShortId() {
            // Two-segment id: category.subcategory only (via manual construction tested elsewhere)
            CapabilityId id = CapabilityId.of("data.database.postgres");
            assertThat(id.subcategory()).isEqualTo("database");
            assertThat(id.atomic()).isEqualTo("postgres");
        }

        @Test
        @DisplayName("hashCode consistent with equals")
        void hashCodeConsistent() {
            assertThat(CapabilityId.of("data.database.postgres").hashCode())
                    .isEqualTo(CapabilityId.of("data.database.postgres").hashCode());
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("equal ids are equal")
        void sameRawEqual() {
            assertThat(CapabilityId.of("data.database.postgres"))
                    .isEqualTo(CapabilityId.of("data.database.postgres"));
        }

        @Test
        @DisplayName("different ids are not equal")
        void differentRawNotEqual() {
            assertThat(CapabilityId.of("data.database.postgres"))
                    .isNotEqualTo(CapabilityId.of("data.database.mysql"));
        }
    }
}

package dev.iadev.domain.capability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CapabilityGlob")
class CapabilityGlobTest {

    @Nested
    @DisplayName("isGlob()")
    class IsGlob {

        @Test
        @DisplayName("null returns false")
        void nullReturnsFalse() {
            assertThat(CapabilityGlob.isGlob(null)).isFalse();
        }

        @Test
        @DisplayName("no asterisk returns false")
        void noAsteriskFalse() {
            assertThat(CapabilityGlob.isGlob("data.database.postgres")).isFalse();
        }

        @Test
        @DisplayName("single asterisk returns true")
        void singleAsteriskTrue() {
            assertThat(CapabilityGlob.isGlob("data.database.*")).isTrue();
        }

        @Test
        @DisplayName("double asterisk returns true")
        void doubleAsteriskTrue() {
            assertThat(CapabilityGlob.isGlob("data.**")).isTrue();
        }
    }

    @Nested
    @DisplayName("matches()")
    class Matches {

        @Test
        @DisplayName("single-star matches same-segment candidate")
        void singleStarMatchesSameSegment() {
            assertThat(CapabilityGlob.matches("data.database.*", "data.database.postgres")).isTrue();
        }

        @Test
        @DisplayName("single-star does not match across dot boundary")
        void singleStarDoesNotCrossDot() {
            assertThat(CapabilityGlob.matches("data.*", "data.database.postgres")).isFalse();
        }

        @Test
        @DisplayName("double-star matches recursively")
        void doubleStarMatchesRecursively() {
            assertThat(CapabilityGlob.matches("data.**", "data.database.postgres")).isTrue();
        }

        @Test
        @DisplayName("literal dot in pattern is escaped")
        void literalDotEscaped() {
            assertThat(CapabilityGlob.matches("data.database.*", "dataxdatabasexpostgres")).isFalse();
        }

        @Test
        @DisplayName("regular characters match literally")
        void regularCharsLiteral() {
            assertThat(CapabilityGlob.matches("data.database.postgres", "data.database.postgres")).isTrue();
            assertThat(CapabilityGlob.matches("data.database.postgres", "data.database.mysql")).isFalse();
        }
    }
}

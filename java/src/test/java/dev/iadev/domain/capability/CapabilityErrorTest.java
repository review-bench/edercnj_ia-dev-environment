package dev.iadev.domain.capability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CapabilityError")
class CapabilityErrorTest {

    @Nested
    @DisplayName("MissingPrerequisite")
    class MissingPrerequisiteTest {

        @Test
        @DisplayName("referredId() and referrerId() accessible")
        void contextAccessible() {
            var err = new CapabilityError.MissingPrerequisite("msg", "web.unknown", "web.spring.boot");
            assertThat(err.referredId()).isEqualTo("web.unknown");
            assertThat(err.referrerId()).isEqualTo("web.spring.boot");
            assertThat(err.getMessage()).isEqualTo("msg");
        }

        @Test
        @DisplayName("remediationHint() includes file path suggestion")
        void remediationHintIncludesPath() {
            var err = new CapabilityError.MissingPrerequisite("msg", "data.database.postgres", "");
            assertThat(err.remediationHint()).contains("capabilities/data/database/postgres.yaml");
        }

        @Test
        @DisplayName("remediationHint() empty when referredId is empty")
        void remediationHintEmptyForBlankId() {
            var err = new CapabilityError.MissingPrerequisite("msg");
            assertThat(err.remediationHint()).isEmpty();
        }
    }

    @Nested
    @DisplayName("CyclicDependency")
    class CyclicDependencyTest {

        @Test
        @DisplayName("cyclePath() returns the cycle")
        void cyclePathAccessible() {
            var err = new CapabilityError.CyclicDependency("cycle", List.of("a.b.c", "a.b.d", "a.b.c"));
            assertThat(err.cyclePath()).containsExactly("a.b.c", "a.b.d", "a.b.c");
        }

        @Test
        @DisplayName("single-arg constructor has empty cyclePath")
        void singleArgEmptyPath() {
            var err = new CapabilityError.CyclicDependency("cycle");
            assertThat(err.cyclePath()).isEmpty();
        }
    }

    @Nested
    @DisplayName("AsymmetricMutex")
    class AsymmetricMutexTest {

        @Test
        @DisplayName("AsymmetricMutex has message")
        void asymmetricMutexHasMessage() {
            var err = new CapabilityError.AsymmetricMutex("asymmetric");
            assertThat(err.getMessage()).isEqualTo("asymmetric");
        }
    }

    @Nested
    @DisplayName("MutexConflict and others")
    class OtherErrors {

        @Test
        @DisplayName("MutexConflict has message")
        void mutexConflictHasMessage() {
            var err = new CapabilityError.MutexConflict("asymmetric mutex: A excludes B");
            assertThat(err.getMessage()).contains("asymmetric mutex");
        }

        @Test
        @DisplayName("UnknownCapability has message")
        void unknownCapabilityHasMessage() {
            var err = new CapabilityError.UnknownCapability("not found");
            assertThat(err.getMessage()).isEqualTo("not found");
        }

        @Test
        @DisplayName("InvalidExpression has message")
        void invalidExpressionHasMessage() {
            var err = new CapabilityError.InvalidExpression("bad expr");
            assertThat(err.getMessage()).isEqualTo("bad expr");
        }
    }
}

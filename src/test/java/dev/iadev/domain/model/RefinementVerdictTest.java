package dev.iadev.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RefinementVerdict")
class RefinementVerdictTest {

    @Nested
    @DisplayName("status predicates")
    class StatusPredicates {

        @Test
        void approvedStatus_isApprovedTrue() {
            RefinementVerdict v =
                    new RefinementVerdict(
                            "approved",
                            "story",
                            "2026-04-30T00:00:00Z",
                            Map.of(),
                            List.of(),
                            "abc");

            assertThat(v.isApproved()).isTrue();
            assertThat(v.isRejected()).isFalse();
            assertThat(v.isTbd()).isFalse();
        }

        @Test
        void rejectedStatus_isRejectedTrue() {
            RefinementVerdict v =
                    new RefinementVerdict(
                            "rejected",
                            "story",
                            "2026-04-30T00:00:00Z",
                            Map.of(),
                            List.of("ac: missing Gherkin"),
                            "abc");

            assertThat(v.isRejected()).isTrue();
            assertThat(v.isApproved()).isFalse();
            assertThat(v.isTbd()).isFalse();
        }

        @Test
        void tbdStatus_isTbdTrue() {
            RefinementVerdict v = new RefinementVerdict("tbd", null, null, null, List.of(), null);

            assertThat(v.isTbd()).isTrue();
            assertThat(v.isApproved()).isFalse();
            assertThat(v.isRejected()).isFalse();
        }

        @Test
        void nullStatus_isTbdTrue() {
            RefinementVerdict v = new RefinementVerdict(null, null, null, null, List.of(), null);

            assertThat(v.isTbd()).isTrue();
        }
    }

    @Nested
    @DisplayName("absent sentinel")
    class AbsentSentinel {

        @Test
        void absent_returnsStatusTbd() {
            RefinementVerdict v = RefinementVerdict.absent();

            assertThat(v.isTbd()).isTrue();
            assertThat(v.status()).isEqualTo("tbd");
        }

        @Test
        void absent_hasEmptyBlockers() {
            RefinementVerdict v = RefinementVerdict.absent();

            assertThat(v.blockers()).isEmpty();
        }

        @Test
        void absent_hasNullScope() {
            RefinementVerdict v = RefinementVerdict.absent();

            assertThat(v.scope()).isNull();
        }
    }

    @Nested
    @DisplayName("DimensionResult")
    class DimensionResultTest {

        @Test
        void checkedDimension_hasNullBlocker() {
            RefinementVerdict.DimensionResult dim =
                    new RefinementVerdict.DimensionResult(true, null);

            assertThat(dim.checked()).isTrue();
            assertThat(dim.blocker()).isNull();
        }

        @Test
        void blockedDimension_hasBlockerReason() {
            RefinementVerdict.DimensionResult dim =
                    new RefinementVerdict.DimensionResult(
                            false, "hypothesis: missing measurable indicator");

            assertThat(dim.checked()).isFalse();
            assertThat(dim.blocker()).isEqualTo("hypothesis: missing measurable indicator");
        }
    }

    @Nested
    @DisplayName("scope discriminator")
    class ScopeDiscriminator {

        @Test
        void epicScope_isStoredAsEpic() {
            RefinementVerdict v =
                    new RefinementVerdict(
                            "approved",
                            "epic",
                            "2026-04-30T00:00:00Z",
                            Map.of(),
                            List.of(),
                            "hash");

            assertThat(v.scope()).isEqualTo("epic");
        }

        @Test
        void storyScope_isStoredAsStory() {
            RefinementVerdict v =
                    new RefinementVerdict(
                            "approved",
                            "story",
                            "2026-04-30T00:00:00Z",
                            Map.of(),
                            List.of(),
                            "hash");

            assertThat(v.scope()).isEqualTo("story");
        }
    }

    @Nested
    @DisplayName("immutability")
    class Immutability {

        @Test
        void record_isImmutable() {
            List<String> blockers = new java.util.ArrayList<>();
            blockers.add("initial blocker");
            RefinementVerdict v =
                    new RefinementVerdict("rejected", "story", null, null, blockers, null);

            blockers.add("added after construction");

            assertThat(v.blockers()).hasSize(2);
        }
    }
}

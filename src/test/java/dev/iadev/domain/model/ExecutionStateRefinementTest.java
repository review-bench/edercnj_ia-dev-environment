package dev.iadev.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ExecutionState — refinementVerdict field (EPIC-0069 / Rule 29)")
class ExecutionStateRefinementTest {

    @Nested
    @DisplayName("parse — refinementVerdict absent (pre-EPIC-0069 legacy)")
    class ParseAbsent {

        @Test
        void legacyStateFile_verdictFieldIsNull() {
            String json = "{\"flowVersion\": \"2\", \"epicId\": \"EPIC-0049\"}";

            ExecutionState state = ExecutionState.parse(json);

            assertThat(state.refinementVerdict()).isNull();
        }

        @Test
        void legacyStateFile_effectiveVerdictIsTbd() {
            String json = "{\"flowVersion\": \"2\", \"epicId\": \"EPIC-0049\"}";

            ExecutionState state = ExecutionState.parse(json);

            assertThat(state.effectiveRefinementVerdict().isTbd()).isTrue();
        }

        @Test
        void flowVersion1_absentVerdict_effectiveIsTbd() {
            String json = "{\"flowVersion\": \"1\", \"epicId\": \"EPIC-0001\"}";

            ExecutionState state = ExecutionState.parse(json);

            assertThat(state.refinementVerdict()).isNull();
            assertThat(state.effectiveRefinementVerdict().status())
                    .isEqualTo(RefinementVerdict.STATUS_TBD);
        }
    }

    @Nested
    @DisplayName("parse — refinementVerdict present")
    class ParsePresent {

        @Test
        void approvedVerdictWithStoryScope_parsesCorrectly() {
            String json =
                    "{"
                            + "\"flowVersion\": \"4\","
                            + "\"epicId\": \"EPIC-0069\","
                            + "\"refinementVerdict\": {"
                            + "  \"status\": \"approved\","
                            + "  \"scope\": \"story\","
                            + "  \"checkedAt\": \"2026-04-30T00:00:00Z\""
                            + "}"
                            + "}";

            ExecutionState state = ExecutionState.parse(json);

            assertThat(state.refinementVerdict()).isNotNull();
            assertThat(state.refinementVerdict().status()).isEqualTo("approved");
            assertThat(state.refinementVerdict().scope()).isEqualTo("story");
            assertThat(state.refinementVerdict().isApproved()).isTrue();
        }

        @Test
        void rejectedVerdictWithEpicScope_parsesCorrectly() {
            String json =
                    "{"
                            + "\"flowVersion\": \"4\","
                            + "\"epicId\": \"EPIC-0069\","
                            + "\"refinementVerdict\": {"
                            + "  \"status\": \"rejected\","
                            + "  \"scope\": \"epic\""
                            + "}"
                            + "}";

            ExecutionState state = ExecutionState.parse(json);

            assertThat(state.refinementVerdict()).isNotNull();
            assertThat(state.refinementVerdict().isRejected()).isTrue();
            assertThat(state.refinementVerdict().scope()).isEqualTo("epic");
        }

        @Test
        void tbdVerdictPresent_isTbdTrue() {
            String json =
                    "{"
                            + "\"flowVersion\": \"4\","
                            + "\"epicId\": \"EPIC-0069\","
                            + "\"refinementVerdict\": {"
                            + "  \"status\": \"tbd\""
                            + "}"
                            + "}";

            ExecutionState state = ExecutionState.parse(json);

            assertThat(state.refinementVerdict()).isNotNull();
            assertThat(state.refinementVerdict().isTbd()).isTrue();
        }

        @Test
        void approvedVerdict_effectiveVerdictReturnsSame() {
            String json =
                    "{"
                            + "\"flowVersion\": \"4\","
                            + "\"epicId\": \"EPIC-0069\","
                            + "\"refinementVerdict\": {"
                            + "  \"status\": \"approved\","
                            + "  \"scope\": \"epic\""
                            + "}"
                            + "}";

            ExecutionState state = ExecutionState.parse(json);

            assertThat(state.effectiveRefinementVerdict()).isSameAs(state.refinementVerdict());
            assertThat(state.effectiveRefinementVerdict().isApproved()).isTrue();
        }
    }

    @Nested
    @DisplayName("effectiveRefinementVerdict — Rule 19 fallback contract")
    class EffectiveVerdict {

        @Test
        void nullVerdict_effectiveReturnsAbsentSentinel() {
            ExecutionState state = new ExecutionState("2", "EPIC-TEST", null, null, false, null);

            RefinementVerdict effective = state.effectiveRefinementVerdict();

            assertThat(effective).isNotNull();
            assertThat(effective.isTbd()).isTrue();
            assertThat(effective.blockers()).isEmpty();
        }

        @Test
        void nonNullVerdict_effectiveReturnsSameInstance() {
            RefinementVerdict verdict = RefinementVerdict.absent();
            ExecutionState state = new ExecutionState("4", "EPIC-TEST", null, null, true, verdict);

            assertThat(state.effectiveRefinementVerdict()).isSameAs(verdict);
        }
    }

    @Nested
    @DisplayName("backward-compatibility — existing flowVersion fields unaffected")
    class BackwardCompatibility {

        @Test
        void flowVersion3WithRefinementVerdict_bothFieldsParseCorrectly() {
            String json =
                    "{"
                            + "\"flowVersion\": \"3\","
                            + "\"epicId\": \"EPIC-0061\","
                            + "\"refinementVerdict\": {\"status\": \"approved\", \"scope\": \"epic\"}"
                            + "}";

            ExecutionState state = ExecutionState.parse(json);

            assertThat(state.flowVersion()).isEqualTo("3");
            assertThat(state.localFirstLifecycle()).isTrue();
            assertThat(state.refinementVerdict().isApproved()).isTrue();
        }

        @Test
        void flowVersion4WithoutRefinementVerdict_remainsBackwardCompatible() {
            String json = "{\"flowVersion\": \"4\", \"epicId\": \"EPIC-0062\"}";

            ExecutionState state = ExecutionState.parse(json);

            assertThat(state.flowVersion()).isEqualTo("4");
            assertThat(state.localFirstLifecycle()).isTrue();
            assertThat(state.refinementVerdict()).isNull();
            assertThat(state.effectiveRefinementVerdict().isTbd()).isTrue();
        }
    }
}

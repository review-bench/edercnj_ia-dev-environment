package dev.iadev.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ExecutionState V3 — flowVersion 3 + localFirstLifecycle")
class ExecutionStateV3Test {

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        void flowVersion3_parsesWithLocalFirstTrue() {
            String json = "{\"flowVersion\": \"3\", \"epicId\": \"EPIC-0061\"}";

            ExecutionState state = ExecutionState.parse(json);

            assertThat(state.flowVersion()).isEqualTo("3");
            assertThat(state.epicId()).isEqualTo("EPIC-0061");
            assertThat(state.localFirstLifecycle()).isTrue();
        }

        @Test
        void flowVersion2_hasLocalFirstFalse() {
            String json = "{\"flowVersion\": \"2\", \"epicId\": \"EPIC-0049\"}";

            ExecutionState state = ExecutionState.parse(json);

            assertThat(state.flowVersion()).isEqualTo("2");
            assertThat(state.localFirstLifecycle()).isFalse();
        }

        @Test
        void flowVersion1_hasLocalFirstFalse() {
            String json = "{\"flowVersion\": \"1\", \"epicId\": \"EPIC-0042\"}";

            ExecutionState state = ExecutionState.parse(json);

            assertThat(state.flowVersion()).isEqualTo("1");
            assertThat(state.localFirstLifecycle()).isFalse();
        }

        @Test
        void missingFlowVersion_defaultsToLegacy() {
            String json = "{\"epicId\": \"EPIC-0001\"}";

            ExecutionState state = ExecutionState.parse(json);

            assertThat(state.flowVersion()).isEqualTo("1");
            assertThat(state.localFirstLifecycle()).isFalse();
        }

        @Test
        void invalidFlowVersionWithLocalFirstTrue_throwsIllegalArgument() {
            String json =
                    "{\"flowVersion\": \"2\", \"epicId\": \"EPIC-X\", \"localFirstLifecycle\": true}";

            assertThatThrownBy(() -> ExecutionState.parse(json))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("localFirstLifecycle=true");
        }
    }

    @Nested
    @DisplayName("isLocalFirst")
    class IsLocalFirst {

        @Test
        void flowVersion3_isLocalFirst() {
            ExecutionState state = new ExecutionState("3", "EPIC-0061", null, null, true);
            assertThat(state.localFirstLifecycle()).isTrue();
        }

        @Test
        void flowVersion4_isAlsoLocalFirst() {
            ExecutionState state = new ExecutionState("4", "EPIC-0061", null, null, true);
            assertThat(state.localFirstLifecycle()).isTrue();
        }
    }
}

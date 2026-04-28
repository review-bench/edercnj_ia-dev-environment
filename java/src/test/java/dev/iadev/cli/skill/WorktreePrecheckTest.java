package dev.iadev.cli.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WorktreePrecheck")
class WorktreePrecheckTest {

    @Nested
    @DisplayName("precheck(allowDirty=false)")
    class PrecheckStrict {

        @Test
        void cleanTree_whenCalled_returnsClean() {
            ProcessRunner runner = mock(ProcessRunner.class);
            when(runner.run(any())).thenReturn("", "");
            WorktreePrecheck precheck = new WorktreePrecheck(runner);

            PrecheckResult result = precheck.precheck(false);

            assertThat(result).isEqualTo(PrecheckResult.CLEAN);
        }

        @Test
        void dirtyTreeOnly_whenCalled_returnsDirty() {
            ProcessRunner runner = mock(ProcessRunner.class);
            when(runner.run(any()))
                    .thenReturn(" M plans/unknown/telemetry/events.ndjson", "");
            WorktreePrecheck precheck = new WorktreePrecheck(runner);

            PrecheckResult result = precheck.precheck(false);

            assertThat(result).isEqualTo(PrecheckResult.DIRTY);
        }

        @Test
        void divergentBranchOnly_whenCalled_throwsAmbiguous() {
            ProcessRunner runner = mock(ProcessRunner.class);
            when(runner.run(any())).thenReturn("", "ahead 2");
            WorktreePrecheck precheck = new WorktreePrecheck(runner);

            assertThatThrownBy(() -> precheck.precheck(false))
                    .isInstanceOf(WorktreeAmbiguousException.class)
                    .hasMessageContaining("WORKTREE_AMBIGUOUS");
        }

        @Test
        void dirtyAndDivergent_whenCalled_throwsAmbiguous() {
            ProcessRunner runner = mock(ProcessRunner.class);
            when(runner.run(any())).thenReturn("M  foo.java", "behind 3");
            WorktreePrecheck precheck = new WorktreePrecheck(runner);

            assertThatThrownBy(() -> precheck.precheck(false))
                    .isInstanceOf(WorktreeAmbiguousException.class)
                    .hasMessageContaining("WORKTREE_AMBIGUOUS");
        }

        @Test
        void gitNotFound_whenCalled_throwsOperationalError() {
            ProcessRunner runner = mock(ProcessRunner.class);
            when(runner.run(any())).thenThrow(
                    new IllegalStateException("OPERATIONAL_ERROR: git not found on PATH"));
            WorktreePrecheck precheck = new WorktreePrecheck(runner);

            assertThatThrownBy(() -> precheck.precheck(false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("OPERATIONAL_ERROR");
        }
    }

    @Nested
    @DisplayName("precheck(allowDirty=true)")
    class PrecheckPermissive {

        @Test
        void ambiguousState_whenAllowDirty_returnsAmbiguousWithoutThrowing() {
            ProcessRunner runner = mock(ProcessRunner.class);
            when(runner.run(any())).thenReturn("M  foo.java", "ahead 1");
            WorktreePrecheck precheck = new WorktreePrecheck(runner);

            PrecheckResult result = precheck.precheck(true);

            assertThat(result).isEqualTo(PrecheckResult.AMBIGUOUS);
        }

        @Test
        void divergentOnly_whenAllowDirty_returnsDivergent() {
            ProcessRunner runner = mock(ProcessRunner.class);
            when(runner.run(any())).thenReturn("", "behind 5");
            WorktreePrecheck precheck = new WorktreePrecheck(runner);

            PrecheckResult result = precheck.precheck(true);

            assertThat(result).isEqualTo(PrecheckResult.DIVERGENT);
        }
    }

    @Nested
    @DisplayName("dirtyFiles")
    class DirtyFiles {

        @Test
        void cleanTree_whenCalled_returnsEmptyList() {
            ProcessRunner runner = mock(ProcessRunner.class);
            when(runner.run(any())).thenReturn("", "");
            WorktreePrecheck precheck = new WorktreePrecheck(runner);

            List<String> files = precheck.dirtyFiles();

            assertThat(files).isEmpty();
        }

        @Test
        void dirtyTree_whenCalled_returnsDirtyFileLines() {
            ProcessRunner runner = mock(ProcessRunner.class);
            when(runner.run(any()))
                    .thenReturn(" M plans/unknown/telemetry/events.ndjson\n?? foo.txt", "");
            WorktreePrecheck precheck = new WorktreePrecheck(runner);

            List<String> files = precheck.dirtyFiles();

            assertThat(files).hasSize(2);
        }
    }

    @Nested
    @DisplayName("currentBranch")
    class CurrentBranch {

        @Test
        void whenCalled_returnsBranchName() {
            ProcessRunner runner = mock(ProcessRunner.class);
            when(runner.run(any())).thenReturn("epic/0061", "");
            WorktreePrecheck precheck = new WorktreePrecheck(runner);

            String branch = precheck.currentBranch();

            assertThat(branch).isEqualTo("epic/0061");
        }
    }
}

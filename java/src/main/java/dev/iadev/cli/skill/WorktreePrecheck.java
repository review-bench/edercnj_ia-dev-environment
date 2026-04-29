package dev.iadev.cli.skill;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Classifies the state of a git working tree before orchestrator execution.
 *
 * <p>Uses a {@link ProcessRunner} abstraction so tests can inject a mock without a real git binary.
 * In production, construct with {@link #WorktreePrecheck()} which uses the system git.
 */
public final class WorktreePrecheck {

    private static final List<String> STATUS_CMD = List.of("git", "status", "--porcelain");
    private static final List<String> UPSTREAM_CMD =
            List.of("git", "rev-list", "--count", "--left-right", "@{upstream}...HEAD");
    private static final List<String> BRANCH_CMD =
            List.of("git", "rev-parse", "--abbrev-ref", "HEAD");

    private final ProcessRunner runner;

    /** Production constructor using the system git binary via {@link SystemProcessRunner}. */
    public WorktreePrecheck() {
        this(new SystemProcessRunner());
    }

    /** Test constructor for injecting a mock {@link ProcessRunner}. */
    WorktreePrecheck(ProcessRunner runner) {
        this.runner = runner;
    }

    /**
     * Classifies the working tree state.
     *
     * @param allowDirty when {@code true}, ambiguous states are returned without throwing
     * @return the classification result
     * @throws WorktreeAmbiguousException when state is DIVERGENT or AMBIGUOUS and allowDirty=false
     */
    public PrecheckResult precheck(boolean allowDirty) {
        String statusOutput = runner.run(STATUS_CMD);
        String upstreamOutput = runner.run(UPSTREAM_CMD);

        boolean isDirty = !statusOutput.isBlank();
        boolean isDivergent = isDivergent(upstreamOutput);

        PrecheckResult result = classify(isDirty, isDivergent);

        if (!allowDirty
                && (result == PrecheckResult.DIVERGENT || result == PrecheckResult.AMBIGUOUS)) {
            throw new WorktreeAmbiguousException(
                    "WORKTREE_AMBIGUOUS: working tree has uncommitted changes and branch divergence."
                            + " Use --allow-dirty to bypass or git stash to resolve.");
        }
        return result;
    }

    /**
     * Returns the current branch name.
     *
     * @return branch name from git
     */
    public String currentBranch() {
        return runner.run(BRANCH_CMD).trim();
    }

    /**
     * Returns the list of dirty file lines from {@code git status --porcelain}.
     *
     * @return list of lines (empty when working tree is clean)
     */
    public List<String> dirtyFiles() {
        String output = runner.run(STATUS_CMD);
        if (output.isBlank()) {
            return List.of();
        }
        return Arrays.stream(output.split("\n"))
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());
    }

    private boolean isDivergent(String upstreamOutput) {
        return upstreamOutput != null && !upstreamOutput.isBlank();
    }

    private PrecheckResult classify(boolean dirty, boolean divergent) {
        if (dirty && divergent) return PrecheckResult.AMBIGUOUS;
        if (divergent) return PrecheckResult.DIVERGENT;
        if (dirty) return PrecheckResult.DIRTY;
        return PrecheckResult.CLEAN;
    }

    private static final class SystemProcessRunner implements ProcessRunner {

        @Override
        public String run(List<String> command) {
            try {
                Process process = new ProcessBuilder(command).start();
                try (BufferedReader reader =
                        new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            } catch (IOException e) {
                throw new IllegalStateException("OPERATIONAL_ERROR: git not found on PATH", e);
            }
        }
    }
}

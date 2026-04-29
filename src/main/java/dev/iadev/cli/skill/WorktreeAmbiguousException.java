package dev.iadev.cli.skill;

/**
 * Thrown when {@link WorktreePrecheck#precheck(boolean)} detects an ambiguous working tree state
 * (dirty files combined with branch divergence, or divergence alone) and {@code allowDirty=false}.
 *
 * <p>Exit code contract: {@code WORKTREE_AMBIGUOUS=15}.
 */
public final class WorktreeAmbiguousException extends RuntimeException {

    static final int EXIT_CODE = 15;

    WorktreeAmbiguousException(String message) {
        super(message);
    }

    /** The stable exit code for this exception. */
    public int exitCode() {
        return EXIT_CODE;
    }
}

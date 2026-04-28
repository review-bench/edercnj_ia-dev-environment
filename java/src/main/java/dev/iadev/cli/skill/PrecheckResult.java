package dev.iadev.cli.skill;

/** Classifies the state of a git working tree before orchestrator execution. */
public enum PrecheckResult {

    /** Working tree is clean and branch is aligned with upstream. */
    CLEAN,

    /** Has uncommitted changes but no branch divergence from upstream. */
    DIRTY,

    /** Branch has commits ahead or behind upstream, but working tree is clean. */
    DIVERGENT,

    /** Both uncommitted changes and branch divergence present. */
    AMBIGUOUS
}

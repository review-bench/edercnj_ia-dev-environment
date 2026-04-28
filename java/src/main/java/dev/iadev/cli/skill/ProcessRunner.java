package dev.iadev.cli.skill;

import java.util.List;

/** Abstraction over process execution to enable testability without a real git binary. */
public interface ProcessRunner {

    /**
     * Runs a command and returns its stdout as a single string.
     *
     * @param command the command and arguments
     * @return stdout output (trimmed)
     * @throws IllegalStateException if the command is not found or execution fails
     */
    String run(List<String> command);
}

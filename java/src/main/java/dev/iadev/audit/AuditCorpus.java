package dev.iadev.audit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Provides file-system walks over the generator's resource corpus for use by {@link Auditor}
 * implementations.
 *
 * <p>All walkers are bounded and do not follow symbolic links. Paths are relative to {@code
 * rootDir}.
 */
public final class AuditCorpus {

    private final Path rootDir;

    /**
     * Creates a corpus rooted at the given directory.
     *
     * @param rootDir the repository or test-fixture root
     */
    public AuditCorpus(Path rootDir) {
        this.rootDir = rootDir.toAbsolutePath().normalize();
    }

    /**
     * Returns the root directory of this corpus.
     *
     * @return the root path
     */
    public Path rootDir() {
        return rootDir;
    }

    /**
     * Streams every {@code SKILL.md} found under {@code targets/claude/skills/}.
     *
     * @return stream of SKILL.md paths (closed by caller via try-with-resources or {@code
     *     .toList()})
     */
    public Stream<Path> walkSkills() {
        Path skillsRoot = rootDir.resolve("java/src/main/resources/targets/claude/skills");
        if (!Files.isDirectory(skillsRoot)) {
            skillsRoot = rootDir;
        }
        return walkFiles(skillsRoot, "SKILL.md");
    }

    /**
     * Streams every {@code *.md} found under {@code targets/claude/agents/}.
     *
     * @return stream of agent markdown paths
     */
    public Stream<Path> walkAgents() {
        Path agentsRoot = rootDir.resolve("java/src/main/resources/targets/claude/agents");
        return walkFiles(agentsRoot, ".md");
    }

    /**
     * Streams every {@code execution-state.json} found under {@code ai/epics/} or {@code plans/}.
     *
     * @return stream of execution-state.json paths
     */
    public Stream<Path> walkExecutionStates() {
        return Stream.concat(
                walkFiles(rootDir.resolve("ai"), "execution-state.json"),
                walkFiles(rootDir.resolve("plans"), "execution-state.json"));
    }

    /**
     * Lists all {@code *.sh.tpl} template files under {@code
     * java/src/main/resources/targets/claude/scripts/{stack}/} for the given stack.
     *
     * @param stack e.g. {@code "java-maven"}
     * @return list of template paths
     */
    public List<Path> listAuditTemplates(String stack) {
        Path scriptsDir =
                rootDir.resolve("java/src/main/resources/targets/claude/scripts/" + stack);
        try {
            if (!Files.isDirectory(scriptsDir)) {
                return List.of();
            }
            try (Stream<Path> stream = Files.list(scriptsDir)) {
                return stream.filter(p -> p.getFileName().toString().endsWith(".sh.tpl"))
                        .sorted()
                        .toList();
            }
        } catch (IOException e) {
            return List.of();
        }
    }

    private Stream<Path> walkFiles(Path dir, String fileNameSuffix) {
        if (!Files.isDirectory(dir)) {
            return Stream.empty();
        }
        try {
            return Files.walk(dir)
                    .filter(p -> p.getFileName().toString().endsWith(fileNameSuffix))
                    .filter(Files::isRegularFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk " + dir, e);
        }
    }
}

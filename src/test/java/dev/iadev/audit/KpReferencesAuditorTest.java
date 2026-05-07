package dev.iadev.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Maven CI-blocking audit harness for audit-kp-references.sh (EPIC-0078 story-0078-0015).
 *
 * <p>Validates exit codes 0/1/2/3/4 per Rule 26 §Standardized Exit Codes + story-0078-0015
 * contract.
 */
@DisplayName("KpReferencesAuditorTest (Maven CI-blocking)")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Bash script tests require POSIX environment")
class KpReferencesAuditorTest {

    private static final Path AUDIT_SCRIPT =
            Path.of(
                    System.getProperty("user.dir"),
                    "src/main/resources/targets/claude/scripts/audit-kp-references.sh");

    private static final Path KNOWLEDGE_ROOT =
            Path.of(System.getProperty("user.dir"), "src/main/resources/targets/claude/knowledge");

    private static final Path SKILLS_ROOT =
            Path.of(System.getProperty("user.dir"), "src/main/resources/targets/claude/skills");

    private static final String REPO_DIR = System.getProperty("user.dir");

    private int run(File workdir, String... args) throws IOException, InterruptedException {
        String[] cmd = new String[args.length + 2];
        cmd[0] = "/bin/bash";
        cmd[1] = AUDIT_SCRIPT.toString();
        System.arraycopy(args, 0, cmd, 2, args.length);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workdir);
        pb.environment().put("CLAUDE_PROJECT_DIR", workdir.getAbsolutePath());
        pb.inheritIO();
        Process proc = pb.start();
        proc.waitFor(30, TimeUnit.SECONDS);
        return proc.exitValue();
    }

    private int runWithEnv(File workdir, java.util.Map<String, String> env, String... args)
            throws IOException, InterruptedException {
        String[] cmd = new String[args.length + 2];
        cmd[0] = "/bin/bash";
        cmd[1] = AUDIT_SCRIPT.toString();
        System.arraycopy(args, 0, cmd, 2, args.length);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workdir);
        pb.environment().putAll(env);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.waitFor(30, TimeUnit.SECONDS);
        return proc.exitValue();
    }

    @Test
    @DisplayName("audit-kp-references.sh exists and is executable")
    void script_isPresentAndExecutable() {
        assertThat(AUDIT_SCRIPT).as("audit-kp-references.sh must exist").exists();
        assertThat(AUDIT_SCRIPT.toFile().canExecute())
                .as("audit-kp-references.sh must be executable")
                .isTrue();
    }

    @Test
    @DisplayName("--self-check passes when knowledge and skills roots exist")
    void selfCheck_passesWithValidRoots() throws IOException, InterruptedException {
        ProcessBuilder pb =
                new ProcessBuilder("/bin/bash", AUDIT_SCRIPT.toString(), "--self-check");
        pb.directory(new File(REPO_DIR));
        pb.environment().put("CLAUDE_PROJECT_DIR", REPO_DIR);
        pb.environment().put("KNOWLEDGE_ROOT", KNOWLEDGE_ROOT.toString());
        pb.environment().put("SKILLS_ROOT", SKILLS_ROOT.toString());
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.waitFor(15, TimeUnit.SECONDS);
        assertThat(proc.exitValue()).as("self-check must exit 0").isEqualTo(0);
    }

    @Test
    @DisplayName("degenerate: empty knowledge root exits 0 (no KPs = no orphans)")
    void degenerateEmptyKnowledgeRoot_exits0(@TempDir Path tempKp, @TempDir Path tempSkills)
            throws IOException, InterruptedException {
        // tempKp is empty — no .md files
        java.util.Map<String, String> env = new java.util.HashMap<>();
        env.put("KNOWLEDGE_ROOT", tempKp.toString());
        env.put("SKILLS_ROOT", tempSkills.toString());
        int exit = runWithEnv(new File(REPO_DIR), env);
        assertThat(exit).as("empty knowledge root must exit 0").isEqualTo(0);
    }

    @Test
    @DisplayName("happy path: KP referenced by skill exits 0")
    void happyPath_kpReferenced_exits0(@TempDir Path tempRoot)
            throws IOException, InterruptedException {
        Path knowledgeDir = tempRoot.resolve("knowledge/security/anti-patterns");
        Files.createDirectories(knowledgeDir);
        Files.writeString(knowledgeDir.resolve("j1-sql.md"), "# J1 SQL Injection\n");

        Path skillsDir = tempRoot.resolve("skills/x-review-qa");
        Files.createDirectories(skillsDir);
        Files.writeString(
                skillsDir.resolve("SKILL.md"),
                "---\nname: x-review-qa\n---\n"
                        + "Read knowledge/security/anti-patterns/j1-sql.md\n");

        java.util.Map<String, String> env = new java.util.HashMap<>();
        env.put("KNOWLEDGE_ROOT", tempRoot.resolve("knowledge").toString());
        env.put("SKILLS_ROOT", tempRoot.resolve("skills").toString());
        int exit = runWithEnv(new File(REPO_DIR), env);
        assertThat(exit).as("referenced KP must exit 0").isEqualTo(0);
    }

    @Test
    @DisplayName("orphan KP exits 1 with KP_ORPHAN")
    void orphanKp_exits1(@TempDir Path tempRoot) throws IOException, InterruptedException {
        Path knowledgeDir = tempRoot.resolve("knowledge/governance");
        Files.createDirectories(knowledgeDir);
        Files.writeString(
                knowledgeDir.resolve("capability-composition.md"), "# Capability Composition\n");

        Path skillsDir = tempRoot.resolve("skills/x-some-skill");
        Files.createDirectories(skillsDir);
        Files.writeString(
                skillsDir.resolve("SKILL.md"), "---\nname: x-some-skill\n---\n# No KP refs here\n");

        ProcessBuilder pb = new ProcessBuilder("/bin/bash", AUDIT_SCRIPT.toString());
        pb.directory(new File(REPO_DIR));
        pb.environment().put("KNOWLEDGE_ROOT", tempRoot.resolve("knowledge").toString());
        pb.environment().put("SKILLS_ROOT", tempRoot.resolve("skills").toString());
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        proc.waitFor(15, TimeUnit.SECONDS);
        assertThat(proc.exitValue()).as("orphan KP must exit 1").isEqualTo(1);
        assertThat(output).as("must contain KP_ORPHAN").contains("KP_ORPHAN");
    }

    @Test
    @DisplayName("path traversal in --knowledge-root exits 2")
    void pathTraversal_exits2() throws IOException, InterruptedException {
        ProcessBuilder pb =
                new ProcessBuilder("/bin/bash", AUDIT_SCRIPT.toString(), "--knowledge-root", "..");
        pb.directory(new File(REPO_DIR));
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        proc.waitFor(15, TimeUnit.SECONDS);
        assertThat(proc.exitValue()).as("path traversal must exit 2").isEqualTo(2);
        assertThat(output).as("must contain OPERATIONAL_ERROR").contains("OPERATIONAL_ERROR");
    }

    @Test
    @DisplayName("real repo passes: no new orphan KPs beyond baseline (integration)")
    void realRepo_allKpsReferenced() throws IOException, InterruptedException {
        if (!KNOWLEDGE_ROOT.toFile().isDirectory() || !SKILLS_ROOT.toFile().isDirectory()) {
            return; // skip if roots missing
        }
        Path baselinePath = Path.of(REPO_DIR, "governance/baselines/kp-references-baseline.txt");
        ProcessBuilder pb =
                new ProcessBuilder("/bin/bash", AUDIT_SCRIPT.toString(), "--mode=kp-orphan");
        pb.directory(new File(REPO_DIR));
        pb.environment().put("CLAUDE_PROJECT_DIR", REPO_DIR);
        pb.environment().put("KNOWLEDGE_ROOT", KNOWLEDGE_ROOT.toString());
        pb.environment().put("SKILLS_ROOT", SKILLS_ROOT.toString());
        pb.environment().put("BASELINE_PATH", baselinePath.toString());
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        proc.waitFor(30, TimeUnit.SECONDS);
        int exit = proc.exitValue();
        if (exit != 0) {
            System.err.println("KP orphan audit output:\n" + output);
        }
        assertThat(exit).as("no new orphan KPs beyond baseline: " + output).isEqualTo(0);
    }
}

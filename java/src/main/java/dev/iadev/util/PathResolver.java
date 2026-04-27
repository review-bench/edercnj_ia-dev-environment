package dev.iadev.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves operational artifact paths for both v3 (legacy {@code plans/})
 * and v4 ({@code ai/epics/}) layouts via an automatic filesystem probe.
 *
 * <p>Usage: inject an instance with the repository root as {@code basePath}.
 * All returned paths are absolute and normalized.
 *
 * <p>Thread-safe: the probe is stateless per call (no caching).
 */
public final class PathResolver {

    private static final Logger LOG =
            Logger.getLogger(PathResolver.class.getName());

    private static final String EPIC_ID_PATTERN = "\\d{4}";

    private final Path basePath;

    public PathResolver(Path basePath) {
        this.basePath = basePath.toAbsolutePath().normalize();
    }

    /**
     * Resolves the top-level directory for an epic.
     *
     * <p>Returns v4 path when {@code ai/epics/epic-{id}-<slug>} exists;
     * falls back to v3 {@code plans/epic-{id}}.
     *
     * @param epicId 4-digit numeric string (e.g. {@code "0060"})
     * @return absolute, normalized path to the epic directory
     * @throws IllegalArgumentException if {@code epicId} is null or not 4 digits
     */
    public Path epicDir(String epicId) {
        validateEpicId(epicId);
        return probeV4EpicDir(epicId)
                .orElseGet(() -> basePath.resolve("plans/epic-" + epicId).normalize());
    }

    /**
     * Resolves the work-unit directory inside an epic.
     *
     * @param epicId  4-digit epic identifier
     * @param type    unit type (STORY, BUG, SPIKE, CHORE)
     * @param unitId  4-digit unit identifier within the epic
     * @return path to {@code <epicDir>/work/<type.subFolder>/<type.prefix>-<epicId>-<unitId>}
     */
    public Path unitDir(String epicId, UnitType type, String unitId) {
        if (type == null) {
            throw new IllegalArgumentException("UnitType cannot be null");
        }
        Path epic = epicDir(epicId);
        String dirName = type.prefix() + "-" + epicId + "-" + unitId;
        return epic.resolve("work/" + type.subFolder() + "/" + dirName).normalize();
    }

    /** Plans sub-directory for a work unit. */
    public Path planDir(String epicId, UnitType type, String unitId) {
        return unitDir(epicId, type, unitId).resolve("plans");
    }

    /** Reviews sub-directory for a work unit. */
    public Path reviewDir(String epicId, UnitType type, String unitId) {
        return unitDir(epicId, type, unitId).resolve("reviews");
    }

    /** Reports sub-directory for a work unit. */
    public Path reportDir(String epicId, UnitType type, String unitId) {
        return unitDir(epicId, type, unitId).resolve("reports");
    }

    /** Telemetry NDJSON file for an epic. */
    public Path epicTelemetry(String epicId) {
        return epicDir(epicId).resolve("telemetry/events.ndjson");
    }

    /** execution-state.json for an epic. */
    public Path epicState(String epicId) {
        return epicDir(epicId).resolve("execution-state.json");
    }

    /** Releases root directory ({@code ai/releases/}). */
    public Path releasesDir() {
        return basePath.resolve("ai/releases").normalize();
    }

    /** Runs root directory ({@code ai/runs/}). */
    public Path runsDir() {
        return basePath.resolve("ai/runs").normalize();
    }

    // ── private helpers ────────────────────────────────────────────────────

    private void validateEpicId(String epicId) {
        if (epicId == null || !epicId.matches(EPIC_ID_PATTERN)) {
            throw new IllegalArgumentException(
                    "Invalid epicId format: must be 4-digit string (e.g., \"0060\"), got: "
                            + epicId);
        }
    }

    private Optional<Path> probeV4EpicDir(String epicId) {
        Path v4Base = basePath.resolve("ai/epics");
        String glob = "epic-" + epicId + "-*";
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(v4Base, glob)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    Path resolved = entry.toAbsolutePath().normalize();
                    LOG.log(Level.FINE,
                            "PathResolver probe: epic={0} layout=v4 path={1}",
                            new Object[]{epicId, resolved});
                    return Optional.of(resolved);
                }
            }
        } catch (IOException e) {
            LOG.log(Level.FINE,
                    "PathResolver probe: epic={0} ai/epics not found, fallback=v3",
                    epicId);
        }
        LOG.log(Level.FINE,
                "PathResolver probe: epic={0} layout=v3", epicId);
        return Optional.empty();
    }
}

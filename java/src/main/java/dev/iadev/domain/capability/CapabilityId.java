package dev.iadev.domain.capability;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object for a capability identifier.
 *
 * <p>Canonical format: {@code category.subcategory.atomic} (e.g., {@code data.database.postgres}).
 * Glob variants supported: {@code data.database.*} or {@code data.**}.
 */
public final class CapabilityId {

    private static final Pattern CANONICAL =
            Pattern.compile("^[a-z][a-z0-9]*(\\.[a-z0-9][a-z0-9-]*)+$");
    private static final Pattern GLOB_PATTERN =
            Pattern.compile("^[a-z][a-z0-9]*(\\.(([a-z0-9][a-z0-9-]*)|\\*|\\*\\*))+$");

    private final String raw;
    private final String[] segments;
    private final boolean glob;

    private CapabilityId(String raw) {
        this.raw = raw;
        this.segments = raw.split("\\.");
        this.glob = CapabilityGlob.isGlob(raw);
    }

    public static CapabilityId of(String raw) {
        Objects.requireNonNull(raw, "capability id must not be null");
        if (CapabilityGlob.isGlob(raw)) {
            if (!GLOB_PATTERN.matcher(raw).matches()) {
                throw new CapabilityError.UnknownCapability(
                        "'"
                                + raw
                                + "' is not a valid glob — must follow category.subcategory.* format");
            }
        } else {
            if (!CANONICAL.matcher(raw).matches()) {
                throw new CapabilityError.UnknownCapability(
                        "'" + raw + "' must follow category.subcategory.atomic format");
            }
        }
        return new CapabilityId(raw);
    }

    public String category() {
        return segments[0];
    }

    public String subcategory() {
        return segments.length > 1 ? segments[1] : "";
    }

    public String atomic() {
        return segments.length > 2 ? segments[2] : "";
    }

    public boolean isGlob() {
        return glob;
    }

    public boolean matches(CapabilityId candidate) {
        if (!glob) return this.equals(candidate);
        return CapabilityGlob.matches(raw, candidate.raw);
    }

    public String value() {
        return raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CapabilityId other)) return false;
        return raw.equals(other.raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    @Override
    public String toString() {
        return raw;
    }
}

package dev.iadev.domain.products;

import java.util.Objects;
import java.util.Set;

public final class CommitPathWhitelist {

    private static final Set<String> STANDARD_PREFIXES =
            Set.of(
                    "plans/",
                    ".claude/templates/",
                    "ai/epics/",
                    "ai/products/",
                    "ai/memory/",
                    "ai/releases/");

    private final Set<String> prefixes;

    private CommitPathWhitelist(Set<String> prefixes) {
        this.prefixes = prefixes;
    }

    public static CommitPathWhitelist of(Set<String> prefixes) {
        if (prefixes == null) {
            throw new IllegalArgumentException("prefixes must not be null");
        }
        return new CommitPathWhitelist(Set.copyOf(prefixes));
    }

    public static CommitPathWhitelist standard() {
        return new CommitPathWhitelist(STANDARD_PREFIXES);
    }

    public boolean isAllowed(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        if (path.isBlank()) return false;
        return prefixes.stream().anyMatch(path::startsWith);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommitPathWhitelist other)) return false;
        return prefixes.equals(other.prefixes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefixes);
    }

    @Override
    public String toString() {
        return "CommitPathWhitelist{prefixes=" + prefixes + "}";
    }
}

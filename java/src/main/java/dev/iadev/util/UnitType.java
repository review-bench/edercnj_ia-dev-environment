package dev.iadev.util;

/** Discriminates the type of work unit within an epic directory. */
public enum UnitType {
    STORY("stories", "story"),
    BUG("bugs", "bug"),
    SPIKE("spikes", "spike"),
    CHORE("chores", "chore");

    private final String subFolder;
    private final String prefix;

    UnitType(String subFolder, String prefix) {
        this.subFolder = subFolder;
        this.prefix = prefix;
    }

    /** Sub-directory name under {@code work/} (e.g. {@code "stories"}). */
    public String subFolder() {
        return subFolder;
    }

    /** File/directory prefix for work units (e.g. {@code "story"}). */
    public String prefix() {
        return prefix;
    }
}

package dev.iadev.application.memory;

import java.util.List;

/**
 * Immutable value object representing the YAML frontmatter of an epic memory summary file.
 *
 * <p>All list fields are unmodifiable. {@code supersededBy} is {@code null} when absent.
 */
public record MemoryFrontmatter(
        String epicId,
        String slug,
        String summaryVersion,
        String created,
        String lastUpdated,
        boolean indexable,
        boolean archived,
        String supersededBy,
        List<String> tags,
        List<String> capabilitiesAffected,
        List<String> rulesAffected,
        List<String> adrsReferenced,
        List<String> patternsIntroduced,
        List<String> antipatternsRejected,
        List<String> dependenciesOf,
        List<String> dependenciesFor) {}

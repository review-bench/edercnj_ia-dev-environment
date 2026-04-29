package dev.iadev.application.composition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Scans a targets directory for markdown artifacts that have a frontmatter block.
 *
 * <p>Returns artifacts in sorted (deterministic) order (RULE-004).
 */
final class ArtifactScanner {

    private static final Logger LOG = Logger.getLogger(ArtifactScanner.class.getName());

    record ScannedArtifact(Path path, String relativePath, List<String> requiredCapabilities) {}

    List<ScannedArtifact> scan(Path root) throws IOException {
        List<ScannedArtifact> result = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(this::isMarkdownArtifact)
                    .sorted()
                    .forEach(
                            p -> {
                                try {
                                    String content = Files.readString(p);
                                    List<String> caps = extractRequiredCapabilities(content);
                                    if (caps != null) {
                                        String rel = root.relativize(p).toString();
                                        result.add(new ScannedArtifact(p, rel, caps));
                                    }
                                } catch (IOException ignored) {
                                }
                            });
        }
        return result;
    }

    private boolean isMarkdownArtifact(Path p) {
        if (!Files.isRegularFile(p)) return false;
        String name = p.getFileName().toString();
        return name.endsWith(".md") && !name.startsWith("_");
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRequiredCapabilities(String content) {
        String stripped = content.stripLeading();
        if (!stripped.startsWith("---")) return null;
        int firstNl = stripped.indexOf('\n');
        if (firstNl < 0) return null;
        int endFm = stripped.indexOf("\n---", firstNl);
        if (endFm < 0) return null;
        String fm = stripped.substring(firstNl + 1, endFm);
        if (!fm.contains("requires-capabilities")) {
            LOG.warning(
                    () ->
                            "ArtifactScanner: frontmatter v2 artifact missing requires-capabilities"
                                    + " (Rule 28 violation — treating as universal): "
                                    + content.substring(0, Math.min(80, content.length())));
            return List.of();
        }

        List<String> caps = new ArrayList<>();
        boolean inList = false;
        for (String line : fm.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("requires-capabilities:")) {
                String inline = trimmed.substring("requires-capabilities:".length()).trim();
                if (inline.startsWith("[") && inline.endsWith("]")) {
                    String inner = inline.substring(1, inline.length() - 1).trim();
                    if (!inner.isEmpty()) {
                        for (String c : inner.split(",")) {
                            caps.add(c.trim());
                        }
                    }
                    return caps;
                }
                inList = true;
            } else if (inList) {
                if (trimmed.startsWith("-")) {
                    caps.add(trimmed.substring(1).trim());
                } else {
                    break;
                }
            }
        }
        return caps;
    }
}

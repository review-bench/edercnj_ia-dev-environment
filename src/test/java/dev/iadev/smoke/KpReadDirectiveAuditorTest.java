package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Audit test that validates every skill listed in audits/kp-consumers.json contains
 * the corresponding "Read <kp-path>" directive in its SKILL.md.
 *
 * <p>Story: story-0078-0010 (Consumer Skills — Read KP Directives)
 */
@DisplayName("KpReadDirectiveAuditorTest — story-0078-0010")
class KpReadDirectiveAuditorTest {

    private static final Path CONSUMERS_MAP = Path.of("audits", "kp-consumers.json");
    private static final Path SKILLS_ROOT =
            Path.of("src", "main", "resources", "targets", "claude", "skills");

    @Test
    @DisplayName("consumersMapExists")
    void consumersMapExists() {
        assertThat(CONSUMERS_MAP).as("audits/kp-consumers.json must exist").exists();
    }

    @TestFactory
    @DisplayName("eachConsumerHasReadDirective")
    Stream<DynamicTest> eachConsumerHasReadDirective() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<String>> consumers = mapper.readValue(
                CONSUMERS_MAP.toFile(),
                new TypeReference<Map<String, List<String>>>() {});

        return consumers.entrySet().stream().flatMap(entry -> {
            String kpPath = entry.getKey();
            List<String> skillNames = entry.getValue();
            String expectedDirective = "Read src/main/resources/targets/claude/" + kpPath;

            return skillNames.stream().map(skillName -> DynamicTest.dynamicTest(
                    skillName + " → " + kpPath,
                    () -> {
                        Path skillFile = findSkillFile(skillName);
                        assertThat(skillFile)
                                .as("SKILL.md for %s must exist", skillName)
                                .exists();
                        String content = Files.readString(skillFile, StandardCharsets.UTF_8);
                        assertThat(content)
                                .as("SKILL.md for %s must contain Read directive for %s",
                                        skillName, kpPath)
                                .contains(expectedDirective);
                    }));
        });
    }

    @Test
    @DisplayName("consumersMapHasExpectedKpCount")
    void consumersMapHasExpectedKpCount() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<String>> consumers = mapper.readValue(
                CONSUMERS_MAP.toFile(),
                new TypeReference<Map<String, List<String>>>() {});
        assertThat(consumers)
                .as("kp-consumers.json must declare exactly 10 KP paths")
                .hasSize(10);
    }

    private Path findSkillFile(String skillName) throws IOException {
        try (Stream<Path> stream = Files.walk(SKILLS_ROOT)) {
            return stream
                    .filter(p -> p.getFileName().toString().equals("SKILL.md"))
                    .filter(p -> p.getParent().getFileName().toString().equals(skillName))
                    .findFirst()
                    .orElseThrow(() ->
                            new AssertionError("SKILL.md not found for skill: " + skillName));
        }
    }
}

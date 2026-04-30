package dev.iadev.governance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

class ReviewFrontmatterSchemaTest {

    private static final Path SCHEMA_PATH =
            Path.of("governance/schemas/review-frontmatter-1.0.json");

    private static final List<String> REQUIRED_FIELDS =
            List.of(
                    "schema-version",
                    "generated-by",
                    "story-id",
                    "epic-id",
                    "date",
                    "decision",
                    "score",
                    "score-max",
                    "severity-counts",
                    "blocking-findings");

    @Test
    void schemaFile_exists_atExpectedPath() {
        assertThat(SCHEMA_PATH).exists();
    }

    @Test
    void schemaFile_isValidJson_parsesWithoutError() throws IOException {
        String content = Files.readString(SCHEMA_PATH);
        JsonNode root = new ObjectMapper().readTree(content);
        assertThat(root).isNotNull();
        assertThat(root.isObject()).isTrue();
    }

    @Test
    void schemaFile_hasMetaSchemaFields_schemaIdType() throws IOException {
        JsonNode root = parseSchema();
        assertThat(root.has("$schema")).isTrue();
        assertThat(root.has("$id")).isTrue();
        assertThat(root.get("type").asText()).isEqualTo("object");
    }

    @Test
    void schemaFile_requiredArray_containsAllTenFields() throws IOException {
        JsonNode root = parseSchema();
        JsonNode requiredNode = root.get("required");
        assertThat(requiredNode).isNotNull();
        assertThat(requiredNode.isArray()).isTrue();

        List<String> actualRequired =
                StreamSupport.stream(
                                Spliterators.spliteratorUnknownSize(requiredNode.elements(), 0),
                                false)
                        .map(JsonNode::asText)
                        .toList();

        assertThat(actualRequired).containsExactlyInAnyOrderElementsOf(REQUIRED_FIELDS);
    }

    @Test
    void schemaFile_generatedByProperty_hasCorrectPattern() throws IOException {
        JsonNode root = parseSchema();
        JsonNode pattern = root.path("properties").path("generated-by").path("pattern");
        assertThat(pattern.isMissingNode()).isFalse();
        assertThat(pattern.asText()).isEqualTo("^(x-review|x-review-pr)@[0-9a-f]{40}$");
    }

    private JsonNode parseSchema() throws IOException {
        return new ObjectMapper().readTree(Files.readString(SCHEMA_PATH));
    }
}

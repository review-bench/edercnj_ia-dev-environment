package dev.iadev.adapter.outbound.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.capability.Capability;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("CapabilityStubWriter")
class CapabilityStubWriterTest {

    @TempDir
    Path tempDir;

    private static Capability testCapability() {
        return new Capability("capability-c1", "product-0001", List.of());
    }

    @Test
    @DisplayName("write_newFile_createsFile")
    void write_newFile_createsFile() throws IOException {
        var result = CapabilityStubWriter.write(testCapability(), tempDir);
        assertThat(result.path()).exists();
        assertThat(result.skipped()).isFalse();
    }

    @Test
    @DisplayName("write_newFile_containsCapabilityId")
    void write_newFile_containsCapabilityId() throws IOException {
        CapabilityStubWriter.write(testCapability(), tempDir);
        Path file = tempDir.resolve("product-0001-capability-c1-stub.json");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content).contains("\"capabilityId\": \"capability-c1\"");
        assertThat(content).contains("\"productId\": \"product-0001\"");
    }

    @Test
    @DisplayName("write_newFile_containsIdempotencyHash")
    void write_newFile_containsIdempotencyHash() throws IOException {
        CapabilityStubWriter.write(testCapability(), tempDir);
        Path file = tempDir.resolve("product-0001-capability-c1-stub.json");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content).contains("\"idempotencyHash\":");
        assertThat(content).doesNotContain("\"idempotencyHash\": \"\"");
    }

    @Test
    @DisplayName("write_sameInputTwice_secondCallSkipped")
    void write_sameInputTwice_secondCallSkipped() throws IOException {
        CapabilityStubWriter.write(testCapability(), tempDir);
        var result2 = CapabilityStubWriter.write(testCapability(), tempDir);
        assertThat(result2.skipped()).isTrue();
    }

    @Test
    @DisplayName("write_nullCapability_throwsIllegalArgument")
    void write_nullCapability_throwsIllegalArgument() {
        assertThatThrownBy(() -> CapabilityStubWriter.write(null, tempDir))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("write_nullOutputDir_throwsIllegalArgument")
    void write_nullOutputDir_throwsIllegalArgument() {
        assertThatThrownBy(() -> CapabilityStubWriter.write(testCapability(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

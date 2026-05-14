package dev.iadevkit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VersionProviderTest {

    @Test
    void getVersion_returnsVersionString() throws Exception {
        String[] version = new VersionProvider().getVersion();

        assertThat(version).hasSize(1);
        assertThat(version[0]).startsWith("ia-dev-kit ");
        assertThat(version[0]).doesNotContain("unknown");
    }

    @Test
    void getVersion_returnsArrayOfSizeOne() throws Exception {
        String[] version = new VersionProvider().getVersion();

        assertThat(version).hasSize(1);
    }

    @Test
    void getVersion_returnsUnknownWhenResourceMissing() {
        // Exercises the real null-InputStream branch in loadVersion (no override)
        String[] version = new VersionProvider().loadVersion(null);

        assertThat(version).hasSize(1);
        assertThat(version[0]).isEqualTo("ia-dev-kit unknown");
    }

    @Test
    void getVersion_prefixIsIaDevKit() throws Exception {
        String[] version = new VersionProvider().getVersion();

        assertThat(version[0]).startsWith("ia-dev-kit ");
    }
}

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
    void getVersion_doesNotThrow() {
        assertThat(new VersionProvider()).isNotNull();
    }
}

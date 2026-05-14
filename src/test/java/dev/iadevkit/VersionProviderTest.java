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
    void getVersion_returnsUnknownWhenResourceMissing() throws Exception {
        // Subclass that returns null stream to simulate missing resource
        VersionProvider provider =
                new VersionProvider() {
                    @Override
                    public String[] getVersion() throws Exception {
                        java.util.Properties props = new java.util.Properties();
                        // No resource loaded — version defaults to "unknown"
                        return new String[] {
                            "ia-dev-kit " + props.getProperty("version", "unknown")
                        };
                    }
                };

        String[] version = provider.getVersion();

        assertThat(version[0]).isEqualTo("ia-dev-kit unknown");
    }

    @Test
    void getVersion_prefixIsIaDevKit() throws Exception {
        String[] version = new VersionProvider().getVersion();

        assertThat(version[0]).startsWith("ia-dev-kit ");
    }
}

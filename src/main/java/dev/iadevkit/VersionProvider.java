package dev.iadevkit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/dev/iadevkit/version.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignored) {
        }
        return new String[] {"ia-dev-kit " + props.getProperty("version", "unknown")};
    }
}

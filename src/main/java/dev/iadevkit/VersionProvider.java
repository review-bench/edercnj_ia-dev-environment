package dev.iadevkit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {

    static final String PROPERTIES_RESOURCE = "/dev/iadevkit/version.properties";

    @Override
    public String[] getVersion() throws Exception {
        return loadVersion(getClass().getResourceAsStream(PROPERTIES_RESOURCE));
    }

    String[] loadVersion(InputStream source) {
        Properties props = new Properties();
        if (source != null) {
            try (InputStream in = source) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Warning: could not read version.properties: " + e.getMessage());
            }
        }
        return new String[] {"ia-dev-kit " + props.getProperty("version", "unknown")};
    }
}

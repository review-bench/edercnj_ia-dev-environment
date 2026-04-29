package dev.iadev.domain.port.output;

import dev.iadev.domain.capability.CapabilityDefinition;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface CapabilityCatalogRepository {

    List<CapabilityDefinition> loadAll(Path catalogRoot);

    Optional<CapabilityDefinition> load(Path capabilityFile);
}

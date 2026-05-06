package dev.iadev.adapter.inbound.cli;

import java.nio.file.Path;
import java.util.Optional;

public record CreateProductRequest(
        Path ideationFile, Path outputDir, Optional<String> productId, boolean dryRun) {}

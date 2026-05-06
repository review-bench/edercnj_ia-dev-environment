package dev.iadev.domain.product;

import java.util.List;

public final class AutoDecomposeHeuristic {

    private static final List<String> DEFAULT_CAPABILITIES =
            List.of("ingest", "process", "store", "query", "serve");

    public List<String> decompose(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product must not be null");
        }
        return DEFAULT_CAPABILITIES;
    }
}

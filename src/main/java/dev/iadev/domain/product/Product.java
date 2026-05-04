package dev.iadev.domain.product;

import java.util.List;

public final class Product {

    private final String name;
    private final List<RNFRoot> rnfRoots;

    public Product(String name, List<RNFRoot> rnfRoots) {
        this.name = name;
        this.rnfRoots = List.copyOf(rnfRoots);
    }

    public String name() {
        return name;
    }

    public List<RNFRoot> rnfRoots() {
        return rnfRoots;
    }
}

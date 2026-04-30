package dev.iadev.domain.capability;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ParameterSpec(
        String name,
        ParameterType type,
        List<String> values,
        Optional<Object> defaultValue,
        String description) {

    public enum ParameterType {
        STRING,
        INT,
        BOOLEAN,
        ENUM
    }

    public ParameterSpec {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        values = values == null ? List.of() : List.copyOf(values);
        defaultValue = defaultValue == null ? Optional.empty() : defaultValue;
        description = description == null ? "" : description;
        if (type == ParameterType.ENUM && values.isEmpty()) {
            throw new IllegalArgumentException("ENUM parameter '" + name + "' must declare values");
        }
    }

    public static ParameterSpec of(String name, ParameterType type) {
        return new ParameterSpec(name, type, List.of(), Optional.empty(), "");
    }
}

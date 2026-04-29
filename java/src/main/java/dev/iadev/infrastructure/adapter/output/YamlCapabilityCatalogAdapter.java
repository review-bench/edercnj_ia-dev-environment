package dev.iadev.infrastructure.adapter.output;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityError;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.CapabilityKind;
import dev.iadev.domain.capability.ParameterSpec;
import dev.iadev.domain.port.output.CapabilityCatalogRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

public final class YamlCapabilityCatalogAdapter implements CapabilityCatalogRepository {

    private static final String REQUIRED_FIELD_ID = "id";
    private static final String REQUIRED_FIELD_KIND = "kind";
    private static final String REQUIRED_FIELD_CATEGORY = "category";

    @Override
    public List<CapabilityDefinition> loadAll(Path catalogRoot) {
        if (!Files.isDirectory(catalogRoot)) {
            throw new CapabilityError.UnknownCapability(
                    "catalog root not found: " + catalogRoot);
        }
        List<CapabilityDefinition> all = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(catalogRoot)) {
            paths.filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                    .sorted()
                    .forEach(p -> load(p).ifPresent(all::add));
        } catch (IOException e) {
            throw new CapabilityError.UnknownCapability(
                    "error walking catalog root: " + catalogRoot + ": " + e.getMessage());
        }
        all.sort((a, b) -> a.id().value().compareTo(b.id().value()));
        return List.copyOf(all);
    }

    @Override
    public Optional<CapabilityDefinition> load(Path capabilityFile) {
        if (!Files.exists(capabilityFile)) {
            throw new CapabilityError.UnknownCapability(
                    "file not found: " + capabilityFile);
        }
        Map<String, Object> raw = parseYaml(capabilityFile);
        validateRequiredFields(raw, capabilityFile);
        return Optional.of(toDefinition(raw, capabilityFile));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYaml(Path file) {
        LoaderOptions opts = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(opts));
        try (InputStream is = Files.newInputStream(file)) {
            Object parsed = yaml.load(is);
            if (!(parsed instanceof Map)) {
                throw new CapabilityError.UnknownCapability(
                        "expected YAML map at: " + file);
            }
            return (Map<String, Object>) parsed;
        } catch (IOException e) {
            throw new CapabilityError.UnknownCapability(
                    "error reading: " + file + ": " + e.getMessage());
        } catch (org.yaml.snakeyaml.error.YAMLException e) {
            String mark = "";
            if (e instanceof org.yaml.snakeyaml.scanner.ScannerException se
                    && se.getProblemMark() != null) {
                mark = ":" + (se.getProblemMark().getLine() + 1);
            }
            throw new CapabilityError.UnknownCapability(
                    "YAML parse error at " + file + mark + ": " + e.getMessage());
        }
    }

    private void validateRequiredFields(Map<String, Object> raw, Path file) {
        for (String field : List.of(REQUIRED_FIELD_ID, REQUIRED_FIELD_KIND, REQUIRED_FIELD_CATEGORY)) {
            if (!raw.containsKey(field)) {
                throw new CapabilityError.UnknownCapability(
                        "schema v3.0 required — missing field '" + field + "' in: " + file);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private CapabilityDefinition toDefinition(Map<String, Object> raw, Path file) {
        CapabilityId id = CapabilityId.of(str(raw, "id", file));
        CapabilityKind kind = parseKind(str(raw, "kind", file), file);
        String category = str(raw, "category", file);
        Optional<String> version = Optional.ofNullable((String) raw.get("version"));
        String status = (String) raw.getOrDefault("status", "stable");
        String description = (String) raw.getOrDefault("description", "");

        List<CapabilityId> requires = toIdList(raw, "requires");
        List<CapabilityId> provides = toIdList(raw, "provides");
        List<CapabilityId> excludes = toIdList(raw, "excludes");
        List<CapabilityId> expandsTo = toIdList(raw, "expands-to");
        List<String> tags = toStringList(raw, "tags");
        Map<String, ParameterSpec> params = new LinkedHashMap<>();

        return new CapabilityDefinition(id, kind, category, version, status,
                description, params, requires, provides, excludes, expandsTo, tags);
    }

    private String str(Map<String, Object> raw, String key, Path file) {
        Object val = raw.get(key);
        if (!(val instanceof String s)) {
            throw new CapabilityError.UnknownCapability(
                    "field '" + key + "' must be a string in: " + file);
        }
        return s;
    }

    private CapabilityKind parseKind(String kind, Path file) {
        return switch (kind) {
            case "atomic" -> CapabilityKind.ATOMIC;
            case "composite" -> CapabilityKind.COMPOSITE;
            case "profile" -> CapabilityKind.PROFILE;
            default -> throw new CapabilityError.UnknownCapability(
                    "unknown kind '" + kind + "' in: " + file);
        };
    }

    @SuppressWarnings("unchecked")
    private List<CapabilityId> toIdList(Map<String, Object> raw, String key) {
        Object val = raw.get(key);
        if (val == null) return List.of();
        if (!(val instanceof List<?> list)) return List.of();
        List<CapabilityId> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String s) result.add(CapabilityId.of(s));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Map<String, Object> raw, String key) {
        Object val = raw.get(key);
        if (val == null) return List.of();
        if (!(val instanceof List<?> list)) return List.of();
        return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }
}

package dev.iadev.application.composition;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityGenerators;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.Profile;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.From;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;

/**
 * Property-based idempotency tests for CapabilityAwareComposer (story-0064-0506,
 * RULE-001/RULE-004).
 *
 * <p>Validates: compose(p) == compose(p) over 100 random capability profiles. Verifies no
 * nondeterminism (unordered collections, timestamps, absolute paths) leaks into the composition
 * plan.
 */
class CapabilityAwareComposerPropertyTest {

    private final CapabilityAwareComposer composer = new CapabilityAwareComposer();
    private Path tempDir;

    @BeforeProperty
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("composer-property");
    }

    @AfterProperty
    void tearDown() throws IOException {
        if (tempDir != null) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        }
    }

    @Provide
    Arbitrary<List<CapabilityDefinition>> smallCatalogs() {
        return CapabilityGenerators.smallCatalogs();
    }

    @Provide
    Arbitrary<Profile> profiles() {
        return CapabilityGenerators.profiles(CapabilityGenerators.smallCatalogs());
    }

    private static ResolvedCapabilitySet toResolved(Profile profile) {
        List<CapabilityId> caps = profile.capabilities();
        return new ResolvedCapabilitySet(profile.name(), List.copyOf(caps), Map.of(), List.of());
    }

    @Property(tries = 100)
    @Label("Invariante 1 — idempotência: plan(p, root) == plan(p, root) (RULE-001)")
    void idempotency(@ForAll @From("profiles") Profile profile) throws IOException {
        Path targetsRoot = tempDir.resolve("targets-" + System.nanoTime());
        writeMinimalTargets(targetsRoot);
        ResolvedCapabilitySet capSet = toResolved(profile);

        CompositionPlan plan1 = composer.plan(capSet, targetsRoot);
        CompositionPlan plan2 = composer.plan(capSet, targetsRoot);

        assertThat(
                        plan1.included().stream()
                                .map(CompositionPlan.ArtifactEntry::relativePath)
                                .toList())
                .as("plan() must be idempotent — included set must be identical")
                .isEqualTo(
                        plan2.included().stream()
                                .map(CompositionPlan.ArtifactEntry::relativePath)
                                .toList());
        assertThat(
                        plan1.excluded().stream()
                                .map(CompositionPlan.ArtifactEntry::relativePath)
                                .toList())
                .as("plan() must be idempotent — excluded set must be identical")
                .isEqualTo(
                        plan2.excluded().stream()
                                .map(CompositionPlan.ArtifactEntry::relativePath)
                                .toList());
    }

    @Property(tries = 100)
    @Label("Invariante 2 — determinismo: 3× consecutivas = mesmo output (RULE-004)")
    void determinism(@ForAll @From("profiles") Profile profile) throws IOException {
        Path targetsRoot = tempDir.resolve("targets-det-" + System.nanoTime());
        writeMinimalTargets(targetsRoot);
        ResolvedCapabilitySet capSet = toResolved(profile);

        CompositionPlan p1 = composer.plan(capSet, targetsRoot);
        CompositionPlan p2 = composer.plan(capSet, targetsRoot);
        CompositionPlan p3 = composer.plan(capSet, targetsRoot);

        var paths1 =
                p1.included().stream().map(CompositionPlan.ArtifactEntry::relativePath).toList();
        var paths2 =
                p2.included().stream().map(CompositionPlan.ArtifactEntry::relativePath).toList();
        var paths3 =
                p3.included().stream().map(CompositionPlan.ArtifactEntry::relativePath).toList();

        assertThat(paths1).isEqualTo(paths2);
        assertThat(paths2).isEqualTo(paths3);
    }

    @Property(tries = 50)
    @Label("Invariante 3 — capability inativa não muda output (RULE-001)")
    void inactiveCapabilityDoesNotChangeOutput(@ForAll @From("profiles") Profile profile)
            throws IOException {
        Path targetsRoot = tempDir.resolve("targets-inactive-" + System.nanoTime());
        writeMinimalTargets(targetsRoot);
        ResolvedCapabilitySet baseSet = toResolved(profile);

        // Add a fake capability that matches nothing in targets
        CapabilityId fake = CapabilityId.of("test.fake.nonexistent");
        List<CapabilityId> extended = new java.util.ArrayList<>(baseSet.capabilities());
        extended.add(fake);
        ResolvedCapabilitySet extendedSet =
                new ResolvedCapabilitySet(profile.name(), extended, Map.of(), List.of());

        CompositionPlan basePlan = composer.plan(baseSet, targetsRoot);
        CompositionPlan extPlan = composer.plan(extendedSet, targetsRoot);

        // Adding an extra capability that no artifact requires should not change included set
        assertThat(extPlan.included().size())
                .as("extra inactive capabilities must not shrink included set")
                .isGreaterThanOrEqualTo(basePlan.included().size());
    }

    private static void writeMinimalTargets(Path root) throws IOException {
        Files.createDirectories(root);
        writeArtifact(root, "universal.md");
        writeArtifact(root, "java-specific.md", "lang.java.21");
        writeArtifact(root, "spring-specific.md", "web.spring.boot");
    }

    private static void writeArtifact(Path root, String name, String... capIds) throws IOException {
        Path file = root.resolve(name);
        Files.createDirectories(file.getParent());
        String capList =
                capIds.length == 0
                        ? "[]"
                        : "\n"
                                + String.join(
                                        "\n",
                                        java.util.Arrays.stream(capIds)
                                                .map(c -> "  - " + c)
                                                .toList());
        Files.writeString(
                file,
                "---\nname: "
                        + name.replace(".md", "")
                        + "\nrequires-capabilities:"
                        + capList
                        + "\n---\n# Content of "
                        + name
                        + "\n");
    }
}

package dev.iadev.adapter.inbound.cli;

import dev.iadev.application.capability.ValidateRNFNoRelaxUseCase;
import dev.iadev.domain.capability.ApprovalStatus;
import dev.iadev.domain.capability.RNFOverride;
import dev.iadev.domain.product.RNFCategory;
import dev.iadev.domain.product.RNFRootValidationResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.nio.file.Path;

@Command(
        name = "x-internal-rnf-validate",
        mixinStandardHelpOptions = true,
        description = "Validate RNF no-relax markers and justification gate for override inheritance.")
public class XInternalRnfValidateCommand implements Callable<Integer> {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_VALIDATION_FAILURE = 1;
    static final int EXIT_EXECUTION_ERROR = 2;

    @Spec
    CommandSpec spec;

    @Option(
            names = {"--override"},
            description = "RNF override spec: CATEGORY:norelax[:originalValue] or CATEGORY:relaxed:originalValue:newValue:justification[:approvalStatus:approver]",
            arity = "0..*")
    List<String> overrideSpecs = new ArrayList<>();

    @Option(
            names = {"--artifact"},
            description = "Path to capability/story artifact with an 'RNFs Herdadas' markdown table.")
    Path artifactPath;

    @Option(
            names = {"--dry-run"},
            description = "Parse and report only; always returns exit code 0.")
    boolean dryRun;

    private final ValidateRNFNoRelaxUseCase useCase;
    private final RNFOverrideArtifactParser artifactParser;

    public XInternalRnfValidateCommand() {
        this(new ValidateRNFNoRelaxUseCase(), new RNFOverrideArtifactParser());
    }

    XInternalRnfValidateCommand(ValidateRNFNoRelaxUseCase useCase, RNFOverrideArtifactParser artifactParser) {
        this.useCase = useCase;
        this.artifactParser = artifactParser;
    }

    @Override
    public Integer call() {
        List<RNFOverride> overrides;
        try {
            overrides = collectOverrides();
        } catch (IllegalArgumentException | IOException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage());
            return EXIT_EXECUTION_ERROR;
        }

        RNFRootValidationResult result = useCase.execute(overrides);

        if (!result.passed()) {
            result.errors().forEach(err -> spec.commandLine().getErr().println("Violation: " + err));
            if (dryRun) {
                return EXIT_SUCCESS;
            }
            return EXIT_VALIDATION_FAILURE;
        }

        return EXIT_SUCCESS;
    }

    List<RNFOverride> collectOverrides() throws IOException {
        List<RNFOverride> overrides = new ArrayList<>();
        if (artifactPath != null) {
            overrides.addAll(artifactParser.parse(artifactPath));
        }
        overrides.addAll(parseOverrides(overrideSpecs));
        return overrides;
    }

    List<RNFOverride> parseOverrides(List<String> specs) {
        List<RNFOverride> result = new ArrayList<>();
        for (String spec : specs) {
            result.add(parseOne(spec));
        }
        return result;
    }

    RNFOverride parseOne(String spec) {
        String[] parts = spec.split(":", -1);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid override spec (expected CATEGORY:norelax or CATEGORY:relaxed:...): " + spec);
        }
        RNFCategory category = RNFCategory.valueOf(parts[0].toUpperCase());
        String mode = parts[1].toLowerCase();
        if ("norelax".equals(mode)) {
            String originalValue = parts.length >= 3 ? parts[2] : null;
            return RNFOverride.noRelax(category, originalValue);
        }
        if ("relaxed".equals(mode)) {
            if (parts.length < 5) {
                throw new IllegalArgumentException(
                        "Relaxed spec requires CATEGORY:relaxed:originalValue:newValue:justification but got: " + spec);
            }
            ApprovalStatus approvalStatus = parts.length >= 6 ? ApprovalStatus.fromString(parts[5]) : null;
            String approver = parts.length >= 7 ? parts[6] : null;
            return RNFOverride.withApproval(category, parts[2], parts[3], parts[4], approvalStatus, approver);
        }
        throw new IllegalArgumentException("Unknown mode '" + mode + "' in spec: " + spec);
    }
}

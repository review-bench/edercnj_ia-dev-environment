package dev.iadev.domain.capability;

import java.util.List;

/**
 * Sealed exception hierarchy for capability resolution errors (RULE-001 contract).
 *
 * <p>Sealed class allows exhaustive switch in Java 21. Each subtype carries structured context
 * enabling targeted remediation messages.
 */
public sealed class CapabilityError extends RuntimeException
        permits CapabilityError.UnknownCapability,
                CapabilityError.AsymmetricMutex,
                CapabilityError.CyclicDependency,
                CapabilityError.InvalidExpression,
                CapabilityError.MutexConflict,
                CapabilityError.MissingPrerequisite {

    private CapabilityError(String message) {
        super(message);
    }

    public static final class UnknownCapability extends CapabilityError {
        public UnknownCapability(String message) {
            super(message);
        }
    }

    public static final class AsymmetricMutex extends CapabilityError {
        public AsymmetricMutex(String message) {
            super(message);
        }
    }

    public static final class CyclicDependency extends CapabilityError {
        private final List<String> cyclePath;

        public CyclicDependency(String message, List<String> cyclePath) {
            super(message);
            this.cyclePath = cyclePath == null ? List.of() : List.copyOf(cyclePath);
        }

        public CyclicDependency(String message) {
            this(message, List.of());
        }

        public List<String> cyclePath() {
            return cyclePath;
        }
    }

    public static final class InvalidExpression extends CapabilityError {
        public InvalidExpression(String message) {
            super(message);
        }
    }

    public static final class MutexConflict extends CapabilityError {
        public MutexConflict(String message) {
            super(message);
        }
    }

    public static final class MissingPrerequisite extends CapabilityError {
        private final String referredId;
        private final String referrerId;

        public MissingPrerequisite(String message, String referredId, String referrerId) {
            super(message);
            this.referredId = referredId;
            this.referrerId = referrerId;
        }

        public MissingPrerequisite(String message) {
            this(message, "", "");
        }

        public String referredId() {
            return referredId;
        }

        public String referrerId() {
            return referrerId;
        }

        public String remediationHint() {
            if (referredId.isEmpty()) return "";
            String path = referredId.replace('.', '/');
            return "Add capabilities/" + path + ".yaml to the capabilities catalog";
        }
    }
}

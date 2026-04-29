package dev.iadev.domain.capability;

public sealed class CapabilityError extends RuntimeException
        permits CapabilityError.UnknownCapability,
                CapabilityError.AsymmetricMutex,
                CapabilityError.CyclicDependency,
                CapabilityError.InvalidExpression {

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
        public CyclicDependency(String message) {
            super(message);
        }
    }

    public static final class InvalidExpression extends CapabilityError {
        public InvalidExpression(String message) {
            super(message);
        }
    }
}

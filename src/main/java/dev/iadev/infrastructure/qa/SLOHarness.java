package dev.iadev.infrastructure.qa;

public final class SLOHarness {

    public record SLOSpec(String id, double target, String windowDescription) {
        public SLOSpec {
            if (id == null || id.isBlank())
                throw new IllegalArgumentException("id must not be blank");
            if (windowDescription == null || windowDescription.isBlank())
                throw new IllegalArgumentException("windowDescription must not be blank");
        }
    }

    public record SLOResult(
            boolean passed, double observedValue, double targetValue, double delta) {}

    public SLOResult validate(SLOSpec spec, double observedValue) {
        if (spec == null) throw new IllegalArgumentException("spec must not be null");
        double delta = observedValue - spec.target();
        boolean passed = observedValue >= spec.target();
        return new SLOResult(passed, observedValue, spec.target(), delta);
    }
}

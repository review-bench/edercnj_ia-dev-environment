package dev.iadev.infrastructure.qa;

import dev.iadev.infrastructure.qa.SLOHarness.SLOResult;
import dev.iadev.infrastructure.qa.SLOHarness.SLOSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SLOHarnessTest {

    private final SLOHarness harness = new SLOHarness();

    @Test
    void validate_aboveTarget_passes() {
        SLOSpec spec = new SLOSpec("uptime-sla", 99.95, "last 7 days");
        SLOResult result = harness.validate(spec, 99.99);
        assertTrue(result.passed());
        assertEquals(99.99, result.observedValue(), 0.001);
        assertEquals(99.95, result.targetValue(), 0.001);
        assertTrue(result.delta() > 0);
    }

    @Test
    void validate_exactlyTarget_passes() {
        SLOSpec spec = new SLOSpec("uptime-sla", 99.95, "last 30 days");
        SLOResult result = harness.validate(spec, 99.95);
        assertTrue(result.passed());
        assertEquals(0.0, result.delta(), 0.001);
    }

    @Test
    void validate_belowTarget_fails() {
        SLOSpec spec = new SLOSpec("uptime-sla", 99.95, "last 7 days");
        SLOResult result = harness.validate(spec, 99.90);
        assertFalse(result.passed());
        assertTrue(result.delta() < 0);
    }

    @Test
    void validate_zeroObserved_fails() {
        SLOSpec spec = new SLOSpec("uptime-sla", 99.95, "last 7 days");
        SLOResult result = harness.validate(spec, 0.0);
        assertFalse(result.passed());
    }

    @Test
    void validate_nullSpec_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> harness.validate(null, 99.99));
    }

    @Test
    void sloSpec_blankId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new SLOSpec("", 99.95, "last 7 days"));
    }

    @Test
    void sloSpec_blankWindow_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new SLOSpec("uptime-sla", 99.95, ""));
    }

    @Test
    void validate_multipleSpecs_eachIndependent() {
        SLOSpec uptime = new SLOSpec("uptime-sla", 99.95, "last 7 days");
        SLOSpec errorRate = new SLOSpec("error-rate-floor", 99.90, "last 24 hours");
        assertTrue(harness.validate(uptime, 99.99).passed());
        assertFalse(harness.validate(errorRate, 99.80).passed());
    }
}

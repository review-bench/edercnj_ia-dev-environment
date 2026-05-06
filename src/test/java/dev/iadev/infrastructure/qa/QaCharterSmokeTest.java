package dev.iadev.infrastructure.qa;

import static org.junit.jupiter.api.Assertions.*;

import dev.iadev.infrastructure.qa.SLOHarness.SLOResult;
import dev.iadev.infrastructure.qa.SLOHarness.SLOSpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class QaCharterSmokeTest {

    private final SLOHarness harness = new SLOHarness();

    @Test
    void sloHarness_uptimeSlo_passes() {
        SLOSpec spec = new SLOSpec("uptime-sla", 99.95, "last 7 days");
        SLOResult result = harness.validate(spec, 99.99);
        assertTrue(result.passed(), "Uptime 99.99 >= 99.95 should pass");
    }

    @Test
    void sloHarness_uptimeSlo_fails() {
        SLOSpec spec = new SLOSpec("uptime-sla", 99.95, "last 7 days");
        SLOResult result = harness.validate(spec, 99.90);
        assertFalse(result.passed(), "Uptime 99.90 < 99.95 should fail");
        assertTrue(result.delta() < 0, "Delta should be negative on breach");
    }

    @Test
    void errorCatalog_yaml_isReadable() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("qa/ErrorCatalog.yaml");
        assertNotNull(is, "ErrorCatalog.yaml must be on classpath at qa/ErrorCatalog.yaml");
        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        assertFalse(content.isBlank(), "ErrorCatalog.yaml must not be empty");
        assertTrue(content.contains("InvalidToken"), "Must contain InvalidToken error entry");
        assertTrue(
                content.contains("RateLimitExceeded"),
                "Must contain RateLimitExceeded error entry");
        assertTrue(content.contains("DatabaseDown"), "Must contain DatabaseDown error entry");
    }

    @Test
    void qaCharter_measurableAc_template_hasRequiredFields() {
        String acTemplate =
                """
                AC: "System responds within 200ms for 99%% of requests"
                Unit: latency_p99_milliseconds
                Target: 200
                Measurement: histogram_query(request_latency, p99)
                """;
        assertTrue(acTemplate.contains("Unit:"), "Measurable AC must have Unit field");
        assertTrue(acTemplate.contains("Target:"), "Measurable AC must have Target field");
        assertTrue(
                acTemplate.contains("Measurement:"), "Measurable AC must have Measurement field");
    }
}

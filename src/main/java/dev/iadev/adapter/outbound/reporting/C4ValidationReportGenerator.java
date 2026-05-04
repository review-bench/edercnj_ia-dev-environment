package dev.iadev.adapter.outbound.reporting;

import dev.iadev.domain.architecture.C4IntegrityValidator.Violation;
import dev.iadev.domain.quality.PhaseGateC4Validator.PhaseGateResult;

public final class C4ValidationReportGenerator {

    public String generate(PhaseGateResult result) {
        if (result == null) throw new IllegalArgumentException("result must not be null");
        StringBuilder sb = new StringBuilder("# C4 Phase Gate Report\n\n");
        if (result.passed()) {
            sb.append("**Status:** PASSED\n\nAll C4 diagrams are valid.\n");
            return sb.toString();
        }
        sb.append("**Status:** FAILED\n\n## Violations\n\n");
        for (Violation v : result.violations()) {
            sb.append("- [").append(v.severity()).append("] ")
              .append(v.type()).append(": ").append(v.message()).append("\n");
        }
        return sb.toString();
    }
}

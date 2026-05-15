package dev.iadevkit.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class AuditViolationTest {

    @Test
    void constructsWithAllComponentsAndExposesAccessors() {
        AuditViolation violation =
                new AuditViolation("EPIC-0069", "REFINEMENT_CONTRACT", "missing persona signal");

        assertThat(violation.targetId()).isEqualTo("EPIC-0069");
        assertThat(violation.ruleCode()).isEqualTo("REFINEMENT_CONTRACT");
        assertThat(violation.message()).isEqualTo("missing persona signal");
    }

    @Test
    void rejectsNullTargetId() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AuditViolation(null, "RULE", "message"))
                .withMessage("targetId must not be null");
    }

    @Test
    void rejectsNullRuleCode() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AuditViolation("TARGET", null, "message"))
                .withMessage("ruleCode must not be null");
    }

    @Test
    void rejectsNullMessage() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AuditViolation("TARGET", "RULE", null))
                .withMessage("message must not be null");
    }
}

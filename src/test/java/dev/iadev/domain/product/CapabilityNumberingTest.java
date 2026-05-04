package dev.iadev.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CapabilityNumbering")
class CapabilityNumberingTest {

    @Test
    void assignIds_threeNames_returnsCToN() {
        Map<String, String> result =
                CapabilityNumbering.assignIds(List.of("ingest", "query", "storage"));
        assertThat(result).containsKeys("ingest", "query", "storage");
        assertThat(result.get("ingest")).isEqualTo("capability-c1");
        assertThat(result.get("query")).isEqualTo("capability-c2");
        assertThat(result.get("storage")).isEqualTo("capability-c3");
    }

    @Test
    void assignIds_sevenNames_assignsC1ThroughC7() {
        List<String> names = List.of("a", "b", "c", "d", "e", "f", "g");
        Map<String, String> result = CapabilityNumbering.assignIds(names);
        assertThat(result.get("g")).isEqualTo("capability-c7");
    }

    @Test
    void assignIds_nullList_throwsIllegalArgument() {
        assertThatThrownBy(() -> CapabilityNumbering.assignIds(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assignIds_emptyList_throwsIllegalArgument() {
        assertThatThrownBy(() -> CapabilityNumbering.assignIds(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assignIds_preservesInsertionOrder() {
        List<String> names = List.of("z-cap", "a-cap", "m-cap");
        Map<String, String> result = CapabilityNumbering.assignIds(names);
        assertThat(result.get("z-cap")).isEqualTo("capability-c1");
        assertThat(result.get("a-cap")).isEqualTo("capability-c2");
        assertThat(result.get("m-cap")).isEqualTo("capability-c3");
    }
}

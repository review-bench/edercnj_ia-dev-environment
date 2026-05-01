package dev.iadev.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@DisplayName("DocumentationConfig")
class DocumentationConfigTest {

    @Nested
    @DisplayName("fromMap — happy path")
    class HappyPath {

        @Test
        void fromMap_fullBlock_parsesTargetsAndFreshnessWindow() {
            var docMap =
                    Map.<String, Object>of(
                            "targets",
                            List.of("readme", "openapi", "adr"),
                            "freshness-window-hours",
                            24);

            var cfg = DocumentationConfig.fromMap(docMap);

            assertThat(cfg.targets()).containsExactly("readme", "openapi", "adr");
            assertThat(cfg.freshnessWindowHours()).isEqualTo(24);
        }

        @Test
        void fromMap_targetsOnly_defaultsFreshnessWindowToZero() {
            var docMap = Map.<String, Object>of("targets", List.of("readme", "adr"));

            var cfg = DocumentationConfig.fromMap(docMap);

            assertThat(cfg.targets()).containsExactly("readme", "adr");
            assertThat(cfg.freshnessWindowHours()).isZero();
        }

        @Test
        void fromMap_emptyMap_returnsEmptyTargetsAndZeroFreshness() {
            var cfg = DocumentationConfig.fromMap(Map.of());

            assertThat(cfg.targets()).isEmpty();
            assertThat(cfg.freshnessWindowHours()).isZero();
            assertThat(cfg.autoDetect()).isTrue();
        }

        @Test
        void autoDetect_emptyTargets_returnsTrue() {
            var cfg = new DocumentationConfig(List.of(), 0);
            assertThat(cfg.autoDetect()).isTrue();
        }

        @Test
        void autoDetect_nonEmptyTargets_returnsFalse() {
            var cfg = new DocumentationConfig(List.of("readme"), 0);
            assertThat(cfg.autoDetect()).isFalse();
        }
    }

    @Nested
    @DisplayName("fromMap — defaults and optional fields")
    class DefaultsAndOptionals {

        @Test
        void fromMap_missingTargets_returnsEmptyList() {
            var docMap = Map.<String, Object>of("freshness-window-hours", 12);
            var cfg = DocumentationConfig.fromMap(docMap);
            assertThat(cfg.targets()).isEmpty();
        }

        @Test
        void fromMap_emptyTargetsList_returnsEmptyList() {
            var docMap = Map.<String, Object>of("targets", List.of());
            var cfg = DocumentationConfig.fromMap(docMap);
            assertThat(cfg.targets()).isEmpty();
        }
    }

    @Nested
    @DisplayName("immutability")
    class Immutability {

        @Test
        void targets_returnsImmutableList() {
            var cfg = DocumentationConfig.fromMap(Map.of("targets", List.of("readme")));
            assertThatThrownBy(() -> cfg.targets().add("openapi"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("performance — parse 50+ targets in < 500ms")
    class Performance {

        @Test
        @Timeout(1)
        void fromMap_largeTargetsList_completesWithinTimeout() {
            var manyTargets =
                    java.util.stream.IntStream.range(0, 55).mapToObj(i -> "target-" + i).toList();
            var docMap = Map.<String, Object>of("targets", manyTargets);

            var start = System.currentTimeMillis();
            var cfg = DocumentationConfig.fromMap(docMap);
            var elapsed = System.currentTimeMillis() - start;

            assertThat(cfg.targets()).hasSize(55);
            assertThat(elapsed).isLessThan(500L);
        }
    }

    @Nested
    @DisplayName("DEFAULT constant")
    class DefaultConstant {

        @Test
        void default_hasEmptyTargetsAndZeroFreshness() {
            assertThat(DocumentationConfig.DEFAULT.targets()).isEmpty();
            assertThat(DocumentationConfig.DEFAULT.freshnessWindowHours()).isZero();
            assertThat(DocumentationConfig.DEFAULT.autoDetect()).isTrue();
        }
    }
}

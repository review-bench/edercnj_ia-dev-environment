package dev.iadev.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("StackResolver")
class StackResolverTest {

    private final StackResolver resolver = new StackResolver();

    @Nested
    @DisplayName("resolveTemplateDir")
    class ResolveTemplateDir {

        @ParameterizedTest(name = "language={0} framework={1} buildTool={2} -> stack={3}")
        @CsvSource({
            "java, spring-boot, maven, spring-boot",
            "java, quarkus,     maven, java-maven",
            "java, picocli,     maven, java-maven",
            "java, picocli,     gradle, java-gradle",
            "go,   gin,         go-mod, go",
            "python, fastapi,   pip,   python",
            "typescript, express, npm,  node",
            "javascript, express, npm,  node",
            "rust, actix,       cargo, _default"
        })
        void knownCombination_returnsExpectedStack(
                String language, String framework, String buildTool, String expected) {
            String result = resolver.resolveTemplateDir(language, framework, buildTool);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        void unknownStack_returnsFallbackDefault() {
            String result = resolver.resolveTemplateDir("ruby", "rails", "bundler");
            assertThat(result).isEqualTo("_default");
        }

        @Test
        void springBootGradle_resolvesToSpringBoot() {
            String result = resolver.resolveTemplateDir("java", "spring-boot", "gradle");
            assertThat(result).isEqualTo("spring-boot");
        }

        @Test
        void nullLanguage_throwsIllegalArgument() {
            assertThatThrownBy(() -> resolver.resolveTemplateDir(null, "spring-boot", "maven"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("language");
        }
    }

    @Nested
    @DisplayName("supportedStacks")
    class SupportedStacks {

        @Test
        void returnsSixRuntimeStacks() {
            assertThat(resolver.supportedStacks()).hasSize(6);
        }

        @Test
        void containsAllExpectedStacks() {
            assertThat(resolver.supportedStacks())
                    .containsExactlyInAnyOrder(
                            "java-maven", "java-gradle", "spring-boot", "node", "python", "go");
        }

        @Test
        void doesNotContainDefaultStack() {
            assertThat(resolver.supportedStacks()).doesNotContain("_default");
        }
    }

    @Nested
    @DisplayName("isStackSafe")
    class IsStackSafe {

        @Test
        void validStackName_returnsTrue() {
            assertThat(StackResolver.isStackSafe("java-maven")).isTrue();
        }

        @Test
        void pathTraversal_returnsFalse() {
            assertThat(StackResolver.isStackSafe("../secret")).isFalse();
        }

        @Test
        void uppercaseLetters_returnsFalse() {
            assertThat(StackResolver.isStackSafe("Java-Maven")).isFalse();
        }

        @Test
        void underscorePrefix_returnsFalse() {
            assertThat(StackResolver.isStackSafe("_default")).isFalse();
        }
    }
}

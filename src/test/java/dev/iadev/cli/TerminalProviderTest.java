package dev.iadev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TerminalProvider")
class TerminalProviderTest {

    @Test
    @DisplayName("interface_isImplementedByMock")
    void interface_whenCalled_isImplementedByMock() {
        TerminalProvider provider = new MockTerminalProvider();

        assertThat(provider).isInstanceOf(TerminalProvider.class);
    }

    @Test
    @DisplayName("interface_hasSixMethods")
    void interface_whenCalled_hasSixMethods() {
        var methods = TerminalProvider.class.getDeclaredMethods();

        assertThat(methods).hasSize(6);
    }
}

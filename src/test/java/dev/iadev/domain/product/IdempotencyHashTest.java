package dev.iadev.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IdempotencyHash")
class IdempotencyHashTest {

    @Test
    @DisplayName("compute_sameInputs_returnsSameHash")
    void compute_sameInputs_returnsSameHash() {
        String hash1 = IdempotencyHash.compute("product-0001", "some content");
        String hash2 = IdempotencyHash.compute("product-0001", "some content");
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("compute_differentProductId_returnsDifferentHash")
    void compute_differentProductId_returnsDifferentHash() {
        String hash1 = IdempotencyHash.compute("product-0001", "same content");
        String hash2 = IdempotencyHash.compute("product-0002", "same content");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("compute_differentContent_returnsDifferentHash")
    void compute_differentContent_returnsDifferentHash() {
        String hash1 = IdempotencyHash.compute("product-0001", "content-A");
        String hash2 = IdempotencyHash.compute("product-0001", "content-B");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("compute_returnsHexString64Chars")
    void compute_returnsHexString64Chars() {
        String hash = IdempotencyHash.compute("product-0001", "abc");
        assertThat(hash).hasSize(64).matches("[a-f0-9]+");
    }

    @Test
    @DisplayName("compute_nullProductId_throwsIllegalArgument")
    void compute_nullProductId_throwsIllegalArgument() {
        assertThatThrownBy(() -> IdempotencyHash.compute(null, "content"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("compute_blankProductId_throwsIllegalArgument")
    void compute_blankProductId_throwsIllegalArgument() {
        assertThatThrownBy(() -> IdempotencyHash.compute("  ", "content"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("compute_nullContent_throwsIllegalArgument")
    void compute_nullContent_throwsIllegalArgument() {
        assertThatThrownBy(() -> IdempotencyHash.compute("product-0001", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package dev.iadev.domain.products;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProductNumbering")
class ProductNumberingTest {

    @Nested
    @DisplayName("of()")
    class OfTests {

        @Test
        void of_validSequence_returnsNumbering() {
            var pn = ProductNumbering.of(1);
            assertThat(pn.sequence()).isEqualTo(1);
        }

        @Test
        void of_null_throwsIllegalArgument() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ProductNumbering.of(null));
        }

        @Test
        void of_zero_throwsIllegalArgument() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ProductNumbering.of(0));
        }

        @Test
        void of_negativeSequence_throwsIllegalArgument() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ProductNumbering.of(-1));
        }

        @Test
        void of_maxSequence_returnsNumbering() {
            var pn = ProductNumbering.of(9999);
            assertThat(pn.sequence()).isEqualTo(9999);
        }

        @Test
        void of_overMaxSequence_throwsIllegalArgument() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ProductNumbering.of(10000));
        }
    }

    @Nested
    @DisplayName("formatted()")
    class FormattedTests {

        @Test
        void formatted_sequence1_returnsProductId() {
            assertThat(ProductNumbering.of(1).formatted()).isEqualTo("product-0001");
        }

        @Test
        void formatted_sequence10_returnsProductId() {
            assertThat(ProductNumbering.of(10).formatted()).isEqualTo("product-0010");
        }

        @Test
        void formatted_sequence100_returnsProductId() {
            assertThat(ProductNumbering.of(100).formatted()).isEqualTo("product-0100");
        }

        @Test
        void formatted_sequence9999_returnsProductId() {
            assertThat(ProductNumbering.of(9999).formatted()).isEqualTo("product-9999");
        }
    }

    @Nested
    @DisplayName("next()")
    class NextTests {

        @Test
        void next_sequence1_returns2() {
            var next = ProductNumbering.of(1).next();
            assertThat(next.sequence()).isEqualTo(2);
        }

        @Test
        void next_sequence9998_returns9999() {
            var next = ProductNumbering.of(9998).next();
            assertThat(next.sequence()).isEqualTo(9999);
        }

        @Test
        void next_sequence9999_throwsIllegalState() {
            assertThatIllegalStateException()
                    .isThrownBy(() -> ProductNumbering.of(9999).next())
                    .withMessageContaining("9999");
        }
    }

    @Nested
    @DisplayName("equality")
    class EqualityTests {

        @Test
        void equality_sameSequence_equal() {
            assertThat(ProductNumbering.of(1)).isEqualTo(ProductNumbering.of(1));
        }

        @Test
        void equality_differentSequence_notEqual() {
            assertThat(ProductNumbering.of(1)).isNotEqualTo(ProductNumbering.of(2));
        }

        @Test
        void hashCode_sameSequence_sameHash() {
            assertThat(ProductNumbering.of(42).hashCode())
                    .isEqualTo(ProductNumbering.of(42).hashCode());
        }

        @Test
        void toString_containsFormattedId() {
            assertThat(ProductNumbering.of(5).toString()).contains("product-0005");
        }
    }
}

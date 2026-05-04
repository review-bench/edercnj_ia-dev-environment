package dev.iadev.domain.products;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Product domain — ProductId, ProductStatus, Product")
class ProductTest {

    // ── ProductId ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ProductId")
    class ProductIdTests {

        @Test
        @DisplayName("of() creates ProductId with trimmed value")
        void of_validValue_createsId() {
            var id = ProductId.of("  my-product  ");
            assertThat(id.value()).isEqualTo("my-product");
        }

        @Test
        @DisplayName("of() rejects null value")
        void of_null_throwsIllegalArgument() {
            assertThatIllegalArgumentException().isThrownBy(() -> ProductId.of(null));
        }

        @Test
        @DisplayName("of() rejects blank value")
        void of_blank_throwsIllegalArgument() {
            assertThatIllegalArgumentException().isThrownBy(() -> ProductId.of("   "));
        }

        @Test
        @DisplayName("equals() is value-based")
        void equals_sameValue_isEqual() {
            assertThat(ProductId.of("alpha")).isEqualTo(ProductId.of("alpha"));
        }

        @Test
        @DisplayName("equals() distinguishes different values")
        void equals_differentValue_notEqual() {
            assertThat(ProductId.of("alpha")).isNotEqualTo(ProductId.of("beta"));
        }

        @Test
        @DisplayName("hashCode() is consistent with equals()")
        void hashCode_equalIds_sameHashCode() {
            assertThat(ProductId.of("gamma").hashCode())
                    .isEqualTo(ProductId.of("gamma").hashCode());
        }

        @Test
        @DisplayName("toString() returns raw value")
        void toString_returnsValue() {
            assertThat(ProductId.of("my-product").toString()).isEqualTo("my-product");
        }
    }

    // ── ProductStatus ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("ProductStatus")
    class ProductStatusTests {

        @Test
        @DisplayName("parse() returns ACTIVE for 'active' (case-insensitive)")
        void parse_active_returnsActive() {
            assertThat(ProductStatus.parse("active")).isEqualTo(ProductStatus.ACTIVE);
            assertThat(ProductStatus.parse("ACTIVE")).isEqualTo(ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("parse() returns DEPRECATED for 'deprecated'")
        void parse_deprecated_returnsDeprecated() {
            assertThat(ProductStatus.parse("deprecated")).isEqualTo(ProductStatus.DEPRECATED);
        }

        @Test
        @DisplayName("parse() defaults to DRAFT for null input")
        void parse_null_returnsDraft() {
            assertThat(ProductStatus.parse(null)).isEqualTo(ProductStatus.DRAFT);
        }

        @Test
        @DisplayName("parse() defaults to DRAFT for blank input")
        void parse_blank_returnsDraft() {
            assertThat(ProductStatus.parse("  ")).isEqualTo(ProductStatus.DRAFT);
        }

        @Test
        @DisplayName("parse() defaults to DRAFT for unknown value")
        void parse_unknown_returnsDraft() {
            assertThat(ProductStatus.parse("unknown-value")).isEqualTo(ProductStatus.DRAFT);
        }
    }

    // ── Product ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Product")
    class ProductRecordTests {

        @Test
        @DisplayName("isActive() is true only for ACTIVE status")
        void isActive_activeStatus_returnsTrue() {
            var product = new Product(
                    ProductId.of("p1"), "P One", ProductStatus.ACTIVE, Set.of());
            assertThat(product.isActive()).isTrue();
        }

        @Test
        @DisplayName("isActive() is false for DRAFT status")
        void isActive_draftStatus_returnsFalse() {
            var product = new Product(
                    ProductId.of("p2"), "P Two", ProductStatus.DRAFT, Set.of());
            assertThat(product.isActive()).isFalse();
        }

        @Test
        @DisplayName("isActive() is false for DEPRECATED status")
        void isActive_deprecatedStatus_returnsFalse() {
            var product = new Product(
                    ProductId.of("p3"), "P Three", ProductStatus.DEPRECATED, Set.of());
            assertThat(product.isActive()).isFalse();
        }

        @Test
        @DisplayName("capabilityIds is immutable even when constructed with null")
        void capabilityIds_null_treatedAsEmptyImmutableSet() {
            var product = new Product(
                    ProductId.of("p4"), "P Four", ProductStatus.ACTIVE, null);
            assertThat(product.capabilityIds()).isEmpty();
            assertThatExceptionOfType(UnsupportedOperationException.class)
                    .isThrownBy(() -> product.capabilityIds().add("x"));
        }

        @Test
        @DisplayName("capabilityIds is a defensive copy of the input set")
        void capabilityIds_defensiveCopy() {
            var mutable = new java.util.HashSet<>(Set.of("cap.a", "cap.b"));
            var product = new Product(
                    ProductId.of("p5"), "P Five", ProductStatus.ACTIVE, mutable);
            mutable.clear();
            assertThat(product.capabilityIds()).containsExactlyInAnyOrder("cap.a", "cap.b");
        }

        @Test
        @DisplayName("constructor rejects null id")
        void constructor_nullId_throwsNullPointer() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new Product(null, "Name", ProductStatus.DRAFT, Set.of()));
        }

        @Test
        @DisplayName("constructor rejects null name")
        void constructor_nullName_throwsNullPointer() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new Product(ProductId.of("x"), null, ProductStatus.DRAFT, Set.of()));
        }

        @Test
        @DisplayName("constructor rejects blank name")
        void constructor_blankName_throwsIllegalArgument() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Product(ProductId.of("x"), "  ", ProductStatus.DRAFT, Set.of()));
        }

        @Test
        @DisplayName("constructor rejects null status")
        void constructor_nullStatus_throwsNullPointer() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new Product(ProductId.of("x"), "Name", null, Set.of()));
        }
    }
}

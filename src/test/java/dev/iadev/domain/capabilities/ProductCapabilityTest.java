package dev.iadev.domain.capabilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Product-First capabilities domain — ProductCapabilityId, ProductCapability")
class ProductCapabilityTest {

    // ── ProductCapabilityId ────────────────────────────────────────────────

    @Nested
    @DisplayName("ProductCapabilityId")
    class ProductCapabilityIdTests {

        @Test
        @DisplayName("of() creates id with trimmed value")
        void of_validValue_createsId() {
            var id = ProductCapabilityId.of("  product-first.c4-model-mandatory  ");
            assertThat(id.value()).isEqualTo("product-first.c4-model-mandatory");
        }

        @Test
        @DisplayName("of() rejects null")
        void of_null_throwsIllegalArgument() {
            assertThatIllegalArgumentException().isThrownBy(() -> ProductCapabilityId.of(null));
        }

        @Test
        @DisplayName("of() rejects blank")
        void of_blank_throwsIllegalArgument() {
            assertThatIllegalArgumentException().isThrownBy(() -> ProductCapabilityId.of("  "));
        }

        @Test
        @DisplayName("equals() is value-based")
        void equals_sameValue_isEqual() {
            var a = ProductCapabilityId.of("product-first.rnf-validation");
            var b = ProductCapabilityId.of("product-first.rnf-validation");
            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("equals() distinguishes different values")
        void equals_differentValue_notEqual() {
            assertThat(ProductCapabilityId.of("product-first.c4-model-mandatory"))
                    .isNotEqualTo(ProductCapabilityId.of("product-first.pentest-always-on"));
        }

        @Test
        @DisplayName("hashCode() consistent with equals()")
        void hashCode_equalIds_sameHash() {
            assertThat(ProductCapabilityId.of("product-first.story-planning-v5").hashCode())
                    .isEqualTo(ProductCapabilityId.of("product-first.story-planning-v5").hashCode());
        }

        @Test
        @DisplayName("toString() returns raw value")
        void toString_returnsValue() {
            assertThat(ProductCapabilityId.of("product-first.rnf-validation").toString())
                    .isEqualTo("product-first.rnf-validation");
        }
    }

    // ── ProductCapability ─────────────────────────────────────────────────

    @Nested
    @DisplayName("ProductCapability")
    class ProductCapabilityRecordTests {

        private ProductCapabilityId id() {
            return ProductCapabilityId.of("product-first.c4-model-mandatory");
        }

        @Test
        @DisplayName("record holds all fields correctly")
        void record_allFields_accessible() {
            var cap = new ProductCapability(id(), "C4 Model Mandatory", "Enforces C4 diagrams", false);
            assertThat(cap.id().value()).isEqualTo("product-first.c4-model-mandatory");
            assertThat(cap.name()).isEqualTo("C4 Model Mandatory");
            assertThat(cap.description()).isEqualTo("Enforces C4 diagrams");
            assertThat(cap.universal()).isFalse();
        }

        @Test
        @DisplayName("universal=true is preserved")
        void record_universalTrue_preserved() {
            var cap = new ProductCapability(id(), "Universal Cap", "Always active", true);
            assertThat(cap.universal()).isTrue();
        }

        @Test
        @DisplayName("constructor rejects null id")
        void constructor_nullId_throwsNullPointer() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ProductCapability(null, "Name", "Desc", false));
        }

        @Test
        @DisplayName("constructor rejects null name")
        void constructor_nullName_throwsNullPointer() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ProductCapability(id(), null, "Desc", false));
        }

        @Test
        @DisplayName("constructor rejects blank name")
        void constructor_blankName_throwsIllegalArgument() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ProductCapability(id(), "  ", "Desc", false));
        }

        @Test
        @DisplayName("constructor rejects null description")
        void constructor_nullDescription_throwsNullPointer() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ProductCapability(id(), "Name", null, false));
        }
    }
}

# Testing Conventions

Concrete naming, structure, fixture, and assertion conventions used across this project. The philosophy lives in `testing-philosophy.md`; this document is the style guide.

## File and Class Naming

| Element | Convention | Example |
|---------|------------|---------|
| Unit test class | `{ProductionClass}Test` | `OrderServiceTest` |
| Integration test class | `{ProductionClass}IT` | `OrderRepositoryIT` |
| End-to-end test class | `{Feature}E2ETest` | `OrderPlacementE2ETest` |
| Test source location | `src/test/java/` mirrors `src/main/java/` package | `com.example.order.OrderServiceTest` |
| Resource fixtures | `src/test/resources/{topic}/` | `src/test/resources/fixtures/orders/valid.json` |

The Maven Surefire (unit) and Failsafe (integration) plugins separate runs by suffix: `*Test` runs in `test` phase, `*IT` runs in `verify` phase.

## Method Naming

Pattern: `should{ExpectedBehavior}_when{Condition}` or `{methodUnderTest}_should{ExpectedBehavior}_when{Condition}`.

```java
@Test
void shouldRejectOrder_whenInventoryInsufficient() { ... }

@Test
void placeOrder_shouldReserveInventory_whenAllItemsAvailable() { ... }

@Test
void placeOrder_shouldThrowDuplicateException_whenIdempotencyKeyReused() { ... }
```

Avoid:
- `test1`, `test2`, `testOrderService` — uninformative
- `testPlaceOrder` — describes the method, not the behavior
- Long German-style sentences that exceed 80 chars

The method name is the test's documentation. Future developers read it first to decide which test to run.

## AAA — Arrange, Act, Assert

Every test has three blocks separated by blank lines:

```java
@Test
void shouldComputeDiscount_whenLoyaltyTierIsGold() {
    // Arrange
    Customer customer = aCustomer().withLoyaltyTier(GOLD).build();
    Order order = anOrder().withSubtotal(BigDecimal.valueOf(100)).build();
    DiscountCalculator calculator = new DiscountCalculator();

    // Act
    BigDecimal discount = calculator.compute(customer, order);

    // Assert
    assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(15));
}
```

Comments are optional once the team is fluent; the blank lines are not. One assertion focus per test — multiple `assertThat` on the same outcome are fine; multiple unrelated outcomes mean two tests.

## Assertion Library

| Project default | Library |
|-----------------|---------|
| Java | AssertJ (`org.assertj.core.api.Assertions.assertThat`) |
| Kotlin | Kotest assertions or AssertJ |
| JavaScript / TypeScript | Vitest's `expect` or Jest's `expect` |
| Python | `pytest` assert + `pytest-assertions` |
| Go | testify `assert` / `require` |

AssertJ pattern:
```java
assertThat(order.items()).hasSize(3).extracting(Item::sku)
    .containsExactly("SKU-A", "SKU-B", "SKU-C");

assertThat(order.totalCents()).isEqualTo(15_000L);

assertThatThrownBy(() -> service.cancel(order.id()))
    .isInstanceOf(OrderAlreadyShippedException.class)
    .hasMessageContaining("already shipped");
```

Avoid JUnit's `assertEquals` — AssertJ has better failure messages and fluent chains.

## Fixtures and Builders

### Test Data Builders

```java
public class OrderBuilder {
    private OrderId id = OrderId.random();
    private Money subtotal = Money.usd(100);
    private List<OrderItem> items = new ArrayList<>();

    public static OrderBuilder anOrder() { return new OrderBuilder(); }
    public OrderBuilder withSubtotal(Money m) { this.subtotal = m; return this; }
    public OrderBuilder withItem(OrderItem i) { this.items.add(i); return this; }
    public Order build() { return new Order(id, subtotal, items); }
}
```

Builders make tests declarative and resilient to constructor changes. Place them in `src/test/java/.../testing/{Domain}Builder.java`.

### JSON Fixtures

For request/response payloads, store JSON files under `src/test/resources/fixtures/` and load with a helper:

```java
String json = Fixtures.read("orders/valid-order-request.json");
```

Avoid huge inline JSON strings inside test methods.

## Parametrized Tests

Use when the same scenario varies only by inputs:

```java
@ParameterizedTest
@CsvSource({
    "100, 0.0, 100",
    "100, 0.1, 90",
    "100, 0.5, 50",
    "100, 1.0, 0"
})
void shouldApplyDiscount(double subtotal, double rate, double expected) {
    BigDecimal result = calculator.apply(
        BigDecimal.valueOf(subtotal),
        BigDecimal.valueOf(rate));
    assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(expected));
}
```

When inputs come from a structured source, prefer `@MethodSource` returning typed records over flat CSV.

Do **not** parametrize tests that describe different behaviors — those are separate tests with different names.

## Setup, Teardown, Shared State

| Annotation | When |
|------------|------|
| `@BeforeEach` | Create fresh state for each test |
| `@AfterEach` | Clean resources opened in `@BeforeEach` |
| `@BeforeAll` (`static`) | Expensive setup shared across the whole class — Testcontainer start, schema migration |
| `@AfterAll` | Symmetric teardown |

Static fields holding shared state must be reset at boundaries; otherwise tests are order-dependent (violates FIRST.Independent).

## Async Testing

Forbidden:
```java
asyncService.start();
Thread.sleep(5000);          // Rule 10 ANTI-009 — fragile
assertThat(asyncService.done()).isTrue();
```

Required:
```java
asyncService.start();
await().atMost(Duration.ofSeconds(5))
    .pollInterval(Duration.ofMillis(100))
    .untilAsserted(() -> assertThat(asyncService.done()).isTrue());
```

Library: `org.awaitility:awaitility` (Java). Use `vi.waitFor` / `waitForCondition` in JS, `tryFor` in Kotlin coroutines.

## Mocking with Mockito (Java)

```java
@Mock InventoryPort inventoryPort;
@InjectMocks OrderService service;

@Test
void shouldReserveInventory_whenOrderPlaced() {
    Order order = anOrder().withItem(anItem("SKU-1", 2)).build();
    when(inventoryPort.reserve(any())).thenReturn(ReservationResult.success());

    service.placeOrder(order);

    verify(inventoryPort).reserve(argThat(items ->
        items.size() == 1 && items.get(0).quantity() == 2));
}
```

- `any()` only when the value is irrelevant; otherwise capture with `ArgumentCaptor` and assert structure.
- Avoid `verifyNoMoreInteractions` — too brittle as code evolves.
- Avoid mocking value objects, Optional, BigDecimal, or any final class without explicit need.

## Test Resource Files

| Path | Purpose |
|------|---------|
| `src/test/resources/application-test.yml` | Test-specific config (in-memory broker, test ports) |
| `src/test/resources/fixtures/` | JSON / XML payloads |
| `src/test/resources/schemas/` | Frozen schema snapshots for contract tests |
| `src/test/resources/migrations/` | Test-only DB migrations (rare) |

## Forbidden in Test Code

- Real production credentials, real PANs, real PII — use deterministic fakes (`4111111111111111` is a known test card)
- `@Ignore` / `@Disabled` without a JIRA/issue link in the annotation reason
- Tests that print to stdout instead of asserting
- Test classes longer than ~500 lines — split by feature area
- Commenting out failing tests "to be fixed later"

## Cross-References

- `testing-philosophy.md` (why and what)
- Rule 05 — `quality-gates.md` (coverage thresholds)
- Rule 10 — ANTI-009 (Thread.sleep prohibition)
- `knowledge/testing/contract-{openapi,events,grpc}.md`
- `knowledge/testing/mutation-{java,go,python,js}.md`

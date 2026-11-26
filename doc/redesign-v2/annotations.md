# JUnit Annotation Model for AI Playbook Tests

This document defines the annotation design for integrating Neo Aura AI v2 playbook tests into JUnit 5/6.

> **Context**: This is part of the [Neo Aura AI v2 Architecture](architecture.md) redesign. See Q2 and Q3 in the architecture Q&A section for the decisions that led to this design.

---

## Design Principles

1. **Convention over configuration** — zero-arg annotations work by naming convention
2. **`@Test` always required** — `@AiPlaybook` complements `@Test`, never replaces it. Every `@NeodymiumAiTest` class must have at least one `@Test` method
3. **Composable** — class-level defaults, method-level overrides
4. **No legacy coupling** — `@AiDataSet` is separate from classic `@DataSet`
5. **Programmatic escape hatch** — explicit `AiSession` API always available
6. **Future-proof** — domain annotations (`@AiSelenide`, `@AiRest`, ...) slot in later
7. **Mandatory testIds** — data sets with 2+ entries require explicit `testId`; single data set defaults to `"default"`

---

## Annotation Overview

| Annotation | Level | Purpose |
|---|---|---|
| `@NeodymiumAiTest` | Class | Marks this as an AI test class, wires the JUnit extension |
| `@AiPlaybook` | Method | Marks a method as a playbook-driven test |
| `@AiDataFile` | Class / Method | Binds an external YAML/JSON test data file |
| `@AiDataSet` | Class / Method | Filters which data sets to include or exclude |
| `@AiMode` | Class / Method | Overrides the default execution mode(s) |
| `@AiSelenide` / `@AiRest` / ... | Class / Method | Domain selection (future, defaults to Selenide) |

---

## 1. `@NeodymiumAiTest` (Class-Level)

Marks the class as an AI playbook test class. Wires the JUnit 5 `TestTemplateInvocationContextProvider` extension that handles playbook loading, data set iteration, session lifecycle, and recording.

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(NeodymiumAiRunner.class)
public @interface NeodymiumAiTest
{
    /**
     * Optional playbook file path(s). If empty, the extension looks for
     * {@code ClassName.yaml} by convention in the classpath resource folder
     * matching the test class package.
     *
     * If a value is a directory path (ends with '/'), all .yaml files
     * in that directory are loaded, each becoming a separate test invocation.
     *
     * Supports regex patterns for file matching within directories
     * (e.g., "playbooks/checkout-.*\\.yaml").
     */
    String[] value() default {};
}
```

### Resolution rules:
| Annotation | Behavior |
|---|---|
| `@NeodymiumAiTest` | Looks for `ClassName.yaml` in classpath alongside the test class |
| `@NeodymiumAiTest("checkout.yaml")` | Loads specific file |
| `@NeodymiumAiTest("playbooks/shop/")` | Loads all `.yaml` files in the directory |
| `@NeodymiumAiTest({"a.yaml", "b.yaml"})` | Loads multiple specific files |
| `@NeodymiumAiTest("playbooks/checkout-.*\\.yaml")` | Regex: loads all matching files |

---

## 2. `@AiPlaybook` (Method-Level)

Marks a test method as playbook-driven. The extension invokes it once per data set (or once if no data sets are defined).

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AiPlaybook
{
    /**
     * Optional playbook file override. If empty, uses the class-level
     * playbook resolved from @NeodymiumAiTest.
     */
    String value() default "";
}
```

### Method scenarios:

```java
@NeodymiumAiTest("shop.yaml")
class ShopTest
{
    // Uses class-level "shop.yaml", all data sets, global config
    @Test
    void fullCheckout() {}

    // Uses class-level "shop.yaml", filtered to "premium" data set only
    @Test
    @AiPlaybook
    @AiDataSet("premium")
    void premiumCheckout() {}

    // Overrides with a different playbook entirely
    @Test
    @AiPlaybook("login.yaml")
    void loginFlow() {}

    // Programmatic AiSession (still needs @Test)
    @Test
    void customTest()
    {
        final AiSession session = AiSession.selenide(ExecutionMode.RECORD);
        final Playbook playbook = Playbook.builder()
            .step("Navigate to homepage")
            .step("Click login button")
            .build();
        session.execute(playbook);
    }
}
```

**Key rule**: A bare `@Test` method inside a `@NeodymiumAiTest` class uses the class-level playbook and global configuration. `@AiPlaybook` is only needed to override the file or add method-specific configuration.

### Convention-based simplest form:

```java
// GuestCheckoutTest.yaml exists alongside this class
@NeodymiumAiTest
class GuestCheckoutTest
{
    @Test
    void test() {}
}
```

> [!IMPORTANT]
> **Decision**: `@Test` is **always required** on every test method. A bare `@Test` inside a `@NeodymiumAiTest` class runs against the class-level playbook with global defaults. `@AiPlaybook` is optional additional metadata for overriding the file or adding method-specific config. Each `@NeodymiumAiTest` class must have at least one `@Test` method (validated by the extension at startup).

---

## 3. `@AiDataFile` (Class / Method)

Declares an external test data file (YAML or JSON) to bind to an AI test.

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AiDataFile
{
    /**
     * Optional test data file path override. If empty, uses convention auto-discovery.
     */
    String value() default "";
}
```

### Purpose & Behavior:
- **Programmatic Tests**: When applied to a programmatic test (`@AiPlaybook(AiPlaybook.PROGRAMMATIC)`), the data file supplies datasets, intra-dataset variable interpolations, inclusions (`_include:`), and prompt add-ons (`promptAddon:`). Any `steps:` block defined inside the file is deliberately ignored, ensuring the Java code remains the sole workflow driver.
- **Convention Auto-Discovery**: If `@AiDataFile` is omitted, `NeodymiumAiRunner` automatically checks for companion data files matching `<package>/<TestClassName>_<methodName>.yaml` or `<package>/<TestClassName>.yaml` (along with `.yml` and `.json`).
- **3-Tier Data Precedence Hierarchy**:
  1. **Tier 1 (Base / Lowest)**: External Data File (`@AiDataFile` or convention `<TestClassName>.yaml`).
  2. **Tier 2 (Middle)**: Inline `data:` or `SessionData` passed to `session.execute(...)`.
  3. **Tier 3 (Top / Highest)**: Direct runtime programmatic assignment via `session.data().set(...)` or `Neodymium.getData().put(...)`.

---

## 4. `@AiMode` (Class / Method)

Overrides the default execution mode. By default, the extension checks for an existing `PlaybookRecording` — if found, uses `REPLAY_AND_FIX`; if not, uses `RECORD`.

### Available Execution Modes:

| Mode | LLM Used? | Recording Read? | Recording Written? | Use Case |
|---|---|---|---|---|
| `LLM_ONLY` | ✅ Yes | ❌ No | ❌ No | Exploratory/one-shot runs — no recording consumed or produced |
| `RECORD` | ✅ Yes | ❌ No | ✅ Yes | First run to generate a new recording baseline |
| `REPLAY_ONLY` | ❌ No | ✅ Yes | ❌ No | CI strict regression — fails hard on any divergence |
| `REPLAY_AND_FIX` | ✅ Fallback | ✅ Yes | ✅ Updated | Default production mode — replay cached, self-heal on divergence |

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AiMode
{
    /**
     * The execution mode(s) to use. If multiple modes are specified,
     * the test is executed sequentially in each mode (e.g., RECORD first,
     * then REPLAY_ONLY to verify the recording works).
     */
    ExecutionMode[] value();
}
```

### Usage:

```java
// Force LLM-only, never read/write recordings
@AiMode(ExecutionMode.LLM_ONLY)
@AiPlaybook
void exploratory() {}

// Two-pass: first record via LLM, then verify the recording replays cleanly
@AiMode({ExecutionMode.RECORD, ExecutionMode.REPLAY_ONLY})
@AiPlaybook
void verifiedRecording() {}
```

### Default behavior (no `@AiMode`):

```
Has PlaybookRecording on disk?
  ├─ Yes → REPLAY_AND_FIX
  └─ No  → RECORD
```

> [!NOTE]
> **Future dimension expansion**: Execution mode could eventually become a test dimension inside the YAML `data:` block (e.g., `mode: "REPLAY_ONLY"` per data set). When we upgrade to a more elaborate dimension system, `@AiMode` at the annotation level would set the default, but individual data set entries could override it. This is deferred to the dimension discussion.

---

## 4. `@AiDataSet` (Class / Method)

Filters which data sets from the playbook to run. Separate from classic `@DataSet` to avoid breaking existing Neodymium behavior.

### Data Set ID Rules:
- **0 data sets**: No iteration — test runs once with no data bindings
- **1 data set**: `testId` defaults to `"default"` if omitted
- **2+ data sets**: `testId` is **mandatory** on every entry — the parser validates and fails fast with a clear error if any entry is missing a `testId`

Since `testId` is always present, **selection is always by `testId`** — no index-based selection needed.

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(AiDataSets.class)
public @interface AiDataSet
{
    /**
     * Include data sets by testId. If specified, ONLY these data sets run.
     * Supports regex patterns (e.g., "user-.*").
     * Mutually exclusive with {@link #exclude()}.
     */
    String[] value() default {};

    /**
     * Include data sets by testId. Alias for {@link #value()}.
     * Supports regex patterns.
     */
    String[] include() default {};

    /**
     * Exclude data sets by testId. All data sets run EXCEPT these.
     * Supports regex patterns (e.g., "(broken|deprecated)-.*").
     * Mutually exclusive with {@link #include()}.
     */
    String[] exclude() default {};
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AiDataSets
{
    AiDataSet[] value();
}
```

### Usage:

```java
// Run only "perfect" data set
@Test
@AiDataSet("perfect")
void perfectOnly() {}

// Run all except "bad"
@Test
@AiDataSet(exclude = "bad")
void skipBad() {}

// Run specific data sets by testId
@Test
@AiDataSet(include = {"perfect", "normal"})
void goodQuality() {}

// Regex: run all data sets whose testId starts with "user-"
@Test
@AiDataSet(include = "user-.*")
void allUserVariants() {}

// Regex: exclude all data sets matching a pattern
@Test
@AiDataSet(exclude = "(broken|deprecated)-.*")
void skipBrokenAndDeprecated() {}

// Class-level default: all methods skip "bad" unless overridden
@NeodymiumAiTest("checkout.yaml")
@AiDataSet(exclude = "bad")
class ShopTest
{
    @Test
    void checkout() {} // inherits exclude = "bad"

    @Test
    @AiDataSet("bad") // method override: runs ONLY "bad"
    void badQualityTest() {}
}
```

> [!NOTE]
> Both `include` and `exclude` values are matched as **Java regex patterns** against `testId`. Literal strings match exactly; patterns like `"user-.*"` or `"(gold|silver)"` enable flexible selection.

### Precedence: Method-level `@AiDataSet` **overrides** class-level (not additive).

---

## 5. Domain Annotations (Future)

Domain selection defaults to **Selenide** when not specified. Future annotations allow targeting other domains:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AiSelenide
{
    /** Browser profile ID (from browser.properties). Default: use default browser. */
    String browser() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AiRest
{
    /** Base URL for REST API requests. */
    String baseUrl() default "";
}

// Future: @AiPlaywright, @AiCli, @AiDatabase, ...
```

### Usage:

```java
@NeodymiumAiTest("api-tests.yaml")
@AiRest(baseUrl = "${api.baseUrl}")
class ApiTest
{
    @Test
    @AiPlaybook
    void healthCheck() {}
}
```

> [!NOTE]
> Domain selection can also be done programmatically via `AiSession.selenide()`, `AiSession.rest()`, etc. when using the explicit API.

---

## 6. Hooks & Lifecycle

### Standard JUnit lifecycle with AI playbooks:

`@BeforeEach` and `@AfterEach` methods can also carry `@AiPlaybook` (or other AI annotations) to run AI-driven setup or teardown. The session and browser/SUT state is shared across the full `before → test → after` lifecycle within each data set invocation.

```java
@NeodymiumAiTest("checkout.yaml")
class CheckoutTest
{
    // AI-driven setup: runs a login playbook before each test
    @BeforeEach
    @AiPlaybook("login.yaml")
    void login() {}

    // Plain JUnit setup: inject data, configure external state
    @BeforeEach
    void seedTestData()
    {
        // Standard JUnit — no AI involvement
    }

    @Test
    void checkout() {}

    // AI-driven teardown: runs a cleanup playbook after each test
    @AfterEach
    @AiPlaybook("logout.yaml")
    void logout() {}

    // Plain JUnit teardown
    @AfterEach
    void verifyNoErrors()
    {
        // Standard JUnit — assertions on side effects, logs, etc.
    }
}
```

> [!NOTE]
> When `@BeforeEach` or `@AfterEach` carries `@AiPlaybook`, the extension executes that playbook using the same `AiSession` and SUT state as the test method. This means a `@BeforeEach` login flow leaves the browser on the authenticated page, and the `@Test` method continues from there.

### AI-specific hooks:
Pre/PostExecutionHooks are **NOT** registered via annotations. They are:
1. **Programmatic** — registered on `AiSession` when using the explicit API
2. **Property-driven** — built-in hooks (like `LlmExecutionAuditor`, `DataConsistencyAuditor`) are toggled via `neodymium.properties` or `ai.properties`

```properties
# Enable built-in post-execution auditors
neodymium.ai.hooks.llmAuditor.enabled=true
neodymium.ai.hooks.dataConsistencyAuditor.enabled=true
neodymium.ai.hooks.visualAuditor.enabled=false
```

The annotation-driven runner reads these properties and registers the appropriate hooks automatically on the `AiSession` it creates internally.

---

## Complete Example: Full-Featured Test Class

```java
@NeodymiumAiTest("playbooks/checkout/")  // all YAMLs in directory
@AiDataSet(exclude = "broken")            // class-level: skip broken data sets
class CheckoutSuiteTest
{
    @BeforeEach
    void prepareTestUser()
    {
        // Standard JUnit setup
    }

    // Runs all playbooks from directory, all non-excluded data sets
    // Default mode: REPLAY_AND_FIX (if recording exists) or RECORD
    @Test
    void standardFlow() {}

    // Specific playbook, only "premium" data set, force LLM_ONLY
    @Test
    @AiPlaybook("vip-checkout.yaml")
    @AiDataSet("premium")
    @AiMode(ExecutionMode.LLM_ONLY)
    void vipPremiumFlow() {}

    // Two-pass verification: record then replay
    @Test
    @AiPlaybook("smoke.yaml")
    @AiMode({ExecutionMode.RECORD, ExecutionMode.REPLAY_ONLY})
    void verifiedSmoke() {}

    // Programmatic test — still requires @Test
    @Test
    void customProgrammaticTest()
    {
        final AiSession session = AiSession.selenide(ExecutionMode.RECORD);
        session.execute(Playbook.builder()
            .step("Navigate to homepage")
            .step("Verify logo is visible")
            .build());
    }

    @AfterEach
    void cleanup()
    {
        // Standard JUnit teardown
    }
}
```

---

## Summary Table

| Concern | Annotation | Default |
|---|---|---|
| "This is an AI test class" | `@NeodymiumAiTest` | Convention: `ClassName.yaml` |
| "This method runs a playbook" | `@AiPlaybook` | Uses class-level file |
| Execution mode | `@AiMode` | Auto-detect (recording exists → REPLAY_AND_FIX, else RECORD) |
| Data set filtering | `@AiDataSet` | All data sets |
| Domain (browser/REST/...) | `@AiSelenide` / `@AiRest` | Selenide |
| Setup/teardown | `@BeforeEach` / `@AfterEach` | Standard JUnit (can carry `@AiPlaybook`) |
| AI hooks | Properties | Disabled by default |

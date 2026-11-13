# Neodymium Maven Test Profiles Guide

Neodymium houses a rich suite of over 570 test classes across four modules (`neodymium-core`, `neodymium-test-server`, `aura-manager`, `neodymium-e2e-tests`).

To provide fast, reproducible, and cost-effective test execution, dedicated Maven profiles are available at the parent aggregator level and across all child modules.

---

## Quick Reference Table

| Profile | Command | Target Scope | Key Inclusions | Strict Exclusions |
| :--- | :--- | :--- | :--- | :--- |
| **Non-LLM Modern** | `mvn test -Pnon-llm-tests` | Complete modern suite (CI default) | Core AI, Server, VÉRLA UI, VÉRLA mock AI, Sandbox mock, AI Manager | **Zero** classic JUnit 4/5, **zero** live LLMs |
| **Direct Unit** | `mvn test -Punit-tests` | Pure in-memory unit tests (< 5s) | AI prompts, models, telemetry, catalog parsing, manager DTOs/entities | **Zero** browsers, **zero** servers, **zero** AI/LLM |
| **Neo Classic JUnit 4** | `mvn test -Pneo-classic-junit4` | Legacy JUnit 4 Neodymium tests | `com.xceptance.neodymium.junit4.tests.**` (Vintage engine) | **Zero** JUnit 5, **zero** AI, **zero** Verla |
| **Neo Classic JUnit 5** | `mvn test -Pneo-classic-junit5` | Legacy JUnit 5 Neodymium tests | `com.xceptance.neodymium.junit5.tests.**`, classic data utils | **Zero** JUnit 4, **zero** AI, **zero** Verla |
| **VÉRLA Storefront** | `mvn test -Pverla-tests` | VÉRLA storefront UI & mock AI flows | Storefront UI tests (Tailwind, Claude, Apocalypse, etc.), VÉRLA AI mock checkout | **Zero** classic tests, **zero** live LLMs |
| **AI Mock** | `mvn test -Pai-mock-tests` | AI pipelines with mocked LLMs (0 tokens) | Core AI, replay, sandbox mock, VÉRLA mock | **Zero** live LLMs, **zero** classic tests |
| **AI Live** | `mvn test -Pai-live-tests` | Real LLM verification (`GEMINI_API_KEY`) | Tests tagged `@Tag("LiveAPI")`, `@Tag("LiveLlm")` | **Zero** mock tests, **zero** classic tests |
| **AI Sandbox** | `mvn test -Pai-sandbox` (or `-Paura-sandbox`) | AI Glance visual defect sandbox | `org.neodymium.ai.integration.sandbox.**` | **Zero** classic tests, **zero** live LLMs |
| **AI Manager** | `mvn test -Pai-manager` (or `-Paura-manager`) | Neodymium AI Manager Spring Boot hub | Web controllers, services, repositories, UI in `aura-manager` | Other modules skipped |
| **Smoke** | `mvn test -Psmoke` | Fast sanity verification (< 60s) | 1 Verla storefront, 1 mock checkout, 1 sandbox mock, core catalog & unit tests | Non-smoke tests |

---

## Detailed Profile Breakdown

### 1. `non-llm-tests` (Modern Neodymium Suite)
The default recommended profile for Pull Requests, continuous integration, and local pre-commit checks.
* **Why use it**: Runs the comprehensive modern Neodymium and AI test suite with 100% determinism.
* **Guarantees**: Never connects to live LLMs (zero token consumption, no API keys required) and never executes legacy "Neo Classic" tests.
* **Usage**:
  ```bash
  mvn test -Pnon-llm-tests
  ```

### 2. `unit-tests` (Direct In-Memory Unit Tests)
* **Why use it**: Extremely rapid feedback loop during active development.
* **Guarantees**: Finishes in seconds. Never launches a Chrome/Chromium browser window, never opens an embedded Javalin server port, and never executes LLM inferences.
* **Usage**:
  ```bash
  mvn test -Punit-tests
  ```

### 3. `neo-classic-junit4` & `neo-classic-junit5` (Neo Classic)
To prevent cross-framework collisions and deprecated engine overhead, JUnit 4 and JUnit 5 classic tests are strictly isolated into distinct profiles:
* **`neo-classic-junit4`**:
  Executes classic JUnit 4 tests (`NeodymiumRunner`, `@Browser`, `@DataSet`) via JUnit Platform Vintage engine:
  ```bash
  mvn test -Pneo-classic-junit4
  ```
* **`neo-classic-junit5`**:
  Executes classic JUnit 5 tests (`NeodymiumExtension`) via JUnit Platform Jupiter engine:
  ```bash
  mvn test -Pneo-classic-junit5
  ```

### 4. `verla-tests` (VÉRLA Storefronts & AI Flows)
* **Why use it**: Validates both the raw HTML/HTMX storefront behavior and Neodymium AI test flows against the local embedded VÉRLA e-commerce application.
* **Target Storefronts**: Tailwind, Claude Tailwind, Apocalypse, Headless, Apparel, PWA Chaos, and multi-locale i18n variants.
* **Usage**:
  ```bash
  mvn test -Pverla-tests
  ```

### 5. `ai-mock-tests` vs. `ai-live-tests`
Mocked and real LLM tests are strictly partitioned:
* **`ai-mock-tests`**:
  Executes AI tests using pre-recorded golden mock responses. Fully deterministic, runs offline, zero cost:
  ```bash
  mvn test -Pai-mock-tests
  ```
* **`ai-live-tests`**:
  Executes tests that call live external LLM endpoints (Gemini, Claude, GPT). Requires valid API credentials and internet connectivity:
  ```bash
  export GEMINI_API_KEY="your-api-key"
  mvn test -Pai-live-tests
  ```

### 6. `ai-sandbox` (legacy: `aura-sandbox`)
* **Why use it**: Focuses specifically on AI Glance visual defect detection, dHash baseline persistence, multi-port visual comparisons, and dynamic defect replays against the `AuraGlanceTest` sandbox suite.
* **Usage**:
  ```bash
  mvn test -Pai-sandbox
  # or legacy alias:
  mvn test -Paura-sandbox
  ```

### 7. `ai-manager` (legacy: `aura-manager`)
* **Why use it**: Focuses solely on testing the Neodymium AI Manager Spring Boot hub (`aura-manager` module), including dashboard controllers, queue management, file controllers, and reporting storage.
* **Usage**:
  ```bash
  mvn test -Pai-manager
  # or legacy alias:
  mvn test -Paura-manager
  ```

### 8. `smoke`
* **Why use it**: Ultra-fast sanity check under 60 seconds to ensure core system components and runners function before pushing changes:
* **Usage**:
  ```bash
  mvn test -Psmoke
  ```

---

## Filtering Within Profiles

All profiles support narrowing down to specific classes or methods via `-Dtest`:
```bash
# Run a specific VÉRLA test under the verla-tests profile
mvn test -Pverla-tests -Dtest=VerlaTailwindStorefrontTest

# Run a specific unit test under the unit-tests profile
mvn test -Punit-tests -Dtest=VerlaCatalogTest
```
Because `failIfNoSpecifiedTests=false` is configured across all modules, `-Dtest` can be safely executed from the repository root without failing modules where the test class does not reside.

## Why

Traditional test reporting frameworks, such as Allure, are highly static, monolithic, and dated. They fail to support modern, advanced automation requirements, particularly around:
1. **AI/LLM-Driven Execution**: There is no native support for logging LLM reasoning history, system context levels (Lean vs. Visual), self-healing path logs, or token usage.
2. **Visual Regression Diagnostics**: Screenshots are basic attachments; they lack interactive before/after side-by-side split sliders or perceptual dHash differences.
3. **Stateful Analytics & Trends**: Static HTML reports cannot track historical test duration trends, branch regressions, or auto-detect flakiness across commits.
4. **Offline Distribution Barriers**: Allure requires a local web server (CORS restrictions) to view compiled reports, making sharing difficult.
5. **Dependency Bloat & Rigid Lifecycles**: Relying on Allure binds Neodymium to heavy external dependencies (`allure-java-commons`, etc.) and a brittle thread-local listener model that forces fragile reflection hacks to support dynamic features like self-healing and transient retries.

By completely replacing Allure with our own native capture framework and introducing **Aura Server** (a stateful local Java application running on `localhost:8080`), we take absolute ownership of our test lifecycle. Neodymium captures events natively using built-in hooks, serializes them to highly optimized, open JSON/PNG directories on disk, and streams them in real-time to Aura Server. This eliminates Allure completely, making Neodymium leaner, faster, and 100% flexible. To maintain a fully open ecosystem, the server supports both direct local file writing by external runners and a generic HTTP ingestion API where any remote execution tool can send us data and Aura Server writes it to the local disk structure.

## What Changes

1. **Allure Elimination**: Completely remove Allure libraries and annotations from Neodymium. Replace them with native Neodymium annotations (e.g. `@TestStep`, `@VisualAssertion`, `@ExpectedFailure`).
2. **Aura Server (New App)**: Create a lightweight Spring Boot 3.x/Java 21 standalone application. On startup, it scans a configured external results directory to parse and index test run history into a local H2/SQLite database.
3. **Native Capture Listener**: Build a lightweight, native execution listener in Neodymium that captures step lifecycles, intercepts failures, records screenshots, and serializes the state to disk without external logging frameworks.
4. **Dynamic Ingestion & Open Ingestion API**: Support both direct flat-file disk-writing (for ultimate offline execution) and a standard HTTP ingestion receiver where remote/external test suites (e.g. Cypress, Playwright) can send us data, and Aura Server automatically handles writing it to the open filesystem structure.
5. **Interactive Control Panel**: A premium, responsive, glassmorphic Thymeleaf dashboard for run statistics, interactive visual regression comparison (swipe sliders with manual baseline approval), and LangChain4j failure diagnostics.

## Capabilities

## New Capabilities

- `aura-server`: Statefully index test histories, visual regression baselines, AI healing pipelines, and flakiness heuristics. **No initial access control layer** (zero authentication, zero-config MVP) is required, ensuring zero developer friction. The design maintains compatibility for hosted, multi-tenant concepts in the future.
- `aura-capture`: Native Neodymium test lifecycle listener and annotations, providing 100% self-sufficient step, screenshot, and visual hash capture.
- `aura-trace-viewer`: Interactive post-mortem diagnostic console displaying complete step-by-step timelines with DOM snapshots, network request/response headers, browser console logs, and screenshots, enabling instant debugging without re-running tests.
- `aura-live-stream`: Real-time streaming tap for ongoing test suites to receive updates dynamically in the dashboard.
- `aura-open-ingestion`: Support for third-party frameworks to either write directly to our open filesystem layout or POST structured payloads to our receiver, ensuring we do not close the ecosystem.
- `aura-static-generator`: A command-line tool / output option that generates a self-contained **offline static report** (HTML + client-side JS + local file asset links) representing a snapshot of the run's state. Users can host this statically on S3, GitHub Pages, or any static server for zero-infrastructure sharing.
- `aura-quality-gates`: Continuous testing guard that assesses run quality against declarative failure rate thresholds, critical component successes, and regression gates. Exposes API endpoints for CI pipelines to instantly get a pass/fail verdict.

### Modified Capabilities

- `reporting`: Complete deprecation and removal of Allure reporting XML/JSON outputs, replacing them with standardized Aura external file bundles (`target/aura-results/`) and the ability to trigger a static offline report compile step or query automated quality gates.
- `test-management`: Connect automated test suite runs statefully with the manual test cases backlog.

## Impact

- **Dependency Clean Up**: Delete all `io.qameta.allure` Maven dependencies from Neodymium's `pom.xml`.
- **New Subproject**: `aura-server/` (Spring Boot project, Thymeleaf/HTMX UI, H2 database, LangChain4j integration).
- **Test Automation Integration**: Implement `@TestStep` and the native `AuraCaptureListener` in Neodymium to write standardized JSON results and stream live updates if the Aura Server is online.
- **Unified Ingestion, Export, & Quality Framework**: Provide documented JSON schemas and endpoint contracts for third-party environments to send or write test data in a standardized manner, along with a static offline compilation engine and automated Quality Gates for CI/CD gates.





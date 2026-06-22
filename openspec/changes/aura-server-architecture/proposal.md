## Why

Traditional test reporting frameworks, such as Allure, are highly static, monolithic, and dated. They fail to support modern, advanced automation requirements, particularly around:
1. **AI/LLM-Driven Execution**: There is no native support for logging LLM reasoning history, system context levels (Lean vs. Visual), self-healing path logs, or token usage.
2. **Visual Regression Diagnostics**: Screenshots are basic attachments; they lack interactive before/after side-by-side split sliders or perceptual dHash differences.
3. **Stateful Analytics & Trends**: Static HTML reports cannot track historical test duration trends, branch regressions, or auto-detect flakiness across commits.
4. **Offline Distribution Barriers**: Allure requires a local web server (CORS restrictions) to view compiled reports, making sharing difficult.
5. **Dependency Bloat & Rigid Lifecycles**: Relying on Allure binds Neodymium to heavy external dependencies (`allure-java-commons`, etc.) and a brittle thread-local listener model that forces fragile reflection hacks to support dynamic features like self-healing and transient retries.

By introducing **Aura Server** (a stateful local Java application running on `localhost:8080`) specifically for AI-driven tests, we take absolute ownership of the test lifecycle for Neo AI. Since Aura is used for Neo AI first only, we don't need to replace the Allure base framework; we keep Neo Classic completely as it is and only extend Neo AI with Aura first. Neodymium captures events natively for Neo AI runs using built-in hooks, serializes them to highly optimized, open JSON/PNG directories on disk, and streams them in real-time to Aura Server. This coexists perfectly with Allure, making Neo AI highly flexible and lightweight without disrupting standard Neo Classic test runs. To maintain a fully open ecosystem, the server supports both direct local file writing by external runners and a generic HTTP ingestion API where any remote execution tool can send us data and Aura Server writes it to the local disk structure.

## What Changes

1. **Allure Coexistence & Neo Classic Preservation**: Keep Allure libraries and annotations fully active in Neodymium for Neo Classic suites. For Neo AI, introduce native Neodymium annotations (e.g., `@TestStep`, `@VisualAssertion`, `@ExpectedFailure`) that coexist side-by-side with Allure annotations.
2. **Aura Server (New App)**: Create a lightweight Spring Boot 1.5.x (Spring 4.x)/Java 8 standalone application. On startup, it scans a configured external results directory to parse and index test run history into a local H2/SQLite database.
3. **Neo AI Native Capture Listener**: Build a lightweight, native execution listener in Neodymium that captures step lifecycles, intercepts failures, records screenshots, and serializes the state to disk for Neo AI execution, coexisting alongside the legacy Allure listener.
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

- `reporting`: Neo Classic continues using Allure reporting XML/JSON outputs without changes. Neo AI outputs standardized Aura external file bundles (`target/aura-results/`) and supports streaming to Aura Server, enabling high-fidelity diagnostic tracing, visual regression, and conversational AI analysis specifically for AI-driven test runs. **Transition Step**: The ingestion pipeline natively supports the structured JSON layout produced by the lightweight `quick-test-report` capability (introduced in the `human-readable-test-report` change), providing instant backward compatibility and a seamless bridge for runs.
- `test-management`: Connect automated test suite runs statefully with the manual test cases backlog.

## Feedback Integration & Priorities

To address feedback across QA, architecture, DevOps, and business stakeholders, the following architectural guardrails and priority levels are integrated into the Aura project roadmap:

### 1. High Priority (Must-Have for MVP)
- **Non-Blocking Capture & Performance Safeguards (`aura-capture`)**: All file writing, DOM serialization, and network trace collections MUST occur asynchronously or be offloaded to a background thread to prevent introducing execution latency to the test runner.
- **WebSocket Resiliency (`aura-live-stream`)**: The WebSocket live-stream appender MUST operate via a non-blocking queue and gracefully degrade (with near-zero overhead) if the Aura Server is unreachable or offline.
- **Robust Re-Indexing Heuristics (`aura-server`)**: If the embedded H2/SQLite database is corrupted or deleted, the server MUST support a complete "zero-data-loss re-indexing trigger" that rebuilds the index from the raw flat files on disk.
- **CI/CD Storage Scavenger Policy (`aura-server`)**: Implement a declarative scavenger service in the server to auto-purge or downscale raw asset files (screenshots and DOM snapshots) older than `X` days, while retaining historical metadata trends.

### 2. Medium Priority (Should-Have for Hardening)
- **Annotations Migration Bridging**: Provide a lightweight backward-compatibility adapter or custom listener mapping legacy `io.qameta.allure` annotations (e.g., `@Step`) to native `@TestStep` without requiring immediate complete codebase refactoring.
- **Multi-Tenant Schema Readiness**: The underlying database schemas (`Runs`, `TestCases`, etc.) MUST include foundational fields for `tenant_id` and `project_id`. While the MVP runs open on `localhost:8080` without authentication, this prevents major relational rewrites for a future hosted SaaS platform.

### 3. Low/Deferred Priority (Nice-to-Have / Post-MVP)
- **Gemini API Cost & Token Budgeting**: Track and display exact Gemini token usage and estimated costs in the UI. Provide configurable caps or caching to prevent excessive API bills during mass automated test runs.

## Impact

- **Dependency Coexistence**: Retain `io.qameta.allure` Maven dependencies in Neodymium's `pom.xml` for Neo Classic compatibility, while adding dependencies required for Aura.
- **New Subproject**: `aura-server/` (Spring Boot 1.5.x (Spring 4.x) / Java 8 project, Thymeleaf/HTMX UI, H2 database, LangChain4j integration).
- **Test Automation Integration**: Implement `@TestStep` and the native `AuraCaptureListener` in Neodymium to write standardized JSON results and stream live updates if the Aura Server is online.
- **Unified Ingestion, Export, & Quality Framework**: Provide documented JSON schemas and endpoint contracts for third-party environments to send or write test data in a standardized manner, along with a static offline compilation engine and automated Quality Gates for CI/CD gates.






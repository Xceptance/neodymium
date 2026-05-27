## Context

Current test runners (such as Neodymium) write reporting metadata directly into Allure-compatible formats or inline HTML summaries. When dynamic processes like AI self-healing or transient retries occur, Allure's thread-local lifecycle is extremely difficult to interact with, forcing fragile reflection hacks on internal classes. To avoid disrupting legacy suites, Neodymium keeps its Allure implementation completely intact for Neo Classic runs, but introduces Aura specifically to support Neo AI execution.

To overcome this, **Aura** decouples **execution capture** (saving raw, version-controllable JSON/PNG directories to the local disk) from **indexing & display** (managed by a stateful local Java server). When the server starts up, it reads the external directory, builds an optimized relational database index, and delivers a premium interactive console. During active execution, the runner can redirect its output stream directly to the server for real-time console feedback.

## Goals / Non-Goals

**Goals:**
- **Open-File Source of Truth**: Keep execution history, step timelines, playbooks, and screenshots in flat, highly inspectable directories on disk.
- **Framework-Agnostic Open Standard**: The intermediate data schemas (JSON layouts) and streaming event protocols must be fully open, public, and generic. Any testing framework (e.g., Playwright, Cypress, Cypress/Webdriver in Python, or unit tests) can serialize results to this format or stream events directly to the server.
- **Ultra-Fast Local Index**: Scan and index hundreds of runs in under a second on startup using H2 or SQLite.
- **Interactive Visual Lab**: Provide side-by-side swipe sliders for visual mismatches, complete with a "manual baseline approval" engine.
- **AI Diagnostics Workspace**: Integrate LangChain4j (Gemini) to inspect step failures, DOMs, and logs directly from the UI.
- **Zero-Config Developer Flow**: Boot the server on `localhost:8080` with a zero-dependencies embedded database.
- **Static HTML Export (Offline Snapshot)**: Support compiling any test run folder into a completely static offline-viewable layout consisting of standalone HTML, standard vanilla JS, and links to local screenshot files. This provides an easily distributable and hostable static snapshot of a specific run state.

**Non-Goals:**
- Forcing a persistent cloud database; the system is designed to run locally, offline, and self-contained on the developer's machine or a local CI agent.
- Modifying standard JUnit test dispatch mechanisms.
- **User Authentication, Accounts, & Roles in MVP**: To keep local development fast and zero-overhead, the dynamic server will launch without any access control layer (no rights, roles, or accounts). We explicitly design the underlying schema so it can be extended into a multi-tenant hosted service later, but we will start completely open and authentication-free.

## Decisions

### 1. Dedicated Aura Lifecycle for Neo AI (Allure Coexistence)
- **Decision**: Keep Allure fully functional for Neo Classic suites. For Neo AI, introduce native Neodymium metadata annotations (e.g., `@TestStep`, `@VisualAssertion`, `@ExpectedFailure`) and implement a native `AuraCaptureListener` that captures lifecycle hooks specifically during Neo AI executions.
- **Alternative**: Completely replace Allure for all test suites.
- **Rationale**: Completely replacing Allure would force immediate migration of legacy Neo Classic test suites, introducing high friction. By preserving the Allure framework for Neo Classic and dedicating Aura to Neo AI, we gain the full benefits of dynamic step tracing, visual hashes, and self-healing capture for AI-driven tests while maintaining 100% backward compatibility and zero disruption for traditional test suites.

- **Decision**: All results are stored in `target/aura-results/run_<timestamp>_<branch>_<commit>/` using a documented, generic JSON/PNG layout:
  - `run-info.json`: Execution summary, branch/commit SHA, browser environment, and total metrics.
  - `test-cases/<className>_<methodName>.json`: Structured chronological step results, actions, statuses, locators, perceptual visual hashes, and links to trace files.
  - `screenshots/*.png`: Visual execution captures.
  - `dom-snapshots/<className>_<methodName>_step<stepIndex>.html`: Full, serialized HTML DOM snapshots taken at each step.
  - `traces/<className>_<methodName>_network.json`: Complete browser network requests (endpoints, response statuses, headers, sizes, durations) grouped by step.
  - `traces/<className>_<methodName>_console.json`: Browser console error, warning, and info logs captured at each step.
  - `logs/<className>_ai.json`: Dynamic AI discussions, DOM extracts, and prompts.
- **Alternative**: Couple the schema tightly to Neodymium's internal class objects.
- **Rationale**: Keeping the intermediate data representation fully open and generic provides maximum **source flexibility** and avoids closing the ecosystem. Any external tool, framework, or developer can **write directly in our defined way** (e.g., standard files on disk) without running the server at all. Aura Server will ingest and display them flawlessly, making Aura a universal test intelligence platform rather than a proprietary silo.

### 3. Spring Boot + H2 Speed Index Layer
- **Decision**: Built on **Spring Boot 1.5.x (Spring 4.x)**, **Java 8 standard thread pools**, and **H2/SQLite**. On boot, an `AuraFileIngester` scans the results directory, compares file signatures, and bulk-inserts/updates runs in a lightweight database.
- **Rationale**: Keeps UI operations (searching, trend graphing, filtering by flakiness) extremely fast. Rebuilding the database takes seconds if files are deleted or modified.

### 4. Open Centralized Writer API & Live-Stream Event Receiver
- **Decision**: Aura Server hosts a public, generic HTTP/WebSocket ingestion and event-stream receiver (`/api/v1/ingest` and `/api/v1/stream`). 
  - **Dynamic Ingestion (Someone Else Sends, We Write)**: If an external test runner, CI worker, or custom script runs in a separate environment (where it cannot write directly to the server's local file system), it can POST its run metadata, step JSONs, and screenshot base64 payloads to this API. **Aura Server will act as the centralized writer**, serializing these incoming payloads directly into the standard flat-file directory layout on the host disk as the primary source of truth, while simultaneously updating the in-memory/H2 index.
  - **Ingestion Compatibility Bridge**: The dynamic ingestion API and ingester pipeline natively support parsing and indexing the structured JSON report produced by the lightweight `quick-test-report` capability (introduced in the `human-readable-test-report` change). This ensures instant Aura integration and backwards-compatibility for existing Neodymium suites running under standard JUnit 4/5 before refactoring to native Aura annotations.
  - **Live Progress**: An `AuraLiveStreamAppender` streams live execution updates during active runs to update the interactive HTMX dashboard in real-time.
- **Rationale**: This guarantees absolute deployment flexibility. Ecosystem clients have a choice: they can write standard Aura files directly to disk themselves, or they can send the raw data to the server's endpoint and let the server handle disk serialization for them. This keeps the ecosystem 100% open, standard, and accessible from any environment.



### 5. Multi-Dimension Timeline Capture & Interactive Trace Viewer
- **Decision**: Build an interactive Trace Viewer interface that supports full post-mortem analysis of failures without needing to re-run the tests. Neodymium captures four key data vectors at every test execution step:
  - **DOM Snapshots**: Serialized static outerHTML is captured via Javascript at every step and saved as a standalone file. The dashboard renders these inside sandboxed `<iframe>` panels, enabling developers to inspect elements, check locators, and review CSS layout states exactly as they were at the step's execution moment.
  - **Network Traces**: A dedicated background thread captures Chrome DevTools Protocol (CDP) network events (`Network.requestWillBeSent`, `Network.responseReceived`), matching requests with responses and slicing them into step timeline buckets.
  - **Console Logs**: CDP log handlers stream browser-level errors, warnings, and standard console output to step records.
  - **Screenshots**: High-resolution screenshots are captured before/after actions and linked to step timelines.
  - **Layout**: The Aura dashboard displays this as a split-pane diagnostic workspace:
    - *Left sidebar*: A chronological step/action list.
    - *Right workspace*: Tabbed panels for (1) Interactive DOM Snapshot, (2) Network Requests Table, (3) Browser Console Logs, and (4) High-Res Screenshots.
- **Alternative**: Rerun failed tests with live debuggers or rely on stdout logs.
- **Rationale**: Re-running tests to diagnose flaky failures in CI is extremely time-consuming and often fails to reproduce the exact state due to dynamic page states. Serializing DOM snapshots, network traffic, and console messages directly into the open filesystem at the moment of execution allows developers to inspect the exact failure context offline.

### 6. Perceptual dHash Visual Lab & Baseline Approval
- **Decision**: The UI includes an interactive comparison workspace. Visual regression steps display a dual-layer horizontal swipe slider. Clicking "Approve Baseline" makes the Aura Server copy the actual execution screenshot over the baseline screenshot, updating the visual hash directly.
- **Rationale**: Eliminates manual file copying and lets developers visually manage design diffs in seconds.

### 7. Conversational AI Workspace (LangChain4j)
- **Decision**: Integrate LangChain4j to connect the dashboard to Gemini. The UI features:
  - An "Analyze Failure" widget that sends the exact DOM segment and stack trace to Gemini for root-cause analysis.
  - An interactive chat panel to query suite statistics naturally (e.g., *"How many tests have failed because of locator changes on the login page this week?"*).
- **Rationale**: Turns the report from a static failure list into an active assistant that explains *why* the failure occurred and how to fix it.

### 8. Declarative Quality Gates & CI Pipeline Integrations
- **Decision**: Aura supports automated run health checks by evaluating declarative rules stored in `quality-gates.json` or configured via the UI.
  - **KPI Assessments**: Quality gates evaluate rules based on overall Failure Rate % thresholds, mandatory success for critical target suites/components, and **New Regression Detection** (ensuring no *new* unique failure signatures exist that weren't present in the previous baseline/commit run).
  - **CI/CD Integration**: The server hosts a public GET/POST endpoint `/api/v1/runs/{id}/evaluate`. CI/CD systems (GitHub Actions, Jenkins, GitLab) can perform a quick curl request to evaluate the run status, receiving a clear JSON pass/fail verdict with detailed rule breach listings to determine build pass/fail gates instantly.
- **Rationale**: Automating build-triaging via structured quality rules turns Aura from a passive reporting tool into an active, automated pipeline guard, matching premium enterprise tool capabilities (like ReportPortal) with zero setup friction.


## Risks / Trade-offs

- **Risk**: Overlap or conflict between Allure and Aura listeners during execution.
- **Mitigation**: Ensure that `AuraCaptureListener` is only active and registered during Neo AI test runs, while standard Neo Classic runs only activate the Allure listener.
- **Risk**: Maintenance overhead of supporting two reporting pipelines.
- **Mitigation**: Neo Classic's Allure integration is highly mature and stable, requiring minimal active maintenance, while Aura is developed specifically as a high-fidelity engine for new AI features.
- **Risk**: Disk space bloat due to hundreds of runs capturing high-resolution PNGs.
- **Mitigation**: Introduce a background scavenger policy in Aura Server to automatically compress, downscale, or purge screenshots older than `X` days, while retaining historical execution JSON metadata for long-term trend analysis.


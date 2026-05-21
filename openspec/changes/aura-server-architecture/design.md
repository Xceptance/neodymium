## Context

Current test runners (such as Neodymium) write reporting metadata directly into Allure-compatible formats or inline HTML summaries. When dynamic processes like AI self-healing or transient retries occur, Allure's thread-local lifecycle is extremely difficult to interact with, forcing fragile reflection hacks on internal classes. 

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

### 1. Complete Ownership of Test Lifecycle (Zero-Allure Dependency)
- **Decision**: Eliminate all `io.qameta.allure` annotations (`@Step`, `@Attachment`, etc.) and runtime libraries. Introduce native Neodymium metadata annotations (e.g. `@TestStep`, `@VisualAssertion`, `@ExpectedFailure`) and implement a native `AuraCaptureListener` hooking directly into JUnit 5/Selenide execution.
- **Alternative**: Keep using Allure listeners and translate their step formats into Aura files, or use reflection to capture lifecycle hooks.
- **Rationale**: Relying on Allure binds the framework to heavy external dependencies, rigid execution models (unable to cleanly represent dynamic AI paths, self-healing retries, or perceptual hashes), and static single-thread context limitations. By owning the lifecycle capture, we achieve absolute design flexibility, maximum runtime execution speed, and zero external reporting bloat.

### 2. Universal, Open Directory Layout & Schema (Source Flexibility)
- **Decision**: All results are stored in `target/aura-results/run_<timestamp>_<branch>_<commit>/` using a documented, generic JSON/PNG layout:
  - `run-info.json`: Execution summary, branch/commit SHA, browser environment, and total metrics.
  - `test-cases/<className>_<methodName>.json`: Structured chronological step results, actions, statuses, locators, and perceptual visual hashes.
  - `screenshots/*.png`: Visual execution captures.
  - `logs/<className>_ai.json`: Dynamic AI discussions, DOM extracts, and prompts.
- **Alternative**: Couple the schema tightly to Neodymium's internal class objects.
- **Rationale**: Keeping the intermediate data representation fully open and generic provides maximum **source flexibility** and avoids closing the ecosystem. Any external tool, framework, or developer can **write directly in our defined way** (e.g., standard files on disk) without running the server at all. Aura Server will ingest and display them flawlessly, making Aura a universal test intelligence platform rather than a proprietary silo.

### 3. Spring Boot + H2 Speed Index Layer
- **Decision**: Built on **Spring Boot 3.x**, **JDK 21 Virtual Threads**, and **H2/SQLite**. On boot, an `AuraFileIngester` scans the results directory, compares file signatures, and bulk-inserts/updates runs in a lightweight database.
- **Rationale**: Keeps UI operations (searching, trend graphing, filtering by flakiness) extremely fast. Rebuilding the database takes seconds if files are deleted or modified.

### 4. Open Centralized Writer API & Live-Stream Event Receiver
- **Decision**: Aura Server hosts a public, generic HTTP/WebSocket ingestion and event-stream receiver (`/api/v1/ingest` and `/api/v1/stream`). 
  - **Dynamic Ingestion (Someone Else Sends, We Write)**: If an external test runner, CI worker, or custom script runs in a separate environment (where it cannot write directly to the server's local file system), it can POST its run metadata, step JSONs, and screenshot base64 payloads to this API. **Aura Server will act as the centralized writer**, serializing these incoming payloads directly into the standard flat-file directory layout on the host disk as the primary source of truth, while simultaneously updating the in-memory/H2 index.
  - **Live Progress**: An `AuraLiveStreamAppender` streams live execution updates during active runs to update the interactive HTMX dashboard in real-time.
- **Rationale**: This guarantees absolute deployment flexibility. Ecosystem clients have a choice: they can write standard Aura files directly to disk themselves, or they can send the raw data to the server's endpoint and let the server handle disk serialization for them. This keeps the ecosystem 100% open, standard, and accessible from any environment.



### 5. Perceptual dHash Visual Lab & Baseline Approval
- **Decision**: The UI includes an interactive comparison workspace. Visual regression steps display a dual-layer horizontal swipe slider. Clicking "Approve Baseline" makes the Aura Server copy the actual execution screenshot over the baseline screenshot, updating the visual hash directly.
- **Rationale**: Eliminates manual file copying and lets developers visually manage design diffs in seconds.

### 6. Conversational AI Workspace (LangChain4j)
- **Decision**: Integrate LangChain4j to connect the dashboard to Gemini. The UI features:
  - An "Analyze Failure" widget that sends the exact DOM segment and stack trace to Gemini for root-cause analysis.
  - An interactive chat panel to query suite statistics naturally (e.g., *"How many tests have failed because of locator changes on the login page this week?"*).
- **Rationale**: Turns the report from a static failure list into an active assistant that explains *why* the failure occurred and how to fix it.

## Risks / Trade-offs

- **Risk**: Upgrading existing test suites to remove Allure annotations might require user rework.
- **Mitigation**: Introduce standard deprecation cycles, providing a clean compile-time migration path or simple script converters to map `@Step` to `@TestStep` seamlessly.
- **Risk**: Disk space bloat due to hundreds of runs capturing high-resolution PNGs.
- **Mitigation**: Introduce a background scavenger policy in Aura Server to automatically compress, downscale, or purge screenshots older than `X` days, while retaining historical execution JSON metadata for long-term trend analysis.


## Context

Current test runners (such as Neodymium) write reporting metadata directly into Allure-compatible formats or inline HTML summaries. When dynamic processes like AI self-healing or transient retries occur, Allure's thread-local lifecycle is extremely difficult to interact with, forcing fragile reflection hacks on internal classes. 

To overcome this, **Aura** decouples **execution capture** (saving raw, version-controllable JSON/PNG directories to the local disk) from **indexing & display** (managed by a stateful local Java server). When the server starts up, it reads the external directory, builds an optimized relational database index, and delivers a premium interactive console. During active execution, the runner can redirect its output stream directly to the server for real-time console feedback.

## Goals / Non-Goals

**Goals:**
- **Open-File Source of Truth**: Keep execution history, step timelines, playbooks, and screenshots in flat, highly inspectable directories on disk.
- **Ultra-Fast Local Index**: Scan and index hundreds of runs in under a second on startup using H2 or SQLite.
- **Interactive Visual Lab**: Provide side-by-side swipe sliders for visual mismatches, complete with a "manual baseline approval" engine.
- **AI Diagnostics Workspace**: Integrate LangChain4j (Gemini) to inspect step failures, DOMs, and logs directly from the UI.
- **Zero-Config Developer Flow**: Boot the server on `localhost:8080` with a zero-dependencies embedded database.

**Non-Goals:**
- Forcing a persistent cloud database; the system is designed to run locally, offline, and self-contained on the developer's machine or a local CI agent.
- Modifying standard JUnit test dispatch mechanisms.

## Decisions

### 1. Complete Ownership of Test Lifecycle (Zero-Allure Dependency)
- **Decision**: Eliminate all `io.qameta.allure` annotations (`@Step`, `@Attachment`, etc.) and runtime libraries. Introduce native Neodymium metadata annotations (e.g. `@TestStep`, `@VisualAssertion`, `@ExpectedFailure`) and implement a native `AuraCaptureListener` hooking directly into JUnit 5/Selenide execution.
- **Alternative**: Keep using Allure listeners and translate their step formats into Aura files, or use reflection to capture lifecycle hooks.
- **Rationale**: Relying on Allure binds the framework to heavy external dependencies, rigid execution models (unable to cleanly represent dynamic AI paths, self-healing retries, or perceptual hashes), and static single-thread context limitations. By owning the lifecycle capture, we achieve absolute design flexibility, maximum runtime execution speed, and zero external reporting bloat.

### 2. Unified Directory Layout
- **Decision**: All results are stored in `target/aura-results/run_<timestamp>_<branch>_<commit>/` as flat files:
  - `run-info.json`: Execution summary, branch/commit SHA, browser environment.
  - `test-cases/<className>_<methodName>.json`: Structured chronological step results, visual hashes, and locator performance.
  - `screenshots/*.png`: Visual snapshots.
  - `logs/<className>_ai.json`: LLM prompts, reasoning, and DOM extracts.
- **Rationale**: Keeps data fully open, transparent, easy to clean up, and highly version-controllable.

### 3. Spring Boot + H2 Speed Index Layer
- **Decision**: Built on **Spring Boot 3.x**, **JDK 21 Virtual Threads**, and **H2/SQLite**. On boot, an `AuraFileIngester` scans the results directory, compares file signatures, and bulk-inserts/updates runs in a lightweight database.
- **Rationale**: Keeps UI operations (searching, trend graphing, filtering by flakiness) extremely fast. Rebuilding the database takes seconds if files are deleted or modified.

### 4. Live Stream Redirection Pipe
- **Decision**: An `AuraLiveStreamAppender` in Neodymium checks if `localhost:8080` is reachable. If so, it stream-redirects JSON event packets (e.g. `testStarted`, `stepCompleted`, `screenshotCaptured`) over a WebSocket or standard port pipe to the running Aura receiver.
- **Rationale**: Provides immediate, real-time feedback during execution in a unified live console, while ensuring the definitive results are still written to disk at the end of the run.

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


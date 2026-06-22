# Tasks

## Component: Aura Server Setup
- [ ] Initialize Spring Boot 1.5.x (Spring 4.x) / Java 21 subproject `/aura-server`
- [ ] Configure zero-dependency H2/SQLite embedded database
- [ ] Implement JPA entity mappings for `Runs`, `TestCases`, `Steps`, and `VisualBaselines`

## Component: Neodymium Native Lifecycle Capture (Neo AI Extension & Allure Coexistence)
- [ ] Ensure `io.qameta.allure` Maven dependencies remain intact in `pom.xml` for Neo Classic compatibility
- [ ] Implement native Neodymium annotations (e.g., `@TestStep`, `@VisualAssertion`, `@ExpectedFailure`) for Neo AI test runs, coexisting side-by-side with Allure annotations
- [ ] Build a native `AuraCaptureListener` that hooks directly into JUnit 5 and Selenide logging specifically during Neo AI executions (without interfering with Neo Classic runs)
- [ ] Implement step context serialization in Aura capture to cleanly support dynamic steps, self-healing retries, and visual hashes
- [ ] Implement DOM snapshot capture (extracting outerHTML via JavaScript) specifically for Neo AI execution steps
- [ ] Implement CDP network request interceptor matching requests/responses and slicing them into Neo AI step trace buffers
- [ ] Implement CDP console log collector capturing browser console errors, warnings, and messages specifically for Neo AI execution steps

## Component: File System Reader & Ingester (Direct Disk Writing)
- [ ] Define standardized JSON schemas for `run-info.json` and `<testCase>.json`
- [ ] Document the schema layout publicly to allow third-party runners to write directly in our format
- [ ] Create `AuraFileIngester` service to scan directories on startup
- [ ] Implement file signature hashing to skip already-indexed run directories
- [ ] Write integration tests for bulk database import of historical run folders

## Component: Open API Ingestion & Live Stream (Centralized Writing)
- [ ] Build `/api/v1/ingest` POST HTTP endpoint in Aura Server to receive generic run/step/screenshot payloads
- [ ] Create `AuraDiskWriterService` in Aura Server to serialize incoming API payloads directly to the flat-file directory structure
- [ ] Build a WebSocket event endpoint in Aura Server (`/api/v1/stream`) for active live-streaming
- [ ] Create `AuraLiveStreamAppender` in Neodymium to stream JSON event packets asynchronously
- [ ] Implement grace-handling so Neodymium skips streaming if Aura Server is offline
- [ ] Write integration tests verifying that API-ingested test runs successfully serialize to disk and populate the database


## Component: Thymeleaf & HTMX Dashboard
- [ ] Build dashboard home with Chart.js (failure trends, duration stats, Git-commit timeline)
- [ ] Implement live console view updating step progress in real-time via HTMX WebSockets
- [ ] Create **Visual Regression Lab** with interactive before/after horizontal swipe sliders
- [ ] Implement "Approve Baseline" controller that overwrites disk baselines and updates visual hashes
- [ ] Implement **Trace Viewer workspace** inside the dashboard featuring a split-pane layout (left: chronological steps list, right: tabbed diagnostic details)
- [ ] Implement interactive sandboxed `<iframe>` to render historical DOM snapshots at each step
- [ ] Build interactive Network Request tables with search/filter, displaying request/response parameters
- [ ] Build a Console tab reproducing browser console logs color-coded by log level (Error, Warn, Info)

## Component: AI Failure Analyst
- [ ] Integrate LangChain4j and configure connection to Gemini (using env keys)
- [ ] Implement failure analysis endpoint (sending DOM context + stack trace to LLM)
- [ ] Create a conversational chat panel inside the run inspector UI

## Component: Aura Static HTML Generator
- [ ] Create a static compiler utility (`AuraStaticGenerator`) in Java
- [ ] Implement asset bundling (copying vanilla CSS/JS and linked screenshot PNG files into an target directory)
- [ ] Implement FreeMarker/Thymeleaf-based static rendering to output self-contained `index.html` and static run pages
- [ ] Provide command-line triggers to export static snapshots from any local `target/aura-results` folder

## Component: Aura Quality Gates
- [ ] Define the JSON schema for `quality-gates.json` declarative rules
- [ ] Implement the `QualityGateEvaluator` service in Aura Server evaluating Failure Rate %, Critical Component success, and New Failure Regressions
- [ ] Create the `/api/v1/runs/{id}/evaluate` REST endpoint returning unified pass/fail verdicts and breach logs
- [ ] Build a simple quality gate configuration interface in the Thymeleaf dashboard

## Component: Verification & Hardening
- [ ] Write comprehensive unit tests for ingestion heuristics
- [ ] Conduct end-to-end execution verification with posters-demo-store test suite
- [ ] Validate generated offline static HTML files for proper navigation, local styling, and visual asset linking
- [ ] Write integration tests for Quality Gate evaluation, verifying both successful and breached gates under different criteria
- [ ] Create a comprehensive README and startup scripts




# Tasks

## Component: Aura Server Setup
- [ ] Initialize Spring Boot 3.x / Java 21 subproject `/aura-server`
- [ ] Configure zero-dependency H2/SQLite embedded database
- [ ] Implement JPA entity mappings for `Runs`, `TestCases`, `Steps`, and `VisualBaselines`

## Component: Neodymium Native Lifecycle Capture (Allure Elimination)
- [ ] Remove `io.qameta.allure` Maven dependencies from `pom.xml`
- [ ] Implement native Neodymium annotations (e.g. `@TestStep`, `@VisualAssertion`, `@ExpectedFailure`)
- [ ] Build a native `AuraCaptureListener` that hooks directly into JUnit 5 and Selenide logging
- [ ] Implement step context serialization to cleanly support dynamic steps, self-healing retries, and visual hashes

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

## Component: AI Failure Analyst
- [ ] Integrate LangChain4j and configure connection to Gemini (using env keys)
- [ ] Implement failure analysis endpoint (sending DOM context + stack trace to LLM)
- [ ] Create a conversational chat panel inside the run inspector UI

## Component: Verification & Hardening
- [ ] Write comprehensive unit tests for ingestion heuristics
- [ ] Conduct end-to-end execution verification with posters-demo-store test suite
- [ ] Create a comprehensive README and startup scripts


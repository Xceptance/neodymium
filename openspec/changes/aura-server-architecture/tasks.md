# Tasks

## Component: Aura Server Setup
- [ ] Initialize Spring Boot 3.x / Java 21 subproject `/aura-server`
- [ ] Configure zero-dependency H2/SQLite embedded database
- [ ] Implement JPA entity mappings for `Runs`, `TestCases`, `Steps`, and `VisualBaselines`

## Component: File System Reader & Ingester
- [ ] Define standardized JSON schemas for `run-info.json` and `<testCase>.json`
- [ ] Create `AuraFileIngester` service to scan directories on startup
- [ ] Implement file signature hashing to skip already-indexed run directories
- [ ] Write integration tests for bulk database import of historical run folders

## Component: Live Redirection Stream
- [ ] Build a WebSocket event endpoint in Aura Server (`/api/live-stream`)
- [ ] Create `AuraLiveStreamAppender` in Neodymium to stream JSON event packets asynchronously
- [ ] Implement grace-handling so Neodymium skips streaming if Aura Server is offline

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

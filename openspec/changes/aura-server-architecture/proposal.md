## Why

Traditional test reporting frameworks, such as Allure, are highly static, monolithic, and dated. They fail to support modern, advanced automation requirements, particularly around:
1. **AI/LLM-Driven Execution**: There is no native support for logging LLM reasoning history, system context levels (Lean vs. Visual), self-healing path logs, or token usage.
2. **Visual Regression Diagnostics**: Screenshots are basic attachments; they lack interactive before/after side-by-side split sliders or perceptual dHash differences.
3. **Stateful Analytics & Trends**: Static HTML reports cannot track historical test duration trends, branch regressions, or auto-detect flakiness across commits.
4. **Offline Distribution Barriers**: Allure requires a local web server (CORS restrictions) to view compiled reports, making sharing difficult.

By introducing **Aura Server** as a standalone Java web application running on `localhost:8080`, we decouple raw test execution from database indexing and visualization. Tests run extremely fast and write results to raw, open, standardized JSON files on disk. The Aura Server then ingests this file system, acting as an optimized query/indexing engine and providing a gorgeous, stateful, and AI-enabled dashboard.

## What Changes

1. **Aura Server (New App)**: Create a lightweight Spring Boot 3.x/Java 21 standalone application. On startup, it scans a configured external results directory to parse and index test run history into a local H2/SQLite database.
2. **Dynamic Ingestion**: When runs are ongoing, the server receives a redirected live-event stream from the test execution's "writer", rendering real-time execution states via WebSockets/HTMX.
3. **Interactive Control Panel**: A premium, responsive, glassmorphic Thymeleaf dashboard for:
   - Run history and failure trends.
   - Perceptual visual regression lab with side-by-side swipe sliders and manual baseline approval buttons.
   - Conversational AI Diagnostic workspace driven by LangChain4j (Gemini).
4. **Neodymium Aura Collector**: Build a lightweight listener in Neodymium that writes execution results to standard external JSON/PNG structures on disk and supports optional port-redirection streams.

## Capabilities

### New Capabilities

- `aura-server`: Statefully index test histories, visual regression baselines, AI healing pipelines, and flakiness heuristics.
- `aura-live-stream`: Real-time streaming tap for ongoing test suites to receive updates dynamically in the dashboard.

### Modified Capabilities

- `reporting`: Transition from static Allure report XML/JSON outputs to standardized Aura external file bundles (`target/aura-results/`).
- `test-management`: Connect automated test suite runs statefully with the manual test cases backlog.

## Impact

- **New Subproject**: `aura-server/` (Spring Boot project, Thymeleaf/HTMX UI, H2 database, LangChain4j integration).
- **Test Automation Integration**: Integrate the `AuraRunListener` in Neodymium (`neodymium-library`) to produce standardized results and stream live updates if the Aura Server is online.

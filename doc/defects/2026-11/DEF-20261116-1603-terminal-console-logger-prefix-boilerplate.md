# [DEF-20261116-1603] Terminal Console View Displays Full Logger Layout Boilerplate In AI Only Mode

- **Status:** `Resolved`
- **Opened:** 2026-11-16 15:55
- **Closed:** 2026-11-16 16:03
- **Component:** `aura-manager`, `neodymium-core`, `neodymium-archetype`
- **Scope:** `Framework & UI`
- **Symptom:** In the terminal console view of Aura Manager and Neodymium Dashboard, AI log lines display verbose framework logger boilerplate prefixes (e.g. `15:20:49.742 [main] INFO org.neodymium.ai.pipeline.steps.AgentToolLoopStep -- `) on every line instead of clean timestamps and action text.
- **Root Cause:** 
  1. `appendLogsBatch` in `dashboard-runner.js` rendered raw stdout strings into the DOM without stripping standard logger headers.
  2. While `neodymium-core` configured a clean `%d{HH:mm:ss} %m%n` pattern for its internal `AI_STDOUT` appender, downstream test projects (such as `archy_test` and `neodymium-archetype`) defaulted to `<PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>` for all loggers, adding thread, level, class name, and separator delimiters (`--` or `-`) to every AI message.
- **Detection Gap ("What did we miss?"):** `StaticResourceSyntaxTest` evaluated `isAiLogLine` against synthetic clean lines (e.g. `13:30:01 🤖 Agent Turn #1...`) rather than real-world stdout containing full thread, logger, and level prefixes.
- **Resolution:**
  1. Implemented `formatLogLineForDisplay(line)` in `dashboard-runner.js` (`aura-manager` and `neodymium-core`) matching `^(\d{2}:\d{2}:\d{2})(?:\.\d+)?(?:\s+\[.*?\])?\s+(?:TRACE|DEBUG|INFO|WARN|ERROR|FATAL)(?:\s+[\w\.\$]+)?(?:\s+--?|\s*:)\s+(.*)$` to strip logger boilerplate down to clean timestamps while preserving indentation on continuation lines.
  2. Integrated `formatLogLineForDisplay` into `appendLogsBatch` and `appendLog` display pipelines, and updated pattern cleaning in `isAiLogLine`.
  3. Added dedicated `AiConsole` appender and `org.neodymium.ai` logger block in `archy_test/src/test/resources/log4j2.xml`.
- **Safety Net Added:** Automated regression assertions in `StaticResourceSyntaxTest#testAiLogFilteringSuppressesWireLogsAndMavenNoise` verifying `formatLogLineForDisplay()` against full double-dash (`--`), single-dash (`-`), and colon (`:`) real-world logger prefixes, as well as already clean and indented lines.

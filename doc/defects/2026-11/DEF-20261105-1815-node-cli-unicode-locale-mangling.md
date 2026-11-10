# [DEF-20261105-1815] Node CLI Test Argument Locale Mangling Under POSIX Non-UTF8 Locales

- **Status:** `Resolved`
- **Opened:** 2026-11-05 18:15
- **Closed:** 2026-11-06 05:27
- **Component:** `aura-manager`
- **Scope:** `Test/Harness & Config/Environment`
- **Symptom:** When running Maven test suites in environments where POSIX locale specifies ASCII (`LANG=C` or `LC_ALL=C`), 3 test methods in `StaticResourceSyntaxTest` failed:
  1. `testAccumulatedChatMessageHistoryParsingAndDomCollapsing`: Plain text tool error summary mismatch expecting `generateAndUploadImage ➔ ERROR:` but received `generateAndUploadImage ? ERROR:`.
  2. `testDashboardRunnerAiLogFilter`: Failed asserting that ASCII box-drawing characters `╔════...` matched the AI log filter pattern because the line received by Node was mangled to `13:30:00 ?????????????????????????????????????????????????????????????????????????????????????`.
  3. `testMultiTurnToolOutputParsingAndClipboardExport`: `turnInputSummary failed: Tool Result: query_dom ? SUCCESS (1 match: app-image-placeholder.hoveredPlaceholder)`.
- **Root Cause:** `StaticResourceSyntaxTest` passed inline JavaScript test scripts to Node via `new ProcessBuilder("node", "-e", nodeTestScript)`. In OpenJDK on POSIX systems, `ProcessImpl` encodes `String` command-line arguments to native byte arrays using the JVM's `sun.jnu.encoding`. Under `LANG=C` or `LC_ALL=C` (typical of minimal container environments or default CI runners), `sun.jnu.encoding` defaults to US-ASCII. All multibyte Unicode symbols (`╔`, `═`, `➔`, `🚀`) inside `nodeTestScript` were downcast to ASCII `?` (0x3F) before Node.js started. Meanwhile, `report-manager.js` (loaded from disk via UTF-8 `fs.readFileSync`) produced true UTF-8 `➔` (`\u2794`), causing string equality and regex assertions to fail.
- **Detection Gap ("What did we miss?"):** Tests were previously executed on developer workstations with `LANG=en_US.UTF-8`, where `sun.jnu.encoding` is UTF-8, masking the JVM CLI argument downcasting behavior.
- **Resolution:**
  1. Implemented a centralized, robust helper `runNodeScript(nodeTestScript, failureMessagePrefix)` in `StaticResourceSyntaxTest.java`.
  2. The script is written to a temporary `.js` file explicitly encoded in `StandardCharsets.UTF_8` inside `target/temp-node-tests/` (falling back safely to `java.io.tmpdir` if `target/` is not writable). ECMAScript specification § 10.1 guarantees that V8/Node.js decodes source files as UTF-8, completely bypassing OS locale settings and JVM `sun.jnu.encoding`.
  3. Standard output and error are redirected to an OS-level temporary log file (`pb.redirectOutput(tempOutput)`), eliminating the 64 KB OS kernel pipe buffer limit and preventing process deadlocks without consuming thread pool resources.
  4. Added a 30-second execution timeout with forced process termination and clean exception handling in nested `try ... finally` blocks to prevent Windows file-sharing locks from masking primary assertion failures.
  5. Centralized Node availability detection with thread-safe cached double-checked locking, a 10s timeout, and `/dev/null` output redirection.
  6. Added a 10s timeout guard to `verifySyntaxWithNode` to eliminate infinite hangs.
  7. Refactored all 7 Node script execution tests to delegate to `runNodeScript`.
- **Safety Net Added:** Verified via `LANG=C LC_ALL=C mvn test -pl aura-manager -Dtest=StaticResourceSyntaxTest` (17/17 tests passing in 0.78s) and confirmed across the entire module via `LANG=C LC_ALL=C mvn test -pl aura-manager -Pnon-llm-tests` (207/207 tests passing with 0 failures, 0 errors).

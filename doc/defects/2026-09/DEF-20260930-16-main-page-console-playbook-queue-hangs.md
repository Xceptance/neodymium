# [DEF-20260930-16] Main Page Console (Playbook & Queue) Hangs During Test Execution Under High Log Volume

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `aura-manager` (`dashboard-runner.js`, `dashboard-styles.css`)
- **Scope:** `Framework`
- **Symptom:** The aura-manager main UI page (playbook & queue) freezes/hangs during test execution when large amounts of stdout/console logs are printed by the test process.
- **Root Cause:** `dashboard-runner.js` invoked `localStorage.setItem('aura_previous_console_logs', terminalConsole.innerHTML)` synchronously on every single streamed log line. As log output grew to thousands of lines, writing multi-megabyte HTML strings synchronously to `localStorage` on the main JS thread dozens/hundreds of times per second blocked the browser event loop. Additionally, per-line unbatched DOM appends (`insertAdjacentHTML`) and layout queries (`innerText`) caused severe browser layout thrashing.
- **Detection Gap ("What did we miss?"):** UI tests did not run stress tests with high-frequency console output streams to measure browser event-loop latency and DOM reflow overhead.
- **Resolution:** Replaced per-line synchronous `localStorage` writes with debounced persistence (`debouncedSaveConsoleLogs`, throttled to 1 second), batched incoming log lines into single-pass DOM HTML appends (`appendLogsBatch`) per polling tick, replaced reflow-triggering `innerText` with `textContent` in filter updates, and added CSS layout containment (`contain: content`) to `#terminalConsole`—preserving 100% of all log lines without truncating output.
- **Safety Net Added:** Updated `dashboard-runner.js` and `dashboard-styles.css` with batch DOM appends, debounced persistence, and `textContent` filtering.

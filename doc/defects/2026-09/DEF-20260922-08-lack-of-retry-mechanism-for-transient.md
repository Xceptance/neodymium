# [DEF-20260922-08] Lack of Retry Mechanism for Transient SessionNotCreatedException During WebDriver Startup

- **Status:** Resolved
- **Opened:** 2026-09-22
- **Closed:** 2026-09-22
- **Component:** `neodymium-core` (`BrowserRunnerHelper`, `NeodymiumConfiguration`)
- **Scope:** `Framework`
- **Symptom:** Tests fail immediately during setup when ChromeDriver or the browser is temporarily unreachable (`org.openqa.selenium.SessionNotCreatedException: Could not start a new session. Response code 500. Message: session not created from chrome not reachable`).
- **Root Cause:** Transient OS process/socket collisions (e.g. DevTools port lingering in TIME_WAIT or Chrome shutdown latency from a previous test) cause ChromeDriver handshake to fail. Neodymium previously lacked a retry loop for driver session instantiation, treating all `SessionNotCreatedException` failures as fatal.
- **Detection Gap ("What did we miss?"):** No test harness resilience for transient process startup race conditions; browser creation assumed 100% determinism.
- **Resolution:** Wrapped `createWebDriverStateContainer` with a single-retry resilience loop with configurable backoff (default 2–10 seconds jitter), ensured automatic cleanup of partial resources (e.g. embedded proxies) before retry, and refreshed remote debugging port probes on retry.
- **Safety Net Added:** `BrowserRunnerHelperTest` validating single retry with backoff, abort behavior on subsequent failure, non-retry for other exceptions, and proxy leak prevention.

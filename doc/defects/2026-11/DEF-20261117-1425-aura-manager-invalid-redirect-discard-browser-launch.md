# [DEF-20261117-1425] Aura Manager Fails to Open Browser on Start Due to Invalid Input Redirect

- **Status:** `Resolved`
- **Opened:** 2026-11-17 14:25
- **Closed:** 2026-11-17 14:30
- **Component:** `aura-manager`
- **Scope:** `Framework & Config/Environment`
- **Symptom:** Starting Neodymium AI / Aura Manager server fails to open a browser window or tab automatically. The console reports `Could not open browser automatically: Redirect invalid for reading: WRITE`.
- **Root Cause:** Commit `0655401ab` (GitLab #133 / PR #540 / DEF-20261116-1147) configured `.redirectInput(ProcessBuilder.Redirect.DISCARD)` on all `ProcessBuilder` browser launch pipelines (Linux setsid, Linux gio, Linux fallback xdg-open, macOS open, Windows rundll32). In Java (Java 9+), `ProcessBuilder.Redirect.DISCARD` has `Type.WRITE`. Passing it to `redirectInput(Redirect)` immediately throws `java.lang.IllegalArgumentException: Redirect invalid for reading: WRITE`. Because `IllegalArgumentException` is an unchecked `RuntimeException`, it bypassed the inner `catch (final IOException ignored)` handlers in `openLinuxBrowserDetached` and bubbled to the outer catch in `openBrowser`, completely aborting browser launching across all platforms.
- **Detection Gap ("What did we miss?"):** `AiLauncherTest.testOpenBrowserDoesNotThrowWhenDisabled` only exercised `openBrowser()` when browser auto-launch was disabled (`neodymium.ai.openBrowser=false`), which short-circuited on `isBrowserOpenEnabled()` and never executed any `ProcessBuilder` pipeline.
- **Resolution:**
  1. Removed `.redirectInput(ProcessBuilder.Redirect.DISCARD)` from all `ProcessBuilder` invocations in `AiLauncher.java` across macOS, Windows, and Linux launch pipelines.
  2. Widened fallback exception handling in `openLinuxBrowserDetached` to catch `Exception` so unexpected runtime errors fall through gracefully to subsequent launch strategies.
  3. Linked to GitLab ticket #145 (`neo-aura-manager-fails-to-open-browser-due-to-invalid-redirect`).
- **Safety Net Added:** Added unit tests `testOpenBrowserDoesNotThrowWhenEnabled` and `testOpenLinuxBrowserDetachedDoesNotThrowIllegalArgumentException` in `AiLauncherTest.java`.

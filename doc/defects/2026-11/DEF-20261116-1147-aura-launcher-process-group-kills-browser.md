# [DEF-20261116-1147] Aura/AI Manager Server Shutdown Kills Browser via Shared Terminal Process Group

- **Status:** `Resolved`
- **Opened:** 2026-11-16 11:47
- **Closed:** 2026-11-16 11:55
- **Component:** `aura-manager`
- **Scope:** `Framework & Config/Environment`
- **Symptom:** Terminating the Neodymium AI/Aura Manager server (`run-ai.sh` or `run-aura.sh` via `Ctrl+C` in a terminal) unexpectedly terminates the user's entire web browser process and closes all open windows and unrelated browser tabs.
- **Root Cause:** When `AiLauncher.openBrowser()` (and legacy `AuraLauncher.openBrowser()`) launched the browser via `Desktop.getDesktop().browse()` or `Runtime.getRuntime().exec(new String[]{"xdg-open", url})` on Linux, the spawned browser process was created within the foreground terminal session's process group (PGID) without session detachment. When a developer stops the manager via `Ctrl+C`, the Linux TTY line discipline broadcasts `SIGINT` (signal 2) across the entire foreground process group (`kill -SIGINT -<PGID>`). The browser process receives `SIGINT` directly and terminates. Furthermore, the web application lacked a top-level global offline banner to inform users that the server had been stopped or was unreachable.
- **Detection Gap ("What did we miss?"):** Prior manual and automated tests ran in desktop environments where an existing browser instance was already running in the background. In that state, `xdg-open` merely sent an IPC message via D-Bus / local socket to the pre-existing browser daemon and exited immediately, hiding the fact that freshly spawned browser processes inherit the controlling terminal's process group.
- **Resolution:**
  1. Updated `AiLauncher.openBrowser()` on Linux/Unix systems to launch detached from the terminal session via `setsid` using `ProcessBuilder` with discarded I/O streams (`Redirect.DISCARD`), ensuring `SIGINT` sent to the terminal remains confined to Maven/Spring Boot.
  2. Added property `-Dneodymium.ai.openBrowser=false` (and legacy alias `-Dneodymium.aura.openBrowser=false`, plus env vars `AURA_NO_BROWSER` / `NEODYMIUM_AI_NO_BROWSER`) allowing developers and automated environments to suppress auto-launching the browser.
  3. Added a global `#serverOfflineBanner` in `index.html` with styles in `dashboard-styles.css` and reconnect logic in `dashboard-runner.js`. The banner appears after consecutive heartbeat failures or network disconnection and automatically dismisses once the server resumes.
- **Safety Net Added:** Added unit tests `AiLauncherTest.testIsBrowserOpenEnabledHonorsSystemProperty`, `testIsBrowserOpenEnabledHonorsLegacyProperty`, `testIsBrowserOpenEnabledDefaultIsTrue`, and `testOpenBrowserDoesNotThrowWhenDisabled` in `AiLauncherTest.java`, along with syntax checks in `StaticResourceSyntaxTest.java`.

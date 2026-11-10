# [DEF-20261001-07b] TakeScreenshotsThread Throws NullPointerException When Driver Is Not Yet Initialized at Thread Start

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`TakeScreenshotsThread.java`, `FilmTestExecution.java`)
- **Scope:** `Framework`
- **Symptom:** AI/Aura test runs with video recording enabled failed to produce video files when the browser driver was opened dynamically after test initialization.
- **Root Cause:** TakeScreenshotsThread bound to a fixed WebDriver instance passed at constructor time during executeBeforeEach before Selenide/Neodymium opened the browser. Attempting to take screenshots against a null driver threw NullPointerException and aborted video creation.
- **Detection Gap ("What did we miss?"):** Unit tests for TakeScreenshotsThread passed a pre-initialized mock WebDriver, missing the asynchronous driver initialization lifecycle in AI runners.
- **Resolution:** Updated TakeScreenshotsThread with dynamic driver resolution (getActiveDriver()) that gracefully polls until Neodymium or Selenide starts the WebDriver instance before taking screenshots.
- **Safety Net Added:** Added unit test coverage verifying dynamic driver resolution when started prior to browser opening.

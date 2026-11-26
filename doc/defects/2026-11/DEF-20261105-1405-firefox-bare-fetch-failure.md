# [DEF-20261105-1405] Firefox Bare Fetch Invocation Failure in DomQuiescenceWatcher

- **Status:** `Resolved`
- **Opened:** 2026-11-05 14:05
- **Closed:** 2026-11-05 14:12
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** AI tests running in Firefox fail with `TypeError: 'fetch' called on an object that does not implement interface Window` during network activity tracking.
- **Root Cause:** In `DomQuiescenceWatcher.java`, both `INSTALL_TRACKER_SCRIPT` (line 56) and `WAIT_FOR_QUIET_SCRIPT` (line 92) proxied native network calls via `origFetch.apply(this, arguments)`. In JavaScript strict mode or module contexts where `fetch(...)` is called bare without an explicit receiver, `this` is `undefined`. Unlike V8 (Chrome), SpiderMonkey (Firefox) requires `this` for native `Window.prototype.fetch` to implement the `Window` interface, causing an immediate runtime `TypeError`.
- **Detection Gap ("What did we miss?"):** Prior unit tests in `DomQuiescenceWatcherTest` only verified execution without an active browser session (`WebDriverRunner.hasWebDriverStarted() == false`) or reflection contracts, never executing the embedded JavaScript strings inside a real Gecko runtime.
- **Resolution:** Changed `origFetch.apply(this, arguments)` to `origFetch.apply(window, arguments)` across both tracker injection scripts in `DomQuiescenceWatcher.java`. In addition, updated `DomQuiescenceWatcherTest` to deduplicate test logic and guarantee restoration of `Configuration.headless` alongside `Configuration.browser` in `@AfterEach tearDown()`.
- **Safety Net Added:** `DomQuiescenceWatcherTest.testBareFetchCallAfterInstallingTracker` and `DomQuiescenceWatcherTest.testBareFetchCallAfterWaitForDomQuiet`.

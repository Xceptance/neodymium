# [DEF-20261021-1050] Only Use Browser Driver Executable When Explicitly Configured

- **Status:** `Resolved`
- **Opened:** 2026-10-21 09:17
- **Closed:** 2026-10-21 10:50
- **Component:** `neodymium-core`
- **Scope:** `Framework & Infra/Build`
- **Symptom:** When starting local browser sessions (Chrome, Firefox, Edge, IE), tests fail during session startup with:
  `org.openqa.selenium.SessionNotCreatedException: Could not start a new session. Response code 500. Message: session not created: This version of ChromeDriver only supports Chrome version <X>`
  This occurred even though the host machine had a newer browser version (e.g. Chrome 156) and modern Selenium Manager would have normally downloaded and managed the matching driver automatically.
- **Root Cause:** In `BrowserRunnerHelper`, Neodymium eagerly probed the operating system `$PATH` via `new ExecutableFinder().find("<driver>")` (e.g. `chromedriver`, `geckodriver`, `msedgedriver`, `IEDriverServer`). When an outdated system package (e.g. `/usr/bin/chromedriver` from `chromium-chromedriver` at v154) was present on `$PATH`, Neodymium unconditionally invoked `usingDriverExecutable(new File(driverInPathPath))` on the driver service builder. In Selenium 4.6+, explicitly setting a driver executable disables Selenium's built-in Selenium Manager fallback and forces Selenium to execute the specified driver binary, causing session failure if the system driver is older than the browser.
- **Detection Gap ("What did we miss?"):** Existing browser initialization tests executed in environments where the system `$PATH` driver matched the installed browser or where driver paths were stubbed, never verifying behavior when a stale driver binary exists on the system `$PATH` alongside a newer browser.
- **Resolution:**
  - Extracted package-private builder creation methods in `BrowserRunnerHelper` (`createChromeBuilder`, `createGeckoBuilder`, `createIEBuilder`, `createEdgeBuilder`).
  - Removed eager `ExecutableFinder` lookups on system `$PATH` for all driver binaries.
  - Configured builders to ONLY invoke `usingDriverExecutable(...)` if a custom driver path is explicitly configured via `NeodymiumConfiguration` (e.g. `neodymium.webDriver.<browser>.pathToDriverServer`).
  - Left `driverServiceBuilder.exe` as `null` by default so Selenium 4's built-in Selenium Manager seamlessly resolves and downloads the exact driver version required for the installed browser.
- **Safety Net Added:** Added 8 regression unit tests in `BrowserRunnerHelperTest`:
  - `testCreateChromeBuilderWithoutConfiguredDriverPathLeavesExecutableNullForSeleniumManager`
  - `testCreateChromeBuilderWithConfiguredDriverPathSetsExecutable`
  - `testCreateGeckoBuilderWithoutConfiguredDriverPathLeavesExecutableNullForSeleniumManager`
  - `testCreateGeckoBuilderWithConfiguredDriverPathSetsExecutable`
  - `testCreateEdgeBuilderWithoutConfiguredDriverPathLeavesExecutableNullForSeleniumManager`
  - `testCreateEdgeBuilderWithConfiguredDriverPathSetsExecutable`
  - `testCreateIEBuilderWithoutConfiguredDriverPathLeavesExecutableNullForSeleniumManager`
  - `testCreateIEBuilderWithConfiguredDriverPathSetsExecutable`

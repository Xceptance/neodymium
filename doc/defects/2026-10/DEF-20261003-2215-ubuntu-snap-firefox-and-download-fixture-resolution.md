# [DEF-20261003-2215] Ubuntu Snap Firefox Wrapper, Geckodriver Exit 64, and Download Test Fixture Resolution

- **Status:** `Resolved`
- **Opened:** 2026-10-03 21:55
- **Closed:** 2026-10-03 22:14
- **Component:** `neodymium-core`
- **Scope:** `Framework & Test/Harness & Config/Environment`
- **Symptom:** 
  1. `DownloadFilesExecutorTest` failed on form upload with `IllegalArgumentException: File not found: src/test/resources/xceptance_bugs.png`.
  2. `DownloadFilesExecutorTest` failed when starting `firefox_download` with `SessionNotCreatedException: Driver server process died prematurely, exit value: 64` or `SessionNotCreatedException: Response code 400. Message: binary is not a Firefox executable`.
- **Root Cause:** 
  1. `DownloadFilesInDifferentWays` used a relative filesystem path (`new File("src/test/resources/xceptance_bugs.png")`). In the multi-module Maven reactor, Surefire executes from the root directory (`${session.executionRootDirectory}`), where `src/test/resources` no longer resides at root level after module separation.
  2. `DownloadFilesExecutorTest` configured `properties.put("browserprofile.firefox_download.arguments", "--headless=new")`. `--headless=new` is a Chrome 109+ argument; Firefox does not recognize `--headless=new` as headless mode and attempts to start a GUI window navigating to a page named `--headless=new`. Without an X11 display, startup fails.
  3. `BrowserRunnerHelper` resolved `geckodriver` via `new ExecutableFinder().find("geckodriver")` which on Ubuntu systems resolved to `/snap/bin/geckodriver`. `/snap/bin/geckodriver` is a symlink to `/usr/bin/snap`, whose argument parser rejects Selenium's `--port` flag with `error: unknown flag 'port'` (exit code 64). Furthermore, `BrowserRunnerHelper` did not check `Neodymium.configuration().getFirefoxDriverPath()`.
  4. `BrowserRunnerHelper` resolved `firefox` via `new ExecutableFinder().find("firefox")` which on Ubuntu systems resolved to `/usr/bin/firefox`, a transitional shell script wrapper (`#!/bin/sh`). Geckodriver rejects shell scripts as not valid binary executables.
- **Detection Gap ("What did we miss?"):** Prior testing was conducted on macOS or non-snap Linux environments where Firefox is installed as a direct ELF executable in `/Applications` or `/usr/lib`, and tests were executed directly in single-module working directories.
- **Resolution:** 
  1. Converted `DownloadFilesInDifferentWays` (JUnit 4 and JUnit 5) to use Selenide's `uploadFromClasspath("xceptance_bugs.png")`, resolving the fixture from the test classpath (`target/test-classes/`).
  2. Converted `DownloadFilesExecutorTest` (JUnit 4 and JUnit 5) to configure `properties.put("browserprofile.firefox_download.headless", "true")`.
  3. Hardened `BrowserRunnerHelper` to check `Neodymium.configuration().getFirefoxDriverPath()` first and ignore PATH binaries under `/snap/bin/` so that Selenium Manager handles clean driver provisioning.
  4. Hardened `BrowserRunnerHelper` to detect the true ELF binary at `/snap/firefox/current/usr/lib/firefox/firefox` when running on Ubuntu snap systems.
- **Safety Net Added:** Verified via both `com.xceptance.neodymium.junit5.tests.DownloadFilesExecutorTest` and `com.xceptance.neodymium.junit4.tests.DownloadFilesExecutorTest` with all 6 executions passing across Chrome and Firefox.

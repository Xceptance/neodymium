## Why

Developers and QA engineers running automated tests need a quick, highly readable, and lightweight summary of the test run at their fingertips immediately after the test run finishes. 
Currently, checking test results requires either scrolling through extensive and noisy console logs, or using heavy reporting frameworks (like Allure) which must be compiled/opened in a browser. 
Generating a clean, concise, human-readable report at the end of the test run provides instant visibility into which tests passed or failed, what errors occurred, the test run metadata, and critical trace details (including step logs, browser console logs, and failure screenshots).
Furthermore, to pave the way for integration with future reporting platforms (specifically **Aura**), the same structured test execution data must be written in a standard, machine-readable JSON format. This serves as a rapid developer-focused feedback tool and a foundational data producer for subsequent reporting pipelines, rather than a full replacement for heavyweight reporting suites.

## What Changes

We will introduce a new lightweight reporting capability:
- **Test Run Collector**: A centralized, thread-safe test run observer that gathers start/end events, test status, execution duration, step traces, browser logs, screenshots, and failure exceptions across all test threads.
- **Triple Output Formats (HTML, Markdown, and JSON)**: The reporting engine will generate three formats simultaneously:
  - `.md` version: Ideal for CI pipeline consoles and text-based terminals.
  - `.html` version: Self-contained premium HTML dashboard with embedded timelines and failure screenshots.
  - `.json` version: A structured, machine-readable JSON representation of the entire run, including all diagnostic details, step traces, exception stack traces, and asset paths, designed specifically for future Aura integration.
- **Embedded Test Diagnostics**: For failed tests, the report will compile:
  - Step-by-step logs, debug messages, and trace information.
  - Browser console logs (errors and warnings).
  - Absolute/relative file links to failure screenshots and page source.
  - Trimmed, highly readable exception traces.
- **JUnit 4 & JUnit 5 Hooks**: Hooks in the Neodymium runners (JUnit 4 `NeodymiumRunner` / `NeodymiumCucumberRunListener` and JUnit 5 listeners via SPI) to automatically feed execution progress and environment contexts to the collector.
- **Configurable Output**: New configuration settings in `neodymium.properties` to enable/disable the report (`neodymium.report.enabled`), specify custom report folders (`neodymium.report.directory`), and customize standard outputs.

## Capabilities

### New Capabilities
- `quick-test-report`: Handles the collection of execution events, step logs, screenshots, formatting of HTML, Markdown, and structured JSON report versions, and saving of report files at the end of a test run.

### Modified Capabilities
<!-- None, this is a completely new feature introducing no breaking changes or requirement modifications to existing capabilities. -->

## Impact

- **Configuration**: Addition of new configuration properties in `neodymium.properties` (`neodymium.report.enabled`, `neodymium.report.directory`, `neodymium.report.console.summary`).
- **Neodymium Core**:
  - `NeodymiumConfiguration`: Introduce configuration fields and getters.
  - JUnit 4 support: Integration of the report generation trigger into `NeodymiumRunner` and/or registration of a custom `RunListener` inside the test notifier or a JVM shutdown hook to capture the final results.
  - JUnit 5 support: Registering a launcher `TestExecutionListener` via Java SPI (`META-INF/services/org.junit.platform.launcher.TestExecutionListener`) to capture JUnit 5 test runs.
  - Visual Elements: Integration with the screenshot capture utility to link screenshot paths directly to the failed test results.
- **Dependencies**: No external dependencies added; pure JDK, existing Selenide APIs, and JUnit APIs are used. (JSON writing will use standard JSON-B / Jackson, or simple string builder/custom JSON serializer to keep zero external dependencies).

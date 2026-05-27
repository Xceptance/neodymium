## Context

Automated tests in Neodymium run across various environments (local development, CI/CD pipelines) and often execute multiple tests in parallel across different browser configurations. Currently, engineers rely on either raw console logs or heavy external viewers like Allure. Raw console logs are cluttered and hard to parse, while Allure requires a local server or a browser to display results. 

This design establishes a lightweight, zero-configuration (but fully customizable) human-readable and machine-readable test report generation system that operates natively at the end of both JUnit 4 and JUnit 5 test runs. It produces **three** distinct output formats:
1. A Markdown (`.md`) file for terminal/console logs.
2. A beautifully designed, self-contained HTML (`.html`) dashboard with interactive diagnostics, embedded step-by-step execution traces, and failure screenshots.
3. A structured, machine-readable JSON (`.json`) report containing all collected execution metrics, diagnostic traces, and asset paths, designed specifically for future integration with the **Aura** reporting ecosystem.

## Goals / Non-Goals

**Goals:**
- Provide a concise developer-focused Markdown report file summarizing the test run results, failures, and asset links.
- Provide a self-contained, visually premium HTML report featuring dynamic styling, collapsible failed test trees, chronological timelines of Selenide steps, browser console errors, and inline failure screenshots (base64 embedded for portability).
- Produce a structured, schema-stable JSON report containing complete metadata, metrics, and granular step timelines for future ingestion by Aura.
- Print a high-contrast console summary banner at the end of execution to System.out.
- Gather step-by-step browser interaction traces automatically using Selenide's `LogEventListener`.
- Capture browser console logs (errors and warnings) on failures.
- Maintain full thread-safety and fork safety for multi-browser and parallel runs.

**Non-Goals:**
- Fully replace heavy historical reporting systems like Allure.
- Introduce heavy external dependencies (e.g. charting JS libraries or external CSS grids). The HTML file must be entirely self-contained with inline styled CSS.

## Decisions

### Decision 1: Thread-Safe Singleton Collector (`TestReportCollector`)
- **Choice**: Use a thread-safe singleton, `TestReportCollector`, backed by a thread-safe map of thread/test contexts to collect `TestResult` objects and chronological test execution steps.
- **Rationale**: Multi-browser execution and parallel execution are common in Neodymium. A centralized, thread-safe aggregator ensures no events are lost or corrupted by concurrent writes.
- **Alternatives Considered**: 
  - *ThreadLocal collector*: Rejected because it would prevent the final reporter from seeing tests executed on other threads.
  - *Immediate File Writes*: Rejected because it would cause high I/O overhead and severe file locking conflicts in multi-threaded environments.

### Decision 2: Hybrid Lifecycle Triggers (JUnit Listeners + JVM Shutdown Hook)
- **Choice**: Hook into the test frameworks to record execution events, and register a JVM shutdown hook to write the final consolidated report.
  - **JUnit 4**: Override `run(RunNotifier)` in `NeodymiumRunner` and add a custom `RunListener`. Also support Cucumber test runs via `NeodymiumCucumberRunListener`.
  - **JUnit 5**: Register a `TestExecutionListener` via SPI (`META-INF/services/org.junit.platform.launcher.TestExecutionListener`).
  - **Shutdown Hook**: A JVM shutdown hook is registered on the first test start to compile and write the report to disk and print the console banner.
- **Rationale**: Since tests might be run across multiple runner classes or suites, JUnit's `testRunFinished` event is often fired multiple times (once per runner). Triggering the report write only on standard framework events leads to partial or overwritten reports. A JVM shutdown hook ensures that exactly one consolidated report is compiled and written when the entire JVM exits.
- **Alternatives Considered**:
  - *Surefire listener configuration only*: Rejected because it requires the user to modify their `pom.xml`, violating the zero-configuration goal.

### Decision 3: Selenide LogEventListener for Step-by-Step Chronological Traces
- **Choice**: Register a custom Selenide `LogEventListener` during runner initialization. This listener captures all interactive browser events (clicks, inputs, validations) chronologically, recording status, duration, and descriptions.
- **Rationale**: To provide actual trace information, we must record steps. Using Selenide's log events allows us to automatically reconstruct the exact timeline leading to a test failure, helping developers diagnose bugs instantly.

### Decision 4: Single Self-Contained HTML File with Inline Base64 Screenshots
- **Choice**: Produce a single self-contained HTML file. Failure screenshots will be encoded in base64 and embedded directly inside the HTML markup (`<img src="data:image/png;base64,...">`). In addition, raw screenshot files can be saved to a `screenshots/` subdirectory alongside the Markdown file.
- **Rationale**: A self-contained HTML report is extremely portable. It can be shared as a single email attachment, saved as a CI build artifact, or viewed locally without broken relative links or missing asset directory problems.

### Decision 5: Stable JSON Schema for Future Aura Integration
- **Choice**: Compile the final report data into a clean, structured JSON file (`neodymium-report.json`). To avoid external library dependencies, we will use a dedicated custom JSON serializer (utilizing JDK's built-in Gson/Jackson if available in Neodymium, or a robust custom `JsonWriter` pattern) ensuring stable key mappings.
- **JSON Structure**:
  ```json
  {
    "timestamp": "2026-05-26T12:00:00Z",
    "status": "FAILED",
    "metrics": {
      "total": 12,
      "passed": 11,
      "failed": 1,
      "ignored": 0,
      "durationMs": 42100
    },
    "environment": {
      "os": "Linux",
      "javaVersion": "21.0.2",
      "neodymiumVersion": "3.0.0"
    },
    "tests": [
      {
        "className": "com.xceptance.posters.backoffice.TC_BFC_002_Role_Based_Access",
        "methodName": "testAccessControl",
        "browser": "Chrome_120",
        "durationMs": 3500,
        "status": "FAILED",
        "failure": {
          "exception": "java.lang.AssertionError",
          "message": "Expected role dashboard to be visible",
          "screenshotPath": "screenshots/TC_BFC_002_Role_Based_Access_failure.png",
          "pageSourcePath": "screenshots/TC_BFC_002_Role_Based_Access_failure.html",
          "cleanedStackTrace": "...",
          "browserConsoleLogs": [
            {
              "level": "SEVERE",
              "message": "Uncaught ReferenceError: dashboardInit is not defined"
            }
          ]
        },
        "traceSteps": [
          {
            "step": "Click button: Login",
            "status": "PASSED",
            "durationMs": 450
          }
        ]
      }
    ]
  }
  ```
- **Rationale**: This structured JSON output provides a standardized contract. Future reporting backends (like Aura) can simply parse this file to populate their databases or build unified execution histories.

### Decision 6: Harvesting Browser Console Logs on Failure
- **Choice**: Upon test failure, if active browser drivers are available, execute a Selenium API call to retrieve `LogType.BROWSER` logs (warnings and errors).
- **Rationale**: Uncaught JavaScript exceptions or network request errors are often the root cause of automated test failures, but are omitted in typical JUnit exception stack traces.

## Risks / Trade-offs

- **[Risk] Multiple JVM forks in Maven/Gradle**  
  *Description*: If Maven Surefire is configured with `<forkCount>2</forkCount>`, tests run in separate JVMs. A single consolidated report cannot be written because the JVMs are completely isolated.  
  *Mitigation*: Each fork will write its own report at `neodymium-report.md`. We will append the fork or process ID to the report name (e.g., `neodymium-report-fork1.md`) or overwrite it safely if specified, and document this behavior in the configuration guide.
- **[Risk] Large Report File Size due to Base64 Images**  
  *Description*: If many tests fail, embedding multiple base64 screenshots inside a single HTML file can make the file size very large (e.g. tens of megabytes).  
  *Mitigation*: We will only capture and embed screenshots for *failed* test cases. We will also compress screenshots using JPEG or standard PNG compression before converting to base64, and provide a config property `neodymium.report.embed.screenshots` (default: `true`) to allow switching to relative file paths.

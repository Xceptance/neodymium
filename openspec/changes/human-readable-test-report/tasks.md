## 1. Configuration Setup

- [ ] 1.1 Add configuration properties (`neodymium.report.enabled`, `neodymium.report.directory`, `neodymium.report.console.summary`, `neodymium.report.embed.screenshots`) to `NeodymiumConfiguration.java`.
- [ ] 1.2 Document and add default values for the new properties in `config/neodymium.properties`.

## 2. Core Reporting & Step Trace Collection

- [ ] 2.1 Create the thread-safe `TestReportCollector.java` class under package `com.xceptance.neodymium.report` to aggregate execution events and chronological step traces.
- [ ] 2.2 Create `NeodymiumStepTraceListener.java` extending Selenide `LogEventListener` to automatically record browser interactions (clicks, inputs, asserts) per test thread.
- [ ] 2.3 Implement exception cleaning utility to format stack traces, filtering out framework internal reflection, JUnit, and surefire frames.
- [ ] 2.4 Implement browser console log harvester using Selenium's logging API on failure events.

## 3. Formatting & Serialization (HTML, MD, and JSON)

- [ ] 3.1 Create `MarkdownReportFormatter.java` to compile the quantitative metrics and failed test lists with screenshot links and trace summaries.
- [ ] 3.2 Create `HTMLReportFormatter.java` to generate a self-contained premium HTML dashboard with inline CSS, pure SVG circular charts, collapsible failure cards, step timelines, and embedded base64 screenshots.
- [ ] 3.3 Create `JSONReportFormatter.java` to serialize the collected test execution data, metadata, trace steps, and asset paths into a structured machine-readable JSON format for future Aura integration.
- [ ] 3.4 Create `ConsoleReportFormatter.java` to format the high-visibility console banner summary.

## 4. Framework & Runner Hooking

- [ ] 4.1 Override `run(RunNotifier notifier)` in `com.xceptance.neodymium.junit4.NeodymiumRunner` to attach our run listener.
- [ ] 4.2 Implement a custom JUnit 4 `RunListener` to feed test starts, completions, and exceptions to `TestReportCollector`.
- [ ] 4.3 Create `NeodymiumJUnit5ExecutionListener` extending JUnit 5 `TestExecutionListener` to collect platform execution events.
- [ ] 4.4 Register the JUnit 5 listener in `src/main/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener`.

## 5. Lifecycle Management & Fork Safety

- [ ] 5.1 Implement a JVM shutdown hook inside `TestReportCollector` to consolidate collected test data, write HTML, Markdown, and JSON report files on exit, and print the console summary.
- [ ] 5.2 Add process/fork ID naming suffix logic (e.g. `neodymium-report-fork1.json`) when multiple surefire forks are active to prevent concurrent file overwrite.

## 6. Testing & Quality Verification

- [ ] 6.1 Create automated tests to run dummy test cases (passing and failing) and assert that HTML, Markdown, and JSON reports are generated correctly with correct statistics, step logs, and embedded base64 screenshots.
- [ ] 6.2 Ensure all new source files strictly conform to coding standards (aggressive `final` modifiers, MIT/Apache License headers, Allman new-line brace style).

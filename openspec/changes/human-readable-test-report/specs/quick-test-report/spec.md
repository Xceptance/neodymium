## ADDED Requirements

### Requirement: Report Generation Configuration
The reporting system SHALL support configuration of report enabling/disabling, the output directory, and console summary printing via `neodymium.properties`.

#### Scenario: Enabling Report and Specifying Directory
- **WHEN** `neodymium.report.enabled` is set to `true` and `neodymium.report.directory` is set to `target/quick-reports`
- **THEN** the system SHALL generate Markdown, HTML, and JSON reports in `target/quick-reports` upon completion of the test run.

#### Scenario: Disabling Report
- **WHEN** `neodymium.report.enabled` is set to `false`
- **THEN** the system SHALL NOT generate any report files or print any final console summary.

### Requirement: Thread-Safe Metrics and Trace Collection
The system SHALL collect execution metrics and test step logs in a thread-safe manner across all running test suites and execution threads.

#### Scenario: Test Execution Collection in Parallel Run
- **WHEN** a multi-threaded test suite is executed
- **THEN** the system SHALL safely increment passed, failed, and ignored counters and collect separate execution step logs for each test case without race conditions or loss of data.

### Requirement: Capturing Failure Details and Assets
The system SHALL capture detailed diagnostic information, failure screenshots, page source, and browser console logs for every failed test case.

#### Scenario: Recording Failed Test Case with Screenshots
- **WHEN** a test case fails (e.g., throws an `AssertionError` or another `Exception`)
- **THEN** the system SHALL capture the class name, method name, execution duration, active browser name, active screenshot file path, page source file path, and a trimmed/cleaned exception stack trace.

### Requirement: Capturing Test Step Traces and Logs
The system SHALL capture step-by-step trace information and logging details during the execution of each test case.

#### Scenario: Recording Test Step Logs
- **WHEN** a test executes steps (e.g., via Selenide actions, `TestStepListener`, or manual logging)
- **THEN** the system SHALL append these steps chronologically to the test's execution trace.

#### Scenario: Recording Browser Console Logs
- **WHEN** a test case fails
- **THEN** the system SHALL retrieve and store browser console error and warning logs to include in the diagnostic output.

### Requirement: Markdown and HTML Report Generation
The system SHALL generate both a Markdown (`.md`) file and an HTML (`.html`) file summarizing the test run at the end of execution.

#### Scenario: Generating Markdown Report
- **WHEN** the test run completes and the report is enabled
- **THEN** the system SHALL write a `.md` report containing a success status banner, a quantitative metrics table, environmental details, and a clear text-based list of failed tests with trimmed exceptions and links to failure screenshots.

#### Scenario: Generating HTML Report
- **WHEN** the test run completes and the report is enabled
- **THEN** the system SHALL write a self-contained `.html` report featuring an elegant modern UI design, collapsible detailed sections for failed tests, inline image elements displaying failure screenshots, step-by-step execution timelines, and browser console logs.

### Requirement: Machine-Readable JSON Report Generation
The system SHALL generate a structured, machine-readable JSON file (`.json`) containing all collected execution metrics, diagnostic traces, and asset paths at the end of the test run to enable future Aura integration.

#### Scenario: Generating JSON Report
- **WHEN** the test run completes and the report is enabled
- **THEN** the system SHALL write a `.json` file containing structured keys for success rates, total run statistics (total, passed, failed, ignored), browser configurations, and an array of individual test executions with step-by-step trace lists, console outputs, and failure screenshot paths.

### Requirement: Console Summary Output
The system SHALL print a concise, visually clear, high-contrast summary banner to the standard console output at the very end of the test run.

#### Scenario: Printing Banner to Console
- **WHEN** the test run completes and `neodymium.report.console.summary` is set to `true`
- **THEN** it SHALL write a high-visibility text-based banner to the console displaying the overall status, total counts, and names of any failed tests.

### Requirement: JUnit 4 and JUnit 5 Lifecycle Hooks
The reporting system SHALL automatically hook into the lifecycle of both JUnit 4 and JUnit 5 test runs to collect execution events.

#### Scenario: Auto-Registration in Test Suites
- **WHEN** a test suite runs under JUnit 4 (via `NeodymiumRunner`) or JUnit 5
- **THEN** the system SHALL capture test start, finish, failure, and ignore events without requiring any manual test annotations or custom listener setup by the user.

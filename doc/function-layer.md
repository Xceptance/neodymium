# Neodymium Aura: Shared Function Layer & Error Guards Architecture

**Document Version:** 1.0.0  
**Target Architecture:** Shared Application Services, Error Resilience, & Protocol Adapters (MCP / REST)  
**Modules Affected:** `aura-manager`, `neodymium-core`

---

## 1. Overview & Problem Analysis

### 1.1 The Existing System State
1. **Manager UI vs. MCP:**
   - Prior to MCP, test execution was driven via UI form/AJAX endpoints in `AuraTestQueueController`.
   - The MCP server implementation introduced `AuraMcpToolService` and `AuraMcpResourceService`, which directly interact with raw core services (`AuraQueueService`, `AuraFileService`, `AuraReportDataService`, `McpExecutionCoordinator`).
2. **Current Deficiencies:**
   - **Duplicated Business Logic:** Report summary generation, failure distillation, and metric computations are duplicated between `AuraMcpToolService` and `AuraMcpResourceService`.
   - **No Shared Facade for Future API:** Future REST (`/api/v1/...`) or CLI clients would need to reimplement orchestration, path validation, and dataset handling from scratch.
   - **Subprocess Hang Risk (Existing in Manager & MCP):** In `AuraQueueService`, subprocesses are awaited via `p.waitFor()` without an upper-bound timeout. If a headless Chrome instance or Maven child process deadlocks or hangs indefinitely, the execution thread blocks indefinitely, keeping `runningQueue` set to `true`.
   - **Uncaught Exception Propagation:** An unhandled runtime error inside an MCP tool method surfaces as an opaque JSON-RPC `-32603 Internal Error`, giving AI agents no actionable recovery context.

---

## 2. Shared Function Layer Architecture

The architecture decouples business capabilities from transport adapters into three core service interfaces located under `com.xceptance.aura.core.service`:

```
                    External Clients / AI Agents
                ┌───────────────────────────────────┐
                │ Antigravity / Cursor / CI / UI /  │
                └─────────────────┬─────────────────┘
                                  │
            ┌─────────────────────┴─────────────────────┐
            ▼                                           ▼
┌─────────────────────────┐                 ┌─────────────────────────┐
│     MCP Layer (/mcp)    │                 │ Future REST API (/api)  │
│  - AuraMcpToolService   │                 │  - AuraExecutionApiCtrl │
│  - AuraMcpResourceServ. │                 │  - AuraPlaybookApiCtrl  │
└───────────┬─────────────┘                 └───────────┬─────────────┘
            │                                           │
            └─────────────────────┬─────────────────────┘
                                  ▼
        ┌───────────────────────────────────────────────────┐
        │        Shared Aura Application Services           │
        │           (com.xceptance.aura.core)               │
        │                                                   │
        │  ┌───────────────────────┐ ┌────────────────────┐ │
        │  │  AuraPlaybookService  │ │AuraExecutionService│ │
        │  └───────────┬───────────┘ └──────────┬─────────┘ │
        │              │                        │           │
        │              ▼                        ▼           │
        │  ┌───────────────────────┐ ┌────────────────────┐ │
        │  │ AuraDiagnosticService │ │AuraWatchdogService │ │
        │  └───────────┬───────────┘ └──────────┬─────────┘ │
        └──────────────┼────────────────────────┼───────────┘
                       │                        │
                       ▼                        ▼
        ┌───────────────────────────────────────────────────┐
        │          Underlying Platform Engines              │
        │  - McpExecutionCoordinator / Process Runner       │
        │  - AuraQueueService                               │
        │  - AuraFileService                                │
        │  - AuraReportDataService                          │
        │  - SeleniumErrorDistiller                         │
        │  - AdaptivePayloadReporter                        │
        └───────────────────────────────────────────────────┘
```

---

## 3. Component Breakdown & Service Contracts

### 3.1 Domain DTOs (`com.xceptance.aura.core.dto`)
- **`PlaybookSummaryDto`**: Discovered playbooks (`filePath`, `fileName`, `datasetCount`, `hasError`, `errorMessage`).
- **`ExecutionLaunchResultDto`**: Standardized launch result (`runId`, `status`, `queuePosition`, `message`, `errorCode`).
- **`ExecutionStatusResultDto`**: Universal status response with metrics (`total`, `passed`, `failed`, `skipped`, `durationMs`) and adaptive distilled failure clusters.
- **`FailurePageResultDto`**: Paginated failure drilldown (`runId`, `category`, `page`, `pageSize`, `totalCount`, `failures`).
- **`AuraOperationResult<T>`**: Unified result monad / container wrapping payload, status (`SUCCESS`, `ERROR`, `CONFIRMATION_REQUIRED`), and structured diagnostic message.

### 3.2 Service Boundaries (`com.xceptance.aura.core.service`)

#### 1. `AuraPlaybookService`
- `List<PlaybookSummaryDto> listPlaybooks(String filter)`: Safe playbook discovery with error isolation (malformed YAML files in one directory do not crash the entire list).
- `String readPlaybook(String path)`: Validated playbook reading with path traversal defense (`HostAllowlistValidator`).
- `AuraOperationResult<Void> savePlaybook(String path, String content)`: Atomic playbook writing.
- `AuraOperationResult<Boolean> deletePlaybook(String path)`: Safe playbook deletion.

#### 2. `AuraExecutionService`
- `ExecutionLaunchResultDto dispatchRun(String testPath, String browserProfile, String dataset)`: Validates path, initializes defaults, schedules through execution coordinator.
- `AuraOperationResult<Void> cancelRun(String runId)`: Aborts active process, marks queue as cancelled, and drains queue.
- `boolean isRunActive(String runId)`: Status query against active executor.

#### 3. `AuraDiagnosticService`
- `ExecutionStatusResultDto getRunStatus(String runId)`: Centralized status resolution, metrics calculation, and token-bounded distillation using `AdaptivePayloadReporter`. Replaces duplicate report parsing across MCP tools and MCP resources.
- `FailurePageResultDto getFailures(String runId, String category, int page, int pageSize)`: Token-safe paginated stack trace inspection.

#### 4. `AuraWatchdogService`
- Heartbeat & execution watchdog. Periodically inspects active subprocesses against a configurable timeout threshold (`neodymium.aura.execution.maxTimeoutSeconds`, default: 900s / 15 minutes).
- Automatically aborts hanging subprocesses and frees `AuraQueueService` to prevent permanent queue lockups.

---

## 4. Error Guards & Resilience Strategy

### 4.1 Subprocess Hang & Deadlock Prevention (Watchdog Guard)
- **Problem:** `p.waitFor()` blocks forever if headless Chrome freezes or Maven hangs.
- **Solution:**
  1. `AuraQueueService` execution loop adopts `p.waitFor(timeout, unit)` with a heartbeat mechanism.
  2. If the timeout expires without process termination, the watchdog initiates forceful termination (`p.destroyForcibly()`), marks the execution as `ERROR_TIMEOUT`, and cleanly triggers the queue completion handler.

### 4.2 Graceful Error Monads (No Unhandled Internal Crashes)
- All shared service methods wrap operations in protective `try/catch` boundaries.
- Checked or unchecked exceptions are caught and transformed into typed `AuraOperationResult` or standard DTOs with clear `errorCode` and human/agent-readable recovery guidance.
- MCP tools unpack `AuraOperationResult` to return structured JSON responses rather than throwing raw exceptions that trigger JSON-RPC `-32603`.

### 4.3 Two-Step Handshake Guardrails for Disruptive Operations
- Destructive operations (`delete_test`, `cancel_run`) enforce cryptographic or random time-bound confirmation tokens via `TwoStepConfirmationManager`.
- Initial invocation returns `CONFIRMATION_REQUIRED` + token. Secondary invocation with token executes the operation.

---

## 5. MCP Tool Decomposition & Composition Strategy

### 5.1 Protocol Adapter Role
`AuraMcpToolService` and `AuraMcpResourceService` become pure transport adapters:
- They validate MCP tool parameters (`@McpToolParam`).
- They delegate business operations to `AuraExecutionService`, `AuraPlaybookService`, and `AuraDiagnosticService`.
- They convert domain DTOs to MCP-compatible maps or return DTOs directly.

### 5.2 Splitting vs. Combining Tools
- **Split Domain MCP Tools (High Intent):**
  - Discovery: `list_tests`
  - Lifecycle: `create_test`, `delete_test`
  - Execution: `run_test`, `cancel_run`
  - Diagnostics: `get_status`, `get_test_failures`
- **Optional Composite Tool:**
  - `manage_test_execution(action, runId, testPath, ...)` can be provided as a single consolidated tool for token-constrained agent environments, built cleanly on top of the same underlying `AuraExecutionService`.

---

## 6. Implementation & Rollout Steps

1. **Step 1: Domain DTOs (`com.xceptance.aura.core.dto`)**
   - Create protocol-agnostic DTOs and result wrappers.
2. **Step 2: Core Shared Services (`com.xceptance.aura.core.service`)**
   - Implement `AuraPlaybookService`, `AuraExecutionService`, and `AuraDiagnosticService`.
   - Add timeout / watchdog guards in execution coordination.
3. **Step 3: Refactor MCP Adapters**
   - Refactor `AuraMcpToolService` and `AuraMcpResourceService` to delegate all operations to the shared services.
4. **Step 4: Unit & Integration Testing**
   - Comprehensive unit tests covering normal operations, error paths, timeouts, and two-step confirmation flows.

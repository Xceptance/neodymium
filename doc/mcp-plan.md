# Comprehensive Implementation Plan: Model Context Protocol (MCP) Server for Neodymium Aura Manager (Streamable HTTP)

**Linked Issue:** [GitLab Work Item #82](https://gitlab.xceptance.de/testautomation/test-automation-team-tasks/-/work_items/82)  
**Parent Research:** [GitLab Work Item #80](https://gitlab.xceptance.de/testautomation/test-automation-team-tasks/-/work_items/80)  
**Target Branch:** `feature/neo-gl-82-mcp-server-streamable-http`  
**Base Branch:** `develop`

---

## 1. Overview & Context

### 1.1 Problem Statement & Background
Neodymium Aura Manager currently functions primarily as an interactive workstation UI and local test execution hub. As test orchestration migrates to a dedicated remote server instance, external AI agents (e.g., Google Antigravity, Cursor, Claude Desktop, autonomous CI agents) require a standardized, secure, token-efficient, and asynchronous interface to:
1. Discover test suites, playbooks (`.yaml`), and metadata.
2. Dispatch long-running Selenium and AI-driven test runs without blocking HTTP channels.
3. Query live execution progress and retrieve distilled, token-budgeted test reports.
4. Safely create or delete test playbooks with strict validation and destructive safeguards.

To solve this, Aura Manager is implementing a native **Model Context Protocol (MCP) Server** exposed on `/mcp`.

### 1.2 Primary Goals
- **Standardized Ingress via Streamable HTTP**: Expose a compliant MCP Server using the modern `STREAMABLE` protocol over HTTP on `/mcp` (replacing deprecated SSE).
- **Hexagonal Shared Application Services**: The MCP server directly injects existing Spring `@Service` components (`AuraQueueService`, `AuraFileService`, `AuraSettingsService`, `AuraReportDataService`) in-memory. Zero local REST HTTP loopback calls.
- **Asynchronous, Non-Blocking Execution**: Dispatching a test via `run_test` returns an execution token (`runId`) within `<100ms`. Test execution continues in the background regardless of whether an agent disconnects.
- **Token-Budgeted Error Distillation (`SeleniumErrorDistiller`)**: Raw Selenium stack traces (which easily span 50–100 framework frames) are stripped of all internal Selenium/JDK/Spring noise, extracting only element locators, current page URLs, DOM snippets, and the user-level failing step (capped at 1,500 characters).
- **Adaptive Reporting Payload & Universal Metrics Envelope**:
  - Every status inquiry and report resource includes a standard `metrics` block (`total`, `passed`, `failed`, `durationMs`).
  - For small failure counts ($\le 5$), full inline distilled failures are returned.
  - For larger failure counts ($> 5$), failures are clustered by normalized exception signature and root cause message.
  - If distinct failure groups exceed 5 (high entropy / unclustered failures), raw error traces are omitted from inline responses to prevent LLM context window overflow, providing an explicit notice directing the agent to paginate or inspect the dedicated MCP resource.
- **Strict Guardrails & Security**:
  - Bearer token authentication on `/mcp`.
  - Role-Based Access Control (RBAC): `ROLE_MCP_READ` (list, read resources, inspect status) vs. `ROLE_MCP_EXECUTE` (trigger runs, create/delete tests).
  - JSR-380 Bean Validation (`@NotNull`, `@Pattern`, `@Size`) on all tool parameters to defend against LLM hallucination.
  - Two-step handshake confirmation for destructive operations (`delete_test`).
  - SSRF and path traversal validation preventing target execution against private/metadata IPs or unauthorized directories.
- **Aura Settings UI Integration**: A dedicated tab in the Aura Manager Settings modal to configure the MCP server toggle, manage authentication tokens, inspect server status/URL, and copy ready-to-use client JSON configurations.

### 1.3 Non-Goals
- **Multi-Tenant Physical Partitioning**: The MVP operates as a single-tenant workspace per server instance using the current local H2 database and filesystem. Physical database schema alterations (`WHERE tenant_id = :tenantId`), multi-tenant JPA entity rewrites, and S3 bucket tenant prefixing are strictly out of scope for this MVP ticket. The MCP layer will bind a default tenant context (`default`) internally so that multi-tenancy can be enabled in a future release without breaking MCP tool contracts.
- **Modifying Neodymium Core test runner semantics**: The execution engine continues to use the existing `AuraQueueService` and Selenide/Selenium runner pipeline.

---

## 2. Technical & Architectural Decisions

### 2.1 Architecture Diagram

```
 +-------------------------------------------------------------------------+
 |                External AI Agents (Antigravity, Cursor, Claude)          |
 +-------------------------------------------------------------------------+
                                      |
                     Streamable HTTP (/mcp) + Bearer Token
                                      v
 +-------------------------------------------------------------------------+
 | Aura Manager - Security & Ingress Layer                                  |
 |  - McpSecurityInterceptor / TokenAuthFilter (ROLE_MCP_READ / EXECUTE)   |
 |  - HostAllowlistValidator (SSRF & Path Traversal Prevention)            |
 |  - McpSubscriptionManager (45s keep-alive pings, 120s timeout)          |
 +-------------------------------------------------------------------------+
                                      |
                                      v
 +-------------------------------------------------------------------------+
 | Spring AI 2.0 WebMVC MCP Server (@McpTool, @McpResource)                |
 |                                                                         |
 |  Tools:                                  Resources (test://):           |
 |   - list_tests (ReadOnly)                 - test://runs/{runId}/summary |
 |   - run_test (Async Dispatch <100ms)      - test://playbooks/{path}     |
 |   - get_status (Universal Metrics)                                      |
 |   - get_test_failures (Paginated Failure Inspection)                    |
 |   - create_test (Prompt Validation)                                     |
 |   - delete_test (2-Step Handshake)                                      |
 |   - cancel_run (2-Step Handshake Process Abort)                         |
 +-------------------------------------------------------------------------+
          |                                               |
          | Direct In-Memory Injection                     | Distillation Pipeline
          v                                               v
 +-----------------------------+              +----------------------------+
 | Existing Spring Services    |              | SeleniumErrorDistiller     |
 |  - AuraQueueService         |              |  - Strip framework frames  |
 |  - AuraFileService          |              |  - Extract URL & locator   |
 |  - AuraSettingsService      |              |  - Capped to 1,500 chars   |
 |  - AuraReportDataService    |              | AdaptivePayloadReporter    |
 +-----------------------------+              |  - Threshold <= 5: Inline  |
          |                                   |  - Threshold > 5: Clusters |
          v                                   |  - High Entropy: Resource  |
 +-----------------------------+              +----------------------------+
 | Subprocess Execution Engine |
 |  - Neodymium Selenium Runs  |
 +-----------------------------+
```

### 2.2 Architecture Topology
The architecture separates protocol ingress, security, orchestration, and automation execution into clear boundaries:

| Layer                 | Component Key                                           | Responsibilities                                                                                                  |
|-----------------------|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| Agent Ingress         | External AI Agents (Antigravity, Cursor, Claude)        | Send JSON-RPC 2.0 requests over Streamable HTTP with Bearer tokens.                                               |
| Perimeter Security    | McpSecurityConfiguration & McpTokenAuthenticationFilter | Servlet-level token verification (MessageDigest.isEqual), RBAC assignment, SSRF guardrails.                       |
| Session & Lifecycle   | McpSubscriptionManager                                  | 45-second keep-alive heartbeats, 120-second idle eviction, graceful disconnect handling.                          |
| MCP Protocol Adapter  | Spring AI WebMVC Server (@McpTool, @McpResource)        | Declarative schema generation, JSON-RPC dispatch, protocol discovery support.                                     |
| Cross-Thread Bridge   | McpContextTaskDecorator                                 | Propagates SecurityContext, tenant metadata, and MDC logging across asynchronous worker boundaries.               |
| Application Services  | Shared In-Memory Services                               | Direct in-memory invocation of AuraQueueService, AuraFileService, AuraSettingsService, and AuraReportDataService. |
| Telemetry & Artifacts | SeleniumErrorDistiller & AdaptivePayloadReporter        | Framework stack-trace pruning, credential scrubbing, failure clustering, and token capping.                       |
| Execution Engine      | Subprocess Execution Engine                             | Isolated subprocess execution of Neodymium Selenium test runs.                                                    |

### 2.3 Subsystem API Boundary & Coarse-Grained Facade Pattern
To maintain high execution speed and minimize LLM token consumption, the MCP layer acts as a coarse-grained facade:
- In-Memory Core Services: Interactions with AuraFileService, AuraQueueService, and AuraSettingsService occur via direct method calls in the same JVM memory space. Internal HTTP REST calls are strictly avoided.
- Remote Subsystems (Selenium Grid, Remote Nodes): Outbound calls to remote WebDriver hubs or remote reporting stores use a configured Spring RestClient with explicit timeouts (connectTimeout = 3s, readTimeout = 10s).
- Defensive Circuit Breaking & Degradation: If an external dependency (such as Selenium Grid or report storage) is unreachable, the tool layer catches connection exceptions (ResourceAccessException, ConnectException) and returns an informative JSON payload instead of failing with an unhandled exception:
```JSON
{
    "status": "UNAVAILABLE",
    "dependency": "SELENIUM_GRID",
    "message": "The remote Selenium Grid hub at http://grid:4444 is currently unreachable. No tests were dispatched.",
    "retryable": true
}
```
### 2.4 Tech Stack & Dependencies
- **Java Baseline**: Java 21 LTS (`<java.version>21</java.version>`).
- **Spring Boot Baseline**: Spring Boot 4.1.0 (`spring-boot-starter-parent`).
- **MCP Framework**: Spring AI 2.0.0 GA (`org.springframework.ai:spring-ai-starter-mcp-server-webmvc:2.0.0`), verified available on Maven Central.
  - Native integration with Spring Boot 4 WebMVC.
  - Built-in support for MCP Specification (2025-11-25) and `STREAMABLE` transport protocol.
  - Annotation-driven tool and resource scanner (`@McpTool`, `@McpResource`, `@McpToolParam`).
- **Validation**: Jakarta Validation / Hibernate Validator (`jakarta.validation-api`).
- **JSON Handling**: Jackson 3 / Jackson 2 compatibility (aligning with Spring Boot 4 WebMVC).

### 2.5 File Structure & Package Layout

```
aura-manager/
├── pom.xml                                               # Adds spring-ai-starter-mcp-server-webmvc
└── src/
    ├── main/
    │   ├── java/com/xceptance/aura/mcp/
    │   │   ├── config/
    │   │   │   ├── AuraMcpConfiguration.java             # MCP Server bean definitions & transport config
    │   │   │   └── AuraMcpProperties.java                # neodymium.aura.mcp.* configuration properties
    │   │   ├── security/
    │   │   │   ├── McpSecurityInterceptor.java           # Bearer token validation & RBAC role checks
    │   │   │   ├── HostAllowlistValidator.java           # SSRF & path traversal guardrails
    │   │   │   └── TwoStepConfirmationManager.java       # Short-lived tokens for destructive actions
    │   │   ├── session/
    │   │   │   └── McpSubscriptionManager.java           # Keep-alive heartbeats (45s) & session eviction
    │   │   ├── service/
    │   │   │   ├── AuraMcpToolService.java               # @McpTool implementations
    │   │   │   ├── AuraMcpResourceService.java           # @McpResource implementations
    │   │   │   ├── McpExecutionCoordinator.java          # FIFO run queue manager & AuraQueueService adapter
    │   │   │   ├── SeleniumErrorDistiller.java           # Stack trace cleaning & context extraction
    │   │   │   └── AdaptivePayloadReporter.java          # Clustering & universal metrics formatter
    │   │   └── dto/
    │   │       ├── RunStatusResponseDto.java             # JSON DTO for get_status with universal metrics
    │   │       ├── FailureClusterDto.java                # Clustered failure group representation
    │   │       └── ConfirmationTokenDto.java             # Two-step handshake token response
    │   └── resources/
    │       ├── application.properties                    # Default MCP server properties
    │       ├── static/
    │       │   └── dashboard-settings.js                 # JS logic for MCP tab & token copy
    │       └── templates/fragments/
    │           └── settings-modal.html                   # Added MCP Server configuration tab
    └── test/
        └── java/com/xceptance/aura/mcp/
            ├── AuraMcpToolServiceTest.java               # Tests for all @McpTool operations
            ├── AuraMcpSecurityTest.java                  # Tests for token auth, RBAC & SSRF checks
            ├── SeleniumErrorDistillerTest.java           # Unit tests for stack trace pruning & token caps
            └── AdaptivePayloadReporterTest.java          # Unit tests for inline, clustering & spillover
```

---

## 3. Detailed Specifications

### 3.1 Transport & Connection Management
- **Endpoint**: `/mcp`
- **Protocol**: `STREAMABLE` (Streamable HTTP, JSON-RPC 2.0 streaming).
- **Transport Keep-Alive**: Periodic transport-level ping comment sent every **45 seconds** to prevent reverse proxy (NGINX/ALB) timeouts.
- **Connection Timeout**: **120 seconds** idle timeout before session termination.
- **Session Decoupling**: If the client disconnects or aborts the HTTP session, the active test run spawned in `AuraQueueService` is **not aborted**. It completes and persists reports into database/storage.

### 3.2 Security, RBAC & Parameter Guardrails
- **Authentication**:
  - Enabled via `neodymium.aura.mcp.enabled=true`.
  - Header: `Authorization: Bearer <token>`.
  - Configurable in `application.properties` or `ai.properties`: `neodymium.aura.mcp.auth.token=<secret>`.
- **RBAC Roles**:
  - `ROLE_MCP_READ`: Granted to read-only tools (`list_tests`, `get_status`) and resources (`test://*`).
  - `ROLE_MCP_EXECUTE`: Required for execution and mutating tools (`run_test`, `create_test`, `delete_test`, `cancel_run`).
  - Calling functions with unsufficient privileges will return JSON-RPC error -32001 (Unauthorized)
- **Universal Destructive Operation Safety Handshake (`delete_test`, `cancel_run`)**:
  - All destructive or disruptive operations enforce a two-step confirmation token pattern before mutating or terminating resources:
  - **For `delete_test(path)`**:
    - Step 1: Agent calls `delete_test(path)`. Server generates a 60-second UUID confirmation token and returns:
      ```json
      {
        "status": "CONFIRMATION_REQUIRED",
        "confirmationToken": "8f1a2b3c-...",
        "message": "Destructive operation: To permanently delete 'tests/Login.yaml', invoke delete_test with path and confirmationToken."
      }
      ```
    - Step 2: Agent calls `delete_test(path, confirmationToken="8f1a2b3c-...")`. Server validates token and performs deletion.
  - **For `cancel_run(runId)`**:
    - Step 1: Agent calls `cancel_run(runId="run_20260930_101500")`. Server generates a 60-second confirmation token and returns:
      ```json
      {
        "status": "CONFIRMATION_REQUIRED",
        "confirmationToken": "c7e2b19a-...",
        "message": "Disruptive operation: Aborting active test run 'run_20260930_101500' will terminate running browser sessions. Call cancel_run with runId and confirmationToken to confirm.",
        "expiresInSeconds": 60
      }
      ```
    - Step 2: Agent calls `cancel_run(runId="run_20260930_101500", confirmationToken="c7e2b19a-...")`. Server validates token and aborts the process. If the run already finished between steps, returns `"ALREADY_COMPLETED"`.
- **SSRF & Path Allowlist**:
  - All test paths must be normalized and reside strictly inside the project test folder (`tests/` or playbooks directory). Relative traversal (`..`) is rejected.
  - Target URLs in parameters are verified to ensure they do not target cloud metadata endpoints (`169.254.169.254`) or loopback addresses (`127.0.0.1`, `localhost`) unless explicitly allowed.

#### 3.2.1 Perimeter Security Architecture (McpSecurityConfiguration)
Authentication is enforced at the servlet filter level prior to MCP transport evaluation:   
- Endpoint Scope: All requests to /mcp and /mcp/** require authentication.   
- Token Authentication Scheme: Requests must supply an Authorization: Bearer <token> header.   
- Configuration Properties:
  - neodymium.aura.mcp.auth.read-token: Grants ROLE_MCP_READ.
  - neodymium.aura.mcp.auth.admin-token: Grants ROLE_MCP_READ and ROLE_MCP_EXECUTE.
  - neodymium.aura.mcp.auth.token: Master token granting all roles (ROLE_MCP_READ, ROLE_MCP_EXECUTE).
- Timing-Attack Resistance: Token validation utilizes constant-time byte comparisons via MessageDigest.isEqual().

```java
package com.xceptance.aura.mcp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class McpSecurityConfiguration {

    private final McpTokenAuthenticationFilter tokenAuthFilter;

    public McpSecurityConfiguration(McpTokenAuthenticationFilter tokenAuthFilter) {
        this.tokenAuthFilter = tokenAuthFilter;
    }

    @Bean
    public SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/mcp", "/mcp/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/mcp", "/mcp/**").authenticated()
                .anyRequest().denyAll()
            )
            .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

#### 3.2.2 Credential Shielding & Zero-Trust Parameter Rules
- No Secret Exposure: Raw passwords, API tokens, and database credentials are never accepted as parameters in tools or returned in responses.
- Ambient Environment Profiles: Agents supply symbolic identifiers (e.g., environment: "STAGING" or browserProfile: "chrome-headless").
- Server-Side Secret Resolution: AuraMcpToolService maps symbolic identifiers to server-side encrypted credentials managed by AuraSettingsService.
- Sanitization in Distiller: SeleniumErrorDistiller automatically scrubs URL query parameters matching sensitive keys (token=, password=, key=, secret=) before returning summaries to the LLM.

#### 3.2.3 Asynchronous Context Propagation
When run_test dispatches execution to AuraQueueService worker threads, security context and MDC logging information are preserved across thread boundaries using a TaskDecorator:
```java
package com.xceptance.aura.mcp.security;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class McpContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Map<String, String> loggingMdc = MDC.getCopyOfContextMap();

        return () -> {
            try {
                SecurityContextHolder.setContext(securityContext);
                if (loggingMdc != null) {
                    MDC.setContextMap(loggingMdc);
                }
                runnable.run();
            } finally {
                SecurityContextHolder.clearContext();
                MDC.clear();
            }
        };
    }
}
```

### 3.3 Execution Lifecycle & Core MCP Tools (`@McpTool`)

#### Execution State Machine:
All run executions transition through a clean 6-state lifecycle:
1. **`ENQUEUED`**: Waiting for prior test runs to finish in the server queue.
2. **`RUNNING`**: Actively executing browser automation / subprocess tests.
3. **`PASSED`**: Completed successfully with 0 failures.
4. **`FAILED`**: Completed, but 1 or more tests failed.
5. **`CANCELLED`**: Execution was aborted via `cancel_run`.
6. **`ERROR`**: Subprocess crashed, timed out, or encountered an unhandled infrastructure error.

#### Core MCP Tools:
1. **`list_tests`** (`readOnlyHint = true`)
   - Parameters: `filter` (optional string, regex/keyword).
   - Behavior: Calls `AuraFileService.listTestFiles()`. Returns array of test files, step counts, and tags.
2. **`run_test`**
   - Parameters:
     - `testPath` (`@NotNull`, `@Pattern(regexp = "^[a-zA-Z0-9_\\-\\./]+\\.ya?ml$")`): Path to playbook.
     - `browserProfile` (optional string): Target browser configuration key.
     - `dataset` (optional string): Dataset row identifier.
   - Behavior: Enforces canonical Neodymium run ID format (`run_yyyyMMdd_HHmmss`). Coordinates with `McpExecutionCoordinator`:
     - If `AuraQueueService.isRunningQueue()` is false: Dispatches subprocess immediately $\rightarrow$ returns `<100ms`:
       ```json
       {
         "status": "RUNNING",
         "runId": "run_20260930_101500",
         "message": "Test execution started immediately."
       }
       ```
     - If queue is currently busy: Places request in internal thread-safe FIFO queue (`ConcurrentLinkedQueue`) and returns immediately:
       ```json
       {
         "status": "ENQUEUED",
         "runId": "run_20260930_101502",
         "queuePosition": 1,
         "message": "Queue currently busy with active run. Request enqueued."
       }
       ```
     - When an active run completes (intercepted via `AuraQueueService.setOnRunCompletedListener`), `McpExecutionCoordinator` automatically dequeues and executes the next pending run.
3. **`get_status`** (`readOnlyHint = true`)
   - Parameters: `runId` (`@NotNull`, string).
   - Behavior: Returns the current state (`ENQUEUED`, `RUNNING`, `PASSED`, `FAILED`, `CANCELLED`, `ERROR`), the universal `metrics` envelope, and adaptive failure details.
4. **`get_test_failures`** (`readOnlyHint = true`)
   - Parameters:
     - `runId` (`@NotNull`, string): Target run identifier.
     - `category` (optional string): Filter by failure cluster category (e.g. `"ELEMENT_NOT_FOUND"`). If omitted, returns all failures for the run.
     - `page` (optional integer, default 1): 1-indexed page.
     - `pageSize` (optional integer, default 5, max 10): Maximum failures per page to maintain token budget.
   - Behavior: Returns a paginated list of distilled failure objects with exact locator, current URL, DOM snippet, and cleaned stack trace.
5. **`create_test`**
   - Parameters:
     - `testPath` (`@NotNull`, string): Destination relative playbook path.
     - `content` (`@NotNull`, string): YAML content.
   - Behavior: Validates content using Neodymium playbook prompt parser before saving via `AuraFileService`.
6. **`delete_test`** (`destructiveHint = true`)
   - Parameters:
     - `testPath` (`@NotNull`, string): Target file.
     - `confirmationToken` (optional string): Two-step verification token.
   - Behavior: Enforces confirmation handshake; deletes file once confirmed.
7. **`cancel_run`** (`destructiveHint = true`)
   - Parameters:
     - `runId` (`@NotNull`, string): Identifier of the run to cancel.
     - `confirmationToken` (optional string): Two-step verification token.
   - Behavior: Enforces two-step confirmation handshake. On valid token presentation, verifies `runId` matches active process in `AuraQueueService` and safely terminates the subprocess. If the run finished prior to token presentation, returns `"ALREADY_COMPLETED"`.

### 3.4 Selenium Error Distiller & Adaptive Reporting Engine

#### Distillation Rules (`SeleniumErrorDistiller`):
- Filter out all stack frames matching:
  - `org.openqa.selenium.*` (internal drivers, json wire, netty client)
  - `jdk.internal.*`, `java.lang.reflect.*`
  - `org.springframework.*`, `jakarta.servlet.*`
- Retain:
  - User test step text (e.g. `Click the "Add to Cart" button`)
  - Target locator (e.g. `By.cssSelector: button.btn-checkout`)
  - Current browser URL at failure
  - Immediate failing DOM element HTML snippet
- **Cap**: Strict max 1,500 characters per failure.

#### Adaptive Reporting Payload (`AdaptivePayloadReporter`):
Every response returned by `get_status` and `test://runs/{runId}/summary` conforms to:
```json
{
  "runId": "run_20260930_101500",
  "status": "FAILED",
  "metrics": {
    "total": 300,
    "passed": 100,
    "failed": 200,
    "skipped": 0,
    "durationMs": 1420000
  },
  "summary": "200 failures detected across 2 distinct root causes. Failures have been clustered below.",
  "failureClusters": [
    {
      "category": "DATABASE_CONNECTION_TIMEOUT",
      "count": 180,
      "sampleMessage": "Could not obtain JDBC Connection; nested exception is java.sql.SQLException: Connection timed out",
      "affectedSuites": ["com.company.tests.checkout.*", "com.company.tests.user.*"],
      "representativeFailure": "<distilled 1,500 char trace>"
    },
    {
      "category": "ELEMENT_NOT_FOUND",
      "count": 20,
      "sampleMessage": "Cannot locate element: By.id('submit-btn')",
      "affectedSuites": ["com.company.tests.cart.*"],
      "representativeFailure": "<distilled 1,500 char trace>"
    }
  ],
  "nextSteps": "Use tool 'get_test_failures(runId=\"run_20260930_101500\", category=\"ELEMENT_NOT_FOUND\", page=1)' to inspect detailed stack traces for specific clusters."
}
```

#### Dynamic Context-Aware `nextSteps`:
- **When Failures $\le 5$**:
  `"All failure details are included inline above. No further inspection tools required."`
- **When Clustered ($> 5$ failures, $\le 5$ clusters)**:
  `"Use tool 'get_test_failures(runId=\"run_20260930_101500\", category=\"<CATEGORY>\", page=1)' to inspect detailed stack traces for specific clusters."`
- **When High Entropy ($> 5$ clusters)**:
  `"High failure entropy detected (87 distinct root causes). Detailed error causes omitted to preserve context window. Use tool 'get_test_failures(runId=\"run_20260930_101500\", page=1)' to inspect failures page-by-page, or read resource 'test://runs/run_20260930_101500/summary'."`

---

## 4. Step-by-Step Task Checklist

### Phase 1: Setup & Dependencies
- [ ] Add `org.springframework.ai:spring-ai-starter-mcp-server-webmvc:2.0.0` dependency to `aura-manager/pom.xml`.
- [ ] Configure `application.properties` with MCP settings (`spring.ai.mcp.server.protocol=STREAMABLE`, `neodymium.aura.mcp.enabled=true`).
- [ ] Verify clean compilation of `aura-manager` and `neodymium-core`.

### Phase 2: Security, Allowlist & Session Management
- [ ] Implement `AuraMcpProperties` to bind `neodymium.aura.mcp.*` settings.
- [ ] Implement `McpSecurityInterceptor` to enforce Bearer token authentication and `ROLE_MCP_READ` / `ROLE_MCP_EXECUTE`.
- [ ] Implement `HostAllowlistValidator` to block SSRF attempts and path traversal outside the playbook directory.
- [ ] Implement `TwoStepConfirmationManager` with TTL-backed token cache for destructive operations (`delete_test`, `cancel_run`).
- [ ] Implement `McpSubscriptionManager` with 45s keep-alive ping framing and 120s timeout.

### Phase 3: Error Distillation & Adaptive Reporting
- [ ] Implement `McpExecutionCoordinator` (FIFO run queue manager, sequential test run dispatcher, and adapter for `AuraQueueService`).
- [ ] Implement `SeleniumErrorDistiller` with frame filtering, locator extraction, and 1,500-char cap.
- [ ] Implement `AdaptivePayloadReporter` with inline ($\le 5$), clustering ($> 5$), high entropy spillover notice, and universal `metrics` envelope.
- [ ] Create DTOs: `RunStatusResponseDto`, `FailureClusterDto`, `ConfirmationTokenDto`.

### Phase 4: Core MCP Tools & Resources
- [ ] Implement `AuraMcpToolService` exposing `@McpTool` methods:
  - [ ] `list_tests` (fast inspection)
  - [ ] `run_test` (non-blocking dispatch $<100\text{ms}$ to `AuraQueueService`)
  - [ ] `get_status` (universal metrics & adaptive reporting)
  - [ ] `get_test_failures` (paginated token-safe drilled-down failure inspection)
  - [ ] `create_test` (prompt syntax validation prior to save)
  - [ ] `delete_test` (two-step handshake guardrail)
  - [ ] `cancel_run` (two-step handshake guardrail & safe process abort mapping to `runId`)
- [ ] Implement `AuraMcpResourceService` exposing `@McpResource`:
  - [ ] `test://runs/{runId}/summary` (compact distilled markdown/JSON)
  - [ ] `test://playbooks/{path}` (raw playbook definition)

### Phase 5: Aura Manager Settings UI Integration
- [ ] Add **MCP Server** tab to `settings-modal.html`.
- [ ] Add input fields for MCP toggle, port/URL display, token generator, and client connection JSON snippet.
- [ ] Update `AuraTestSettingsController` and `dashboard-settings.js` to persist MCP configuration.

### Phase 6: Automated Testing & Verification
- [ ] Write unit tests for `SeleniumErrorDistillerTest` (frame stripping, character cap).
- [ ] Write unit tests for `AdaptivePayloadReporterTest` (inline, clustered, high entropy spillover).
- [ ] Write integration test `AuraMcpToolServiceTest` using existing test LLM mock (`MockLlmProvider` / `BaseLlmTest`).
- [ ] Verify protocol handshake and tools against `@modelcontextprotocol/inspector`.
- [ ] Perform Mandatory Post-Edit Clean Code Audit (zero unused imports, zero unused variables, pragmatic `final`, license headers).

---

## 5. Edge Cases, Risks & Verification

### 5.1 Edge Cases & Mitigation
1. **Queue Contention / Multiple Dispatches**:
   - *Risk*: Multiple agents trigger `run_test` concurrently.
   - *Mitigation*: `run_test` checks `AuraQueueService.isRunningQueue()`. If busy, it safely enqueues the request and returns status `ENQUEUED` with queue position, preventing concurrent browser process collisions.
2. **Client Abrupt Disconnect**:
   - *Risk*: Broken socket causes unhandled exception in server.
   - *Mitigation*: `McpSubscriptionManager` catches `IOException` / socket disconnects and evicts the listener cleanly. Test execution runs uninterrupted in the background.
3. **High-Entropy Failure Storm**:
   - *Risk*: 100 tests fail with 100 distinct random error strings, causing clustering to produce 100 clusters.
   - *Mitigation*: Hard cap of 5 clusters. When $> 5$ distinct clusters occur, cluster details are omitted from the inline payload and the agent is directed to inspect `test://runs/{runId}/summary`.
4. **Malicious / Hallucinated File Deletion**:
   - *Risk*: LLM attempts to delete `src/main/java` or system files.
   - *Mitigation*: Strict path validation confines deletion strictly to `.yaml` files in the test directory; two-step confirmation token prevents unprompted execution.

### 5.2 Verification & Linting Steps
- **Compilation**: `mvn clean compile -DskipTests` (must build cleanly on JDK 21).
- **Targeted Test Execution**: `mvn test -Dtest=SeleniumErrorDistillerTest,AdaptivePayloadReporterTest,AuraMcpToolServiceTest`.
- **Clean Code Audit**:
  - No inline FQCNs.
  - Zero unused imports and variables.
  - Pragmatic `final` modifiers on method arguments and immutable local variables.
  - AGPLv3 header on all newly created files.

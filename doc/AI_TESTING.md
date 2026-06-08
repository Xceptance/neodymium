# Neodymium Aura AI: Advanced Offline Mocking & Programmatic Testing Guide

This document serves as the comprehensive manual for test developers and framework maintainers testing **Neodymium Aura AI** features. It explains how to verify AI execution behavior, test data resolutions, visual escalations, and retry loops 100% offline, deterministically, and with zero LLM API costs.

---

## 1. Core Testing Philosophy: Determinism & Virtualization

Automated tests validating complex AI agent loops (such as retries, self-healing, context escalations, and variable lookups) can be flaky, slow, and expensive if they rely on live external LLM network APIs. 

To solve this, Neodymium Aura AI provides **total test virtualization** using standard object-oriented subclassing and pluggable constructor dependency injection (DI). The core production AI framework remains 100% unaware of any testing or mocking context (zero `if (testMode)` branches):

1. **Offline LLM Stand-In (`MockLlmClient`)**: Extends production `LlmClient` to intercept all chat and visual requests browserlessly, serving a deterministic queue of pre-configured `AiMockResponse` behaviors.
2. **Page Context Stand-In (`MockPageAnalyzer`)**: Extends production `PageAnalyzer` to return canned HTML DOM contexts and static mock screenshots offline without needing a browser.
3. **Action Interceptor (`MockActionExecutor`)**: Extends production `ActionExecutor` to intercept, record, and block Selenium/WebDriver actions, preventing Selenide from opening a physical browser.

A virtual mock test is initiated by constructing an `AiBrowser` with these three mock implementations and registering it via `Neodymium.setAiBrowser(...)`. When the test completes, closing `AiBrowser` automatically releases all mock references, preventing thread-local pollution or memory leaks.

To eliminate repetitive setup and teardown boilerplate in every test, you should extend the abstract base class **[BaseAiOfflineTest](../src/test/java/com/xceptance/neodymium/ai/BaseAiOfflineTest.java)**. It automatically:
- Manages backing up and restoring system properties.
- Configures API key credentials and disables PESAP offline.
- Exposes pre-constructed and registered mock stand-ins (`llmClient`, `pageAnalyzer`, `actionExecutor`, `mockBrowser`) directly to subclasses.

---

## 2. Inspecting the `AiExecutionResult` Return Object

Every call to `Neodymium.ai().execute(...)` returns an instance of `AiExecutionResult`. If an execution throws an exception and fails, you can retrieve the last result via `Neodymium.getLastAiExecutionResult()`.

The `AiExecutionResult` provides programmatic, thread-safe access to every detail of the execution:

### A. Step-by-Step Instruction Dissection (`StepDetails`)
Access the sequence of raw and expanded steps, timing, and static analysis (PESAP) outputs:
* `String getRawInstruction()`: The original string containing template variables (e.g. `"Type ${userEmail}"`).
* `String getExpandedInstruction()`: The fully-resolved string after looking up variables (e.g. `"Type user@test.com"`).
* `long getDurationMs()`: How long this specific step took to execute.
* `String getFailureReason()`: Exception message if the step execution encountered an error.
* `ContextLevel getPesapPredictedContextLevel()`: The context level predicted by PESAP during classification.
* `List<String> getPesapWarnings()`: Semantic linter warnings raised on the step syntax.

### B. Deep Variable Lookup Auditing (`LookupDetails`)
Verify precisely where and how template placeholders resolved:
```java
final List<LookupDetails> lookups = result.getLookups();
for (final LookupDetails lookup : lookups)
{
    System.out.println("Key: " + lookup.getKey());
    System.out.println("Resolved Value: " + lookup.getResolvedValue());
    System.out.println("Source: " + lookup.getSource()); // "TestData Map", "JSONPath Query", "Localization File", "Neodymium Configuration", or "Not Found"
}
```

### C. Context Level Escalation Logs (`EscalationDetails`)
Assert that the AI correctly escalates visual depth when encountering errors:
* `ContextLevel getFromLevel()`: Initial context (e.g. `AXTREE` or `VISUAL_LEAN`).
* `ContextLevel getToLevel()`: Escalated target context.
* `boolean isLlmRequested()`: True if the LLM explicitly requested it via `"status": "ESCALATE"`. False if triggered by a WebDriver/assertion error.
* `String getReason()`: The detailed reason or the underlying Selenium error message.

### D. Raw LLM Call & Vision Insights (`LlmCallDetails`)
Inspect exact prompt and vision screenshot data sent over the wire:
* `String getSystemPrompt()` / `String getUserPrompt()`: Prompts compiled by the agent.
* `String getDomContext()`: Full HTML string sent to the LLM.
* `String getBase64Screenshot()`: Full Base64 PNG image context if Vision was active.
* `TokenUsage getTokens()`: Input, output, cached input, and total tokens.
* `Throwable getException()`: Network exception or HTTP body detail if the call failed.

---

## 3. Simulating the LLM Offline using `AiMockResponse`

To test AI execution without making network calls, register a `MockLlmClient` and enqueue a sequence of pre-configured `AiMockResponse` behaviors.

### Prerequisites for Offline Testing
Production `AiAgent.execute()` reloads configuration settings and asserts that a non-blank AI API key is present, failing the test immediately if it's missing. Additionally, the pre-execution PESAP static analyzer is enabled by default and sends extra LLM queries to classify and lint steps.

Before running a browserless mock test, you **must** configure these system properties so the validation guards pass and PESAP does not make unwanted network calls:
```java
// Sets a dummy mock key to satisfy the API key guard
System.setProperty("neodymium.ai.apiKey", "mock-offline-key");

// Disables default PESAP static analysis LLM calls
System.setProperty("neodymium.ai.pesap.enabled", "false");
```

Alternatively, you can call the convenience helper `MockLlmClient.configureForOffline()` which sets these system properties automatically.

### The `AiMockResponse` Fluent Builder
An `AiMockResponse` represents a single mock response behavior from the LLM. You can configure:
* **The JSON Payload**: The mock action structure the LLM would have returned. Neodymium's parsing protocol uses abbreviated JSON keys to minimize prompt token overhead:
  * **Main response keys**:
    * `s` (success): `boolean` indicating if the step resolved successfully.
    * `r` (reasoning): `String` capturing the LLM's thought process.
    * `a` (actions): `Array` of proposed browser action objects.
    * `d` (done): `boolean` indicating if all instructions are complete.
    * `st` (status override): `String` status code (e.g. `"ESCALATE"`).
    * `tc` (target context): `String` context level (e.g. `"VISUAL"`).
  * **Action object keys (inside the `a` array)**:
    * `t` (type): `String` action type (e.g. `"CLICK"`, `"TYPE"`, `"SELECT"`).
    * `tg` (target): `String` target element descriptor (e.g. CSS selector).
    * `v` (value): `String` input parameter value (e.g. text to type).
    * `desc` (description): `String` human-readable instruction description mapped to the test script (fallback key `"d"` is also supported for backwards compatibility).
    * `ed` (element details): `String` description of target element (recommended especially if target is dynamic/referenced).
* **Latency Timing**: A simulated network delay (`withDelay`).
* **HTTP Errors**: Simulated server error status codes (e.g. HTTP 503) or exceptions to test self-healing retry rules.
* **Tokens**: Custom token counts for testing budget limits.

```java
// Simulates a successful click action response
AiMockResponse.builder()
    .responseText(
        """
        {
          "s": true,                                    // s  = success (boolean)
          "r": "Success",                               // r  = reasoning/thought process (String)
          "a": [{"t": "CLICK", "tg": "#submit-btn", "desc": "Click submit button"}],    // a  = actions array, t = type, tg = target, desc = description
          "d": true                                     // d  = done (boolean)
        }
        """)
    .build();

// Simulates a 503 Service Unavailable HTTP error
AiMockResponse.builder()
    .httpStatusCode(503)
    .build();

// Simulates a custom exception (e.g. connection timeout)
AiMockResponse.builder()
    .exception(new RuntimeException("Connection timed out"))
    .build();
```

---

### 4. Practical JUnit Test Recipes

All examples are written using aggressive `final` modifiers, Allman brace style, JDK 21 features, and explicit imports, strictly adhering to the Neodymium coding standards.

### Recipe 1: Asserting Retry & Self-Healing on HTTP 503 Errors
Validate that Neodymium correctly logs communication errors, handles retry rules, and succeeds once the service returns.

```java
// AI-generated: Gemini 2.5 Flash
// GNU AGPLv3 License
package com.xceptance.neodymium.ai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.ai.testing.MockActionExecutor;
import com.xceptance.neodymium.ai.testing.MockLlmClient;
import com.xceptance.neodymium.ai.testing.MockPageAnalyzer;
import com.xceptance.neodymium.util.Neodymium;

public final class AiExecutionResultDemoTest
{
    @Test
    public final void testSelfHealingOnHttp503Error()
    {
        // 1. Satisfy the API key guard and disable PESAP for offline execution
        MockLlmClient.configureForOffline();

        // 2. Set up the virtual LLM queue: a 503 failure, followed by a clean action success response
        final MockLlmClient llmClient = new MockLlmClient();
        llmClient.addResponse(AiMockResponse.builder()
                .httpStatusCode(503)
                .delayMs(100L)
                .build());
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [{"t": "CLICK", "tg": "#btn"}],
                      "d": true
                    }
                    """)
                .tokens(150L, 45L)
                .build());

        // 3. Set up page context and action interceptor stand-ins
        final MockPageAnalyzer pageAnalyzer = new MockPageAnalyzer();
        final MockActionExecutor actionExecutor = new MockActionExecutor();

        // 4. Construct and register the virtual browser via dependency injection
        try (final AiBrowser mockBrowser = new AiBrowser(Neodymium.aiConfiguration(), this, llmClient, pageAnalyzer, actionExecutor))
        {
            Neodymium.setAiBrowser(mockBrowser);

            // 5. Execute
            final AiExecutionResult result = mockBrowser.execute("Click on the blue button");

            // 6. Verify self-healing occurred
            Assertions.assertTrue(result.isSuccess());
            Assertions.assertEquals(1, result.getEscalationCount());
            Assertions.assertEquals(0, result.getRetryCount());
            Assertions.assertEquals(2, result.getLlmCalls().size());
            
            // Assert that the first call captured the 503 error details
            Assertions.assertNotNull(result.getLlmCalls().getFirst().getErrorMessage());
            Assertions.assertTrue(result.getLlmCalls().getFirst().getErrorMessage().contains("503"));
        }
    }
}
```

### Recipe 2: Asserting Context Level Escalations
Validate that if the SUT layout shifts or elements are obstructed under `STANDARD` view, the engine correctly escalates to `VISUAL` context and executes the final action.

```java
// AI-generated: Gemini 2.5 Flash
// GNU AGPLv3 License
package com.xceptance.neodymium.ai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.ai.testing.MockActionExecutor;
import com.xceptance.neodymium.ai.testing.MockLlmClient;
import com.xceptance.neodymium.ai.testing.MockPageAnalyzer;
import com.xceptance.neodymium.util.Neodymium;

public final class AiEscalationTest
{
    @Test
    public final void testVisualEscalationVerification()
    {
        MockLlmClient.configureForOffline();

        final MockLlmClient llmClient = new MockLlmClient();
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "st": "ESCALATE",
                      "r": "Elements overlap in layout",
                      "tc": "VISUAL",
                      "a": [],
                      "d": false
                    }
                    """)
                .build());
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [{"t": "CLICK", "tg": "#menu"}],
                      "d": true
                    }
                    """)
                .build());

        final MockPageAnalyzer pageAnalyzer = new MockPageAnalyzer();
        final MockActionExecutor actionExecutor = new MockActionExecutor();

        try (final AiBrowser mockBrowser = new AiBrowser(Neodymium.aiConfiguration(), this, llmClient, pageAnalyzer, actionExecutor))
        {
            Neodymium.setAiBrowser(mockBrowser);

            final AiExecutionResult result = mockBrowser.execute("Click the Menu button");

            Assertions.assertTrue(result.isSuccess());
            Assertions.assertEquals(1, result.getEscalationCount());
            Assertions.assertEquals(2, result.getLlmCalls().size());
            
            // Assert the escalation details
            Assertions.assertEquals(1, result.getEscalations().size());
            Assertions.assertTrue(result.getEscalations().getFirst().isLlmRequested());
            Assertions.assertTrue(result.getEscalations().getFirst().getReason().contains("Elements overlap"));
        }
    }
}
```

### Recipe 3: Intercepting and Verifying Action Sequence Order
Use `MockActionExecutor` to assert that correct Selenium/WebDriver interactions are performed by the execution loop in the correct relative sequence, without running real browsers.

```java
// AI-generated: Gemini 2.5 Flash
// GNU AGPLv3 License
package com.xceptance.neodymium.ai;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.ai.testing.MockActionExecutor;
import com.xceptance.neodymium.ai.testing.MockLlmClient;
import com.xceptance.neodymium.ai.testing.MockPageAnalyzer;
import com.xceptance.neodymium.util.Neodymium;

public final class AiActionSequenceTest
{
    @Test
    public final void testBrowserActionSequenceVerification()
    {
        MockLlmClient.configureForOffline();

        final MockLlmClient llmClient = new MockLlmClient();
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [{"t": "CLICK", "tg": "#input-field"}],
                      "d": false
                    }
                    """)
                .build());
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [{"t": "TYPE", "tg": "#input-field", "v": "Demo"}],
                      "d": true
                    }
                    """)
                .build());

        final MockPageAnalyzer pageAnalyzer = new MockPageAnalyzer();
        final MockActionExecutor actionExecutor = new MockActionExecutor();

        try (final AiBrowser mockBrowser = new AiBrowser(Neodymium.aiConfiguration(), this, llmClient, pageAnalyzer, actionExecutor))
        {
            Neodymium.setAiBrowser(mockBrowser);

            mockBrowser.execute("Enter 'Demo' into input field");

            final List<Action> actionLog = actionExecutor.getExecutedActions();
            Assertions.assertEquals(2, actionLog.size());
            Assertions.assertTrue(actionLog.get(0).toString().contains("CLICK"));
            Assertions.assertTrue(actionLog.get(1).toString().contains("TYPE"));
            Assertions.assertTrue(actionLog.get(1).toString().contains("Demo"));
        }
    }
}
```

### Recipe 4: Asserting Template Variable Lookups
Assert that instructions containing dynamic placeholders resolve variables from the correct, authorized scope sources.

> [!NOTE]
> **Resolution Precedence & Case Sensitivity:**
> Template variables/placeholders (e.g., `${username}`) are resolved **case-insensitively**. However, if there are multiple keys in the `TestData` map differing only by case (e.g. both `username` and `userName` are defined), Neodymium prioritizes the **exact case-sensitive match** first, falling back to a case-insensitive match only if no exact match is found.

```java
// AI-generated: Gemini 2.5 Flash
// GNU AGPLv3 License
package com.xceptance.neodymium.ai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.LookupDetails;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.ai.testing.MockActionExecutor;
import com.xceptance.neodymium.ai.testing.MockLlmClient;
import com.xceptance.neodymium.ai.testing.MockPageAnalyzer;
import com.xceptance.neodymium.util.Neodymium;

public final class AiTestDataVariableTest
{
    @Test
    public final void testTestDataVariableResolutionScope()
    {
        MockLlmClient.configureForOffline();

        // Set up test data variables in Neodymium
        Neodymium.getData().put("accountEmail", "user@neodymium.com");

        final MockLlmClient llmClient = new MockLlmClient();
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [],
                      "d": true
                    }
                    """)
                .build());

        final MockPageAnalyzer pageAnalyzer = new MockPageAnalyzer();
        final MockActionExecutor actionExecutor = new MockActionExecutor();

        try (final AiBrowser mockBrowser = new AiBrowser(Neodymium.aiConfiguration(), this, llmClient, pageAnalyzer, actionExecutor))
        {
            Neodymium.setAiBrowser(mockBrowser);

            // Execute instruction containing placeholder
            final AiExecutionResult result = mockBrowser.execute("Type '${accountEmail}' into email input");

            Assertions.assertTrue(result.isSuccess());
            Assertions.assertEquals(1, result.getLookups().size());
            
            final LookupDetails lookup = result.getLookups().getFirst();
            Assertions.assertEquals("accountEmail", lookup.getKey());
            Assertions.assertEquals("user@neodymium.com", lookup.getResolvedValue());
            Assertions.assertEquals("TestData Map", lookup.getSource()); // Asserts it resolved from standard test data scope
        }
    }
}
```

---

## 5. Local Real-Browser Sandbox Testing

For tests that *must* run inside a real browser to scan visual anomalies, contrast scales, or structural pixel variations, use the **Aura Test Suite Hub** locally:

### 1. The Sandbox Resource Folder
The testing pages reside under:
`src/test/resources/ai-test-pages/AuraGlanceTest/`
* **Sub-Apps**: Contains distinct page templates (SaaS `dashboard/`, E-Commerce `shop/`, Accessibility `a11y/`).
* **Escalation Challenge Pages**:
  - `shop/escalation.html`: Simulates context level escalation from **AXTREE to LEAN** (clicking a custom span link or custom div button, and typing into an input field inside an `aria-hidden` container) and **LEAN to STANDARD** (verifying plain text paragraph content containing the token `AURA-9921-SECURE`).
  - `shop/visual-escalation.html`: Simulates context level escalation from **STANDARD to VISUAL** (verifying text drawn inside a `<canvas>` element or injected via CSS `::after` content rule).
* **Unified Scenario Playground (`shop/sandbox/`)**:
  - Contains standalone sandbox pages testing: SVG-only icon buttons (`svg-icons.html`), coordinate clicks (`canvas-click.html`), Shadow DOM (`shadow-dom.html`), click interception overlays (`click-intercept.html`), input timing reveal (`dynamic-reveal.html`), sequential hover chains (`hover-chain.html`), AJAX table sorting (`table-sorting.html`), scroll list overflow (`scroll-list.html`), floating labels visual overlap (`floating-labels.html`), cross-origin iframes (`cross-origin-iframe.html`), and mock OAuth redirects (`mock-oauth-login.html`). Verified in JUnit 5 integration tests by extending `BaseAiTest`.
* **Aura Chaos Panel**: Click the bottom-right gear trigger icon to dynamically inject layout shifts, contrast bugs, and overlaps directly into the live browser DOM.

### 2. Embedded HTML Server
Tests extending `BaseAiTest` spin up a parallel HTTP + secure HTTPS server dynamically on random free ports (`EmbeddedHtmlServer.java`). This guarantees that your tests can execute visual checks locally on a real browser rendering canvas under zero network or port conflicts.

---

## 6. How to Run Aura AI Integration Tests

To run the automated visual regression and parameterization suites locally:

```bash
# 1. Compile all test cases
mvn test-compile

# 2. Run all local visual matrix and glance tests
mvn test -Pjunit-5 -Dtest=com.xceptance.neodymium.ai.Aura*Test
```

> [!TIP]
> Use standard IDE support (Eclipse, IntelliJ, VS Code) to run `AuraGlanceTest.java` directly. The embedded server starts and stops automatically inside the JUnit setup lifecycle!

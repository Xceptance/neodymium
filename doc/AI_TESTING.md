# Neodymium Aura AI v2: Offline Mocking & JUnit 5 Integration Testing Guide

This document serves as the comprehensive manual for test developers and framework maintainers testing **Neodymium Aura AI (v2)** features. It explains how to build, run, and verify AI playbook executions, test data resolutions, visual context escalations, self-healing, and event listeners 100% offline, deterministically, and with zero external LLM API costs.

---

## 1. Neodymium AI v2 Architecture Overview

Neodymium Aura AI v2 transitions the engine from procedural loops to a **decoupled, event-driven state machine architecture**:

* **JUnit 5 Extension (`NeodymiumAiRunner`)**: The `@NeodymiumAiTest` class annotation wires a JUnit 5 `TestTemplateInvocationContextProvider` that loads playbooks, iterates datasets, manages session lifecycles, and manages recordings.
* **Orchestration (`StateMachineRunner` & `AiSession`)**: `AiSession` creates and manages the pipeline context (`ExecutionContext`). The `StateMachineRunner` drives pipeline states (`ResolveStepState`, `ExecuteActionsStep`, `VerifyOutcomeStep`, `SelfHealingStep`) in a deterministic, thread-isolated loop.
* **Target Isolation (`TargetExecutor`)**: Decouples physical browser interactions (`SelenideTargetExecutor`) from testing stand-ins (`MockTargetExecutor`).
* **LLM Provider Abstraction (`LlmProvider`)**: Interfaces model communications (`GeminiLlmProvider`, `VertexAiProvider`, `MockLlmProvider`).
* **Yaml Playbooks (`YamlPlaybookParser`)**: Playbooks are stored as human-readable `.yaml` files alongside `.recording.json` companion files for millisecond offline replays.
* **Event-Driven Bus (`ExecutionEventBus`)**: Real-time event notifications (`StepStartedEvent`, `ActionExecutedEvent`, `StepFinishedEvent`, `LlmRequestSentEvent`) dispatch to recorder, HUD, and Allure reporting listeners.

---

## 2. JUnit 5 Annotation Model

The v2 framework provides standard annotations for playbook-driven test cases:

| Annotation | Scope | Purpose |
| :--- | :--- | :--- |
| **`@NeodymiumAiTest`** | Class | Marks a test class as an AI playbook test class. Wires `NeodymiumAiRunner`. Resolves `ClassName.yaml` by default. |
| **`@AiPlaybook`** | Method | Marks a method as playbook-driven. Allows overriding the playbook YAML path. |
| **`@AiMode`** | Class / Method | Sets the execution mode (`RECORD`, `REPLAY_ONLY`, `REPLAY_AND_FIX`, `LLM_ONLY`). |
| **`@AiDataSet`** | Class / Method | Filters datasets by `testId` using exact string matches or regex patterns. |
| **`@AiSelenide`** | Class / Method | Specifies browser profile configuration settings. |

### Available Execution Modes

* **`REPLAY_AND_FIX` (Default)**: If a `.recording.json` companion file exists on disk, steps replay offline in milliseconds. On element divergence, the engine invokes the LLM to self-heal and updates the companion file.
* **`RECORD`**: Always invokes the LLM to execute steps and writes a new `.recording.json` baseline.
* **`REPLAY_ONLY`**: Strict CI regression mode. Replays strictly from disk recording and fails hard if the SUT diverges (0 LLM network calls).
* **`LLM_ONLY`**: Exploratory mode. Calls the LLM on every step without reading or writing companion recordings.

---

## 3. Total Test Virtualization (`MockLlmProvider` & `MockTargetExecutor`)

Automated unit/integration tests validating complex AI loops (retries, self-healing, context escalation) must execute deterministically without live LLM network calls or physical browser launches.

Neodymium v2 achieves **total virtualization** via constructor dependency injection:

1. **`MockLlmProvider`**: Implements `LlmProvider` to serve a queued list of pre-configured synthetic LLM JSON responses or simulated HTTP exceptions.
2. **`MockTargetExecutor`**: Implements `TargetExecutor` to intercept, log, and validate actions browserlessly without opening a physical browser window.

---

## 4. Practical JUnit 5 Test Recipes

### Recipe 1: Standard Convention-Based Playbook Test

```java
// AI-generated: Gemini 3.6 Flash
// GNU AGPLv3 License / MIT
package com.xceptance.neodymium.ai;

import org.junit.jupiter.api.Test;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.AiDataSet;

// Automatically loads ShopTest.yaml alongside this test class
@NeodymiumAiTest
public final class ShopTest
{
    // Runs against all datasets in ShopTest.yaml
    @Test
    public final void fullCheckout()
    {
    }

    // Overrides dataset selection: runs only "premium" dataset
    @Test
    @AiPlaybook
    @AiDataSet("premium")
    public final void premiumCheckout()
    {
    }

    // Overrides playbook YAML file
    @Test
    @AiPlaybook("playbooks/custom-login.yaml")
    public final void customLoginFlow()
    {
    }
}
```

---

### Recipe 2: Virtual State Machine Test with `MockLlmProvider` & `MockTargetExecutor`

Validate state machine execution logic, step resolution, and action logs completely offline:

```java
// AI-generated: Gemini 3.6 Flash
// GNU AGPLv3 License / MIT
package com.xceptance.neodymium.ai;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.executor.Action;
import org.neodymium.ai.runner.StateMachineRunner;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.session.ExecutionMode;
import org.neodymium.ai.testing.MockLlmProvider;

public final class VirtualStateMachineTest
{
    @Test
    public final void testOfflineStepExecution() throws Exception
    {
        // 1. Create mock LLM provider with synthetic JSON response
        final MockLlmProvider llmProvider = new MockLlmProvider();
        llmProvider.enqueueResponse("""
            {
              "s": true,
              "r": "Clicking login button",
              "a": [{"t": "CLICK", "tg": "#login-btn", "desc": "Click login button"}],
              "d": true
            }
            """);

        // 2. Create mock target executor
        final MockTargetExecutor targetExecutor = new MockTargetExecutor();

        // 3. Create thread-isolated session in RECORD mode
        final AiSession session = AiSession.builder()
            .executionMode(ExecutionMode.RECORD)
            .llmProvider(llmProvider)
            .targetExecutor(targetExecutor)
            .build();

        // 4. Run StateMachineRunner
        final StateMachineRunner runner = new StateMachineRunner(session);
        final boolean success = runner.run("Click the login button");

        // 5. Assert execution outcomes
        Assertions.assertTrue(success);

        final List<Action> executedActions = targetExecutor.getExecutedActions();
        Assertions.assertEquals(1, executedActions.size());
        Assertions.assertEquals("CLICK", executedActions.get(0).getType());
        Assertions.assertEquals("#login-btn", executedActions.get(0).getTarget());
    }
}
```

---

### Recipe 3: Testing Event Bus Listeners (`ExecutionEventBus`)

Verify that real-time execution events dispatch correctly to audit, reporting, and recording listeners:

```java
// AI-generated: Gemini 3.6 Flash
// GNU AGPLv3 License / MIT
package com.xceptance.neodymium.ai;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.event.ExecutionEvent;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;

public final class EventBusTest
{
    @Test
    public final void testEventDispatching()
    {
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final List<ExecutionEvent> receivedEvents = new ArrayList<>();

        // Register custom event listener
        eventBus.register(receivedEvents::add);

        // Dispatch events
        eventBus.post(new StepStartedEvent("step-1", "Click login button"));
        eventBus.post(new StepFinishedEvent("step-1", "Click login button", true, 42L));

        // Assert event delivery
        Assertions.assertEquals(2, receivedEvents.size());
        Assertions.assertTrue(receivedEvents.get(0) instanceof StepStartedEvent);
        Assertions.assertTrue(receivedEvents.get(1) instanceof StepFinishedEvent);
    }
}
```

---

### Recipe 4: Replay & Self-Healing Testing (`REPLAY_AND_FIX`)

Test that cached `.recording.json` companions replay offline, and self-heal automatically when element locators diverge:

```java
// AI-generated: Gemini 3.6 Flash
// GNU AGPLv3 License / MIT
package com.xceptance.neodymium.ai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.runner.StateMachineRunner;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.session.ExecutionMode;
import org.neodymium.ai.testing.MockLlmProvider;

public final class SelfHealingTest
{
    @Test
    public final void testSelfHealingOnLocatorDivergence() throws Exception
    {
        // Enqueue fallback response for self-healing step
        final MockLlmProvider llmProvider = new MockLlmProvider();
        llmProvider.enqueueResponse("""
            {
              "s": true,
              "r": "Updated selector to #login-submit-v2",
              "a": [{"t": "CLICK", "tg": "#login-submit-v2", "desc": "Click updated login button"}],
              "d": true
            }
            """);

        // Target executor configured to simulate stale element failure on initial selector
        final MockTargetExecutor targetExecutor = new MockTargetExecutor();
        targetExecutor.setFailOnTarget("#old-login-btn");

        final AiSession session = AiSession.builder()
            .executionMode(ExecutionMode.REPLAY_AND_FIX)
            .llmProvider(llmProvider)
            .targetExecutor(targetExecutor)
            .build();

        final StateMachineRunner runner = new StateMachineRunner(session);
        final boolean result = runner.run("Click submit button");

        Assertions.assertTrue(result);
    }
}
```

---

## 5. Local Real-Browser Sandbox Testing (`Aura Test Suite Hub`)

For tests requiring a physical browser engine (Chrome, Firefox, Safari) to scan layout grids, test CSS contrast, or verify shadow DOM boundaries:

### Sandbox Directory Layout
Sandbox assets reside in the classpath under:
`src/test/resources/ai-test-pages/AuraGlanceTest/`

* **Dashboard & Shop Apps**: `dashboard/` (SaaS administration) and `shop/` (Apparel storefront with `homepage-perfect.html`, `homepage-normal.html`, `homepage-bad.html`).
* **Scenario Playground (`shop/sandbox/`)**: Isolated challenge pages testing SVG-only buttons (`svg-icons.html`), canvas clicks (`canvas-click.html`), Shadow DOM (`shadow-dom.html`), z-index click interception (`click-intercept.html`), AJAX table sorting (`table-sorting.html`), and cross-origin iframes (`cross-origin-iframe.html`).

### Embedded Server (`EmbeddedHtmlServer`)
Tests extending `BaseAiTest` spin up an embedded HTTP + HTTPS server automatically on free random ports via `EmbeddedHtmlServer.java`. The server loads `keystore.p12` for local SSL testing and shuts down automatically after test execution.

---

## 6. How to Run AI Tests via Maven

Execute the hermetic test suite via Maven:

```bash
# Compile source and test classes
mvn test-compile

# Run hermetic JUnit 5 unit & state machine suite
mvn test

# Run live integration tests (requires network & API keys configured)
mvn test -PLiveAPI
```

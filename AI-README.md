# Neodymium AI: Native Language Automation

Welcome to the **AI integration** for Neodymium! 

Neodymium AI introduces a paradigm shift in test automation: **Native Language Automation**. Instead of writing brittle Selenium/Selenide selectors and page objects, you can now write your test instructions in plain, natural English. Neodymium AI parses your intent, analyzes the application's UI, and executes the necessary browser actions—complete with automatic self-healing and playbook recording.

---

## 🌟 Key Features

1. **Natural Language Execution**: Write tests like `Open the login page.`, `Type 'user' into the username field.`, and `Click Submit.`.
2. **Playbooks (Caching for Speed & Cost)**: When the AI successfully executes a test, it records the exact DOM elements and actions into a JSON "Playbook". Subsequent runs replay the fast, deterministic Selenium actions without calling the LLM, saving time and API costs.
3. **Self-Healing Tests**: If a replay fails (e.g., a button ID changes or the UI is overhauled), the AI agent catches the failure, re-analyzes the live page using the LLM, fixes the test execution dynamically, and updates the Playbook.
4. **Data-Driven Prompts**: Full integration with Neodymium's `@DataFolder` and `TestData`. You can inject datasets directly into your natural language prompts using `${variable}` syntax.
5. **AI Discussion Logging**: Detailed Allure report attachments that show exactly what the LLM "saw", what it "thought" (reasoning), and what actions it decided to take.

---

## 🚀 Getting Started

### 1. Configuration
To use Neodymium AI, you must configure your LLM credentials. Currently, the Google Gemini model via LangChain4j is utilized.

Add the following to your `neodymium.properties`, `ai.properties`, or system environment variables:
```properties
neodymium.ai.apiKey=YOUR_GEMINI_API_KEY
neodymium.ai.model=gemini-2.5-pro # or your preferred model
```

### 2. Writing Your First AI Test

Instead of traditional driver commands, you interact with the `AiBrowser`. 

**Example: Direct Execution**
```java
import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

public class MyAiTest {
    
    @NeodymiumTest
    public void testLoginWithAi() {
        try (AiBrowser ai = new AiBrowser(this)) {
            ai.execute("""
                Open https://example.com.
                Click on the Login link.
                Type 'user@example.com' into the email address field.
                Type 'supersecret' into the password field.
                Click the Log in button.
                Verify that you are logged in successfully.
            """);
        }
    }
}
```

### 3. Data-Driven AI Tests

You can move your instructions into Neodymium TestData (e.g., a YAML file) and run them against multiple datasets.

**`data.yml`**
```yaml
prompt: 'Open ${neodymium.url}.
  Navigate to the login page.
  Enter email "${email}" and password "${password}".
  Click login.
  Verify login was ${expectedResult}.'
  
data:
  - email: "good@user.com"
    password: "correct"
    expectedResult: "successful"
  - email: "bad@user.com"
    password: "wrong"
    expectedResult: "unsuccessful"
```

**`MyDataDrivenAiTest.java`**
```java
@DataFolder("my/test/data")
public class MyDataDrivenAiTest {
    
    @NeodymiumTest
    public void executeAiPrompt() throws Throwable {
        // Automatically picks up the 'prompt' key from the dataset
        // and resolves the ${variables} before sending to the AI.
        Neodymium.ai().execute();
    }
}
```

> **Tip:** You can include comments within your multi-line prompts to annotate test steps without affecting the AI's execution or the generated playbooks. Any line starting with `#` or `//` will be automatically ignored by the agent.

---

## 📥 Native State Capture (Variables)

Neodymium AI supports dynamic runtime variable extraction using the `STORE` action. This allows you to capture text from the application during a test and reuse it in subsequent steps—perfect for verifying totals, tracking generated order IDs, or validating dynamic workflows.

**Example Playbook:**
```yaml
prompt: |
  Capture the line item price shown in the line item row. Save it as variable 'unitPrice'.
  Capture the subtotal amount. Save it as variable 'subtotal'.
  Verify that 'subtotal' is greater than 'unitPrice'.
```

### How It Works
1. **Extraction**: When the LLM encounters a command like `Capture`, `Store`, or `Record`, it automatically receives the full text-context of the page. It locates the target element and extracts its inner text natively.
2. **Storage**: The string value is saved into the execution context for the duration of the current test run.
3. **Interpolation**: You can seamlessly reuse stored variables in later steps using the `${variableName}` syntax or by referring to them by name. The `ActionExecutor` automatically resolves these placeholders in real-time before executing the action.

> **Note:** Variables extracted during runtime using the `STORE` action are scoped to the current test execution. If a runtime variable shares the same name as a static variable injected via `TestData` or `@DataFolder`, the runtime variable takes precedence.

---

## 🔀 Advanced Conditional Logic (AST Branching)

Neodymium AI natively understands conditional logic without requiring rigid syntax or keywords. You can use natural language to express `If... then... else` logic directly in your test instructions.

**Example Instruction:**
> *"If the cookie banner appears, close it. Otherwise, click login."*

### How It Works
The LLM understands your intent and compiles it into an **Abstract Syntax Tree (AST)** using the native `BRANCH` action. 

The Playbook records the entire logical structure (the condition actions, the then actions, and the else actions) seamlessly into the JSON cache. 

### Offline CI/CD Replay
Because the LLM pre-compiles this logic during the initial generation phase, the conditionals run dynamically and deterministically during offline CI/CD replays. The `AiAgent` will test the condition natively against the live DOM and route the execution path **without** needing to contact the LLM again!

> **Best Practice:** While branching is fully supported, simple, deterministic data-driven tests are generally preferred over massive "choose your own adventure" scripts. Keep conditionals focused on dismissing dynamic UI elements (like banners or modals).

---

## 🔧 Programmatic Assertions (`JAVA_METHOD`)

While the AI handles most validations natively through the `ASSERT` action (checking text, visibility, URLs), some validations require **programmatic logic** that an LLM should not be trusted to perform—such as numeric comparisons, mathematical calculations, or complex data transformations. For these cases, Neodymium provides the `JAVA_METHOD` action.

### How It Works

When the AI encounters an instruction like *"Verify the price is greater than 0"*, it emits a `JAVA_METHOD` action targeting a method name (e.g., `assertPriceGreaterThanZero`) with the extracted value as a parameter. The framework then resolves and invokes the method via reflection.

### Method Resolution Strategy

The framework uses a **two-stage fallback** to locate the target method:

1. **Stage 1 — Test Instance**: The framework first searches the active test class for a matching `public` method (instance or static).
2. **Stage 2 — Registered Utility Classes**: If the method is not found on the test class, the framework scans all classes listed in the `neodymium.ai.agent.javaMethod.utilityClasses` configuration property for a matching `public static` method.

This means common assertions work out-of-the-box across all your tests without any boilerplate.

### Built-in Assertions

Neodymium ships with a default utility class, `com.xceptance.neodymium.ai.util.AiAssertions`, which provides common validation methods:

| Method | Description |
|--------|-------------|
| `assertPriceGreaterThanZero(String)` | Validates that a price string (any locale/currency) represents a value > 0 |

These methods are automatically available to the AI in every test class.

### Extending with Custom Utility Classes

To add your own project-specific assertion methods:

1. **Create a utility class** with `public static` methods:
   ```java
   package com.myproject.test.util;

   public final class MyProjectAssertions
   {
       private MyProjectAssertions() {}

       public static void assertDiscountApplied(final String price)
       {
           // your custom validation logic
       }
   }
   ```

2. **Register it** in your `ai.properties` (or `neodymium.properties`):
   ```properties
   # Append your class after the built-in one (comma-separated)
   neodymium.ai.agent.javaMethod.utilityClasses=com.xceptance.neodymium.ai.util.AiAssertions,com.myproject.test.util.MyProjectAssertions
   ```

3. **Use it naturally** in your YAML prompts:
   ```yaml
   prompt: |
     Verify the discount was applied to the displayed price.
   ```
   The AI will emit `JAVA_METHOD: assertDiscountApplied`, and the framework will automatically find it in your registered utility class.

> **Note:** Methods on the test instance always take priority over utility classes. If your test class defines a method with the same name, it will be invoked instead of the utility version. Utility class methods **must** be `public static`.

---

## 🧠 How It Works Under the Hood

1. **The `AiBrowser`**: Wraps the test context and maintains the token/usage stats.
2. **The `AiAgent`**: 
   - Splits your prompt line-by-line.
   - For fast, deterministic commands (like `Navigate to...` or `Go back`), it bypasses the LLM using regex.
   - For UI interactions, it uses the `PageAnalyzer` to extract the DOM context (and optionally a screenshot) and sends it to the LLM.
3. **The LLM Client**: Asks the LLM to locate elements and determine the exact physical actions to perform.
4. **The `ActionExecutor`**: Takes the structured JSON response from the LLM and translates it into physical Selenide/WebDriver interactions (e.g., `ActionType.CLICK`, `ActionType.TYPE`).
5. **Playbooks**: Recorded interactions are saved in `src/test/resources/ai-playbooks`. *You should commit these playbooks to version control.*

---

## 📈 Escalating Context (Token Optimization)

Neodymium AI uses a multi-tier **Escalating Context** strategy to minimize LLM token costs while maximizing reliability. Instead of blindly sending massive HTML dumps to the LLM, the framework progressively reveals more data only when necessary.

There are four context levels:
1. **`HINT` (Zero DOM)**: Triggered automatically if your instruction contains `(hint: ...)`. No DOM is sent. The LLM simply translates the hint into the correct JSON action.
2. **`LEAN` (Interactive Only)**: The default starting level. Sends only interactive elements (buttons, links, inputs) and headings. Text paragraphs are excluded. Covers ~80% of typical actions.
3. **`STANDARD` (Full Text)**: Sends interactive elements plus all visible text content. Required for assertions or disambiguating similar elements (e.g. 5 identical "View Details" links).
4. **`VISUAL` (Screenshot)**: Sends the `STANDARD` DOM alongside a Base64-encoded screenshot of the page. Used as a last resort for complex SVG, canvas, or shadow-DOM interactions.

**How Escalation Works:**
The LLM is a conscious participant in this loop. If it receives a `LEAN` context but cannot figure out what to do, it explicitly returns `{"status": "ESCALATE"}` instead of guessing. The framework catches this, widens the context to `STANDARD`, and tries again without consuming a retry budget. This ensures you only pay for the tokens you actually need!

---

## 📜 Selective Step History (Recovery Context)

During normal execution, each instruction is processed in isolation — the LLM receives only the current instruction, the current DOM, and the SUT context. This keeps prompts lean and token-efficient for the ~80% of steps that succeed on the first attempt.

However, when the agent enters a **recovery scenario** (retry after error, context escalation, or no-actions retry), the framework automatically injects a compact **step history** into the prompt. This gives the LLM awareness of the broader test flow, enabling it to reason about expected page state and disambiguate elements more effectively.

### When History Is Included

| Scenario | History Included? | Rationale |
|---|---|---|
| Direct parse (no LLM call) | ❌ | LLM never called |
| Playbook replay (no LLM call) | ❌ | LLM never called |
| First LLM attempt (happy path) | ❌ | Usually succeeds; low ROI for extra tokens |
| Retry after error | ✅ | LLM needs flow context to reason about expected state |
| Retry after no-actions returned | ✅ | Flow context helps LLM understand what it should do |
| After context escalation | ✅ | LLM is already struggling; more context helps |

### What the History Contains

The history block is a compact numbered list of all **completed** instruction lines (from the Playbook), followed by a `[CURRENT]` marker:

```
## Completed Steps (for context)
1. Open homepage
2. Type "running shoes" into the search field
3. Click the first product result
[CURRENT] → see Instruction above
```

Only the instruction text is included — no DOM snapshots, no action details, no reasoning. This keeps the token overhead minimal (~50-100 tokens per step) while providing the LLM with enough context to understand the test flow.

### How It Works

The history is built by `AiAgentPrompts.buildStepHistory(Playbook)`, which iterates the playbook's completed steps (indices `0` to `cursor - 1`). Steps with null or blank prompt lines are gracefully skipped. The resulting block is injected into the `{historyBlock}` placeholder in the prompt templates (`user-prompt-template.txt`, `retry-prompt-template.txt`, `no-actions-retry-prompt-template.txt`).

An `isRecoveryAttempt` flag in `AiAgent.getActionsFromLLM()` tracks whether the current iteration is a recovery scenario. On the first attempt, the flag is `false` and the history block is empty. It flips to `true` whenever an escalation, error retry, or no-actions retry occurs.

---

## 📊 AI Execution Statistics

After every test run, Neodymium logs a summary of all AI operations. This data is also attached to the Allure report (when `neodymium.ai.attachTokenUsageToReport=true`). Here's how to read it:

```
======== 📊 AI Execution Statistics ========
   LLM calls:        18
   Input tokens:     80463
   Output tokens:    3828
   Total tokens:     84291
   ---
   Context Levels:
     HINT:      0
     LEAN:      12
     STANDARD:  4
     VISUAL:    2
   ---
   Escalations:      6  (LLM: 3, Error: 3)
   Retries:          0  (Error: 0, No-Actions: 0)
   ---
   Replays:          0
   Direct Parses:    0
=============================================
```

### Token Usage

| Metric | Meaning |
|---|---|
| **LLM calls** | Total number of HTTP requests to the LLM API. Each call sends a prompt and receives a response. This is the primary cost driver. |
| **Input tokens** | Tokens sent *to* the LLM (system prompt + user prompt + DOM context). This is typically 95%+ of total tokens since the DOM can be large. |
| **Output tokens** | Tokens received *from* the LLM (the JSON response with actions and reasoning). Usually small (~200-400 tokens per response). |
| **Total tokens** | `Input + Output`. This is what your API billing is based on. |

### Context Level Distribution

Shows how many LLM calls were made at each context level. This directly reflects token efficiency:

| Level | What's sent to the LLM | Typical use |
|---|---|---|
| **HINT** | Zero DOM — only the inline hint locator | Fastest, cheapest. Used when instructions contain `(hint: ...)` |
| **LEAN** | Interactive elements only (buttons, links, inputs, headings) | Default starting level. Handles ~80% of actions |
| **STANDARD** | LEAN + all visible text content (paragraphs, spans, table cells) | Needed for assertions, text verification, disambiguation |
| **VISUAL** | STANDARD + Base64 screenshot | Last resort for complex visual layouts |

**Reading the example:** 12 LEAN + 4 STANDARD + 2 VISUAL = 18 total LLM calls. The majority resolved at LEAN (cheapest), with some needing more context.

### Escalations

Escalations happen when the agent moves to a higher context level. They do **not** consume the retry budget — they're a normal part of the token-optimization protocol.

| Type | Trigger |
|---|---|
| **LLM** | The LLM explicitly returned `{"status": "ESCALATE"}` because it couldn't find the target element or needed more text content to disambiguate |
| **Error** | An action execution failed (e.g., `ElementNotInteractableException`) and the agent automatically escalated before burning a retry |

**Reading the example:** 6 total escalations (3 LLM-requested + 3 error-triggered). This means 6 of the 18 calls were "second attempts" at a higher context level. The remaining 12 succeeded on the first try.

### Retries

Retries happen when the agent has already reached the maximum context level and still fails. Unlike escalations, retries **do** consume the retry budget and represent genuine failures that needed re-prompting.

| Type | Trigger |
|---|---|
| **Error** | An action failed at the highest available context level. The agent re-sends the prompt with error context. |
| **No-Actions** | The LLM returned valid JSON but with an empty actions array. The agent re-sends with a "pressure" prompt demanding at least one action. |

**Reading the example:** 0 retries means every step eventually succeeded via escalation alone — no brute-force re-prompting was needed. This is the ideal outcome.

### Non-LLM Execution Paths

These metrics show how many steps bypassed the LLM entirely:

| Metric | Meaning |
|---|---|
| **Replays** | Steps replayed from a cached playbook JSON without calling the LLM. In CI/CD with committed playbooks, this should be the dominant path. |
| **Direct Parses** | Steps resolved by action plugins via regex/pattern matching (e.g., `Navigate to https://...`, `Go back`). Zero tokens, zero latency. |

**Reading the example:** Both are 0, meaning this was a fresh run with no existing playbook — every step required the LLM. On subsequent runs with the playbook committed, you'd see high replay counts and 0 LLM calls.

### Interpreting the Big Picture

The example shows a **healthy first run**: 18 LLM calls with 6 escalations and 0 retries. The agent started lean, escalated when needed, and never had to retry at the same level. The total token cost (84K) is dominated by input tokens (DOM context). On the next run with the playbook cached, this would drop to 0 LLM calls / 0 tokens if the UI hasn't changed.

---

## 💡 Advanced: Intent-Based Prompt Generation

Neodymium AI includes an experimental `AiPromptGenerator`. Instead of writing the step-by-step natural language yourself, you provide an end-goal (an "intent"), and the AI will explore the application to figure out how to achieve it, generating a reusable playbook in the process.

Requires the `@NeodymiumTestGenerator` annotation.

```java
@NeodymiumTestGenerator
public void generateCheckoutFlow() {
    Neodymium.ai().generatePrompt("Purchase a pair of red shoes as a guest user.");
}
```

---

## ⚠️ Limitations & Best Practices

- **Token Limits**: Be mindful of your application's DOM size. The `PageAnalyzer` attempts to clean up irrelevant tags, but massive pages might hit token limits or slow down LLM execution.
- **Commit Playbooks**: Always commit your generated `ai-playbooks`. This ensures CI/CD pipelines run fast and do not depend on external LLM calls unless the UI breaks and requires self-healing.
- **Clear Instructions**: While the AI is smart, vague instructions can lead to unpredictable behavior. Be descriptive: instead of "Click the button", use "Click the blue 'Submit Order' button".

---

## 🎯 Locator Hints & Zero-DOM Execution

Sometimes you may want to explicitly guide the AI on which element to interact with, bypassing its own DOM analysis to speed up execution or resolve ambiguity. You can do this using **Inline Hints**.

When you provide a hint directly within the instruction using the `(hint: ...)` syntax, Neodymium AI enters **Zero-DOM Execution mode (`ContextLevel.HINT`)**. The LLM is asked to translate your instruction into JSON using *only* the hint you provided. ZERO DOM elements are extracted or sent to the LLM. This makes hint-based steps unbelievably fast and costs practically zero input tokens!

### Inline Hints
You can provide a hint directly within the instruction using the `(hint: ...)` syntax.

```yaml
prompt: |
  Click the search button (hint: .btn-search).
  Type '${searchTerm}' into the search field (hint: #header-search-text).
```

### Using the Hints Dictionary for Placeholders
If you prefer to separate locators from the natural language, or have hints you want to apply across the whole playbook, you can define a `hints` block at the root of your YAML playbook. 

This is a simple dictionary mapping semantic element names to explicit CSS or XPath locators. Behind the scenes, the framework seamlessly merges these into the dataset so they can be interpolated into your prompt!

```yaml
prompt: |
  Click the login button (hint: ${loginButton}).
  Type '${user}' into the username field (hint: ${usernameField}).
hints:
  loginButton: ".nav-login-btn"
  usernameField: "#user-id"
data:
  - testId: 1
    user: "test@example.com"
```

**How it works:**
1. The framework resolves `${loginButton}` using the `hints` map, generating the final instruction: `Click the login button (hint: .nav-login-btn).`
2. The AI detects the inline hint `(hint: .nav-login-btn)` and starts execution at `ContextLevel.HINT` (Zero DOM elements sent to the LLM).
3. The LLM translates the instruction into JSON for practically 0 tokens.
4. **Fallback / Self-Healing:** If the provided hint is broken or stale (e.g., the element doesn't exist on the live page), Selenium throws an exception. The AI catches this error, **escalates the context to `LEAN`**, grabs the actual live DOM, and uses it to automatically self-heal and find the *new* correct selector!

This "Zero-Code" approach allows you to inject deterministic element targeting while retaining the self-healing benefits of the AI agent, all while preserving strict token efficiency by sending absolutely no DOM data unless the hint fails.

---

## 🛠️ Prompt Overriding

To provide maximum flexibility and allow testing strategies to be customized per project, the core LLM instructions and prompt templates have been externalized. By default, the framework loads its prompt templates from the `neodymium.jar` classpath at `ai-prompts/`. 

External projects that include Neodymium (via Maven/Gradle) can easily override any of these system prompts. To do so, simply create a file with the exact same name in your own project's `src/main/resources/ai-prompts/` directory.

Because the Java classloader prioritizes your project's `target/classes` over dependencies, the framework will automatically discover and use your customized prompt file instead of the default one.

### Example Override

If you want to alter the strict data binding instructions in V2 generation, you would create this file in your consuming project:

```text
src/main/resources/ai-prompts/v2-system-exploration-prompt.txt
```

*(You can copy the default file contents directly from the Neodymium source repository to serve as a starting template).*

### Available AI Prompts

Here is a breakdown of the available templates that can be overridden:

#### 1. Test Generation (V2 Mode)
These prompts control the V2 test generation pipeline, which emphasizes robust forward-exploration and a subsequent extraction phase to filter out mistakes.

- **`v2-system-exploration-prompt.txt`**: The overarching system manual for the AI. Defines its persona, JSON formatting requirements, data parameterization rules, and instructions for recovering from UI errors.
- **`v2-exploration-prompt-template.txt`**: The user-facing prompt injected at each step. It interpolates the current DOM state, high-level intent, previously attempted actions, and any known data bindings.
- **`v2-extraction-prompt.txt`**: Instructions for the secondary LLM pass. It tells the AI how to analyze the messy chronological playbook and extract only the successful, linear steps.
- **`v2-extraction-retry-prompt.txt`**: A small retry template used if the AI fails to output the extraction array in the correct JSON format.

#### 2. Test Generation (Legacy / V1 Mode)
These prompts govern the original test generation pipeline, which allows the AI to conceptually backtrack and delete steps mid-exploration using `dropLastNActions`.

- **`system-exploration-prompt.txt`**: The system instructions for the V1 exploratory agent, including backtracking rules.
- **`exploration-prompt-template.txt`**: The user-facing prompt template for the V1 generation loop.

#### 3. Playbook Healing & Validation
These prompts are utilized during the execution of a pre-recorded Playbook when a step fails (e.g., due to a changed locator or moved button).

- **`system-healing-prompt.txt`**: Instructs the AI on how to act as a self-healing agent, asking it to determine if a failure is a genuine application bug or just a UI change that can be fixed.
- **`healing-prompt-template.txt`**: The user-facing template that provides the broken instruction, original element context, the thrown exception, and the current DOM state to diagnose the issue.

#### 4. Basic Agent Execution (Single-Shot)
These prompts are used for direct, single-action commands when utilizing the `AiAgent` for simple instructions outside of a continuous exploration loop.

- **`system-prompt.txt`**: The core system persona for basic test automation, describing available browser capabilities (`CLICK`, `TYPE`, `ASSERT`, etc.).
- **`user-prompt-template.txt`**: The user-facing template combining the human instruction and current DOM state.

#### 5. Retries & Error Handling
Templates used to feed error context back into the LLM if an action fails or the model hallucinates an invalid response format.

- **`retry-prompt-template.txt`**: Used when a chosen action fails (e.g., ElementNotInteractableException). It provides the exception and tells the AI to pick a new approach.
- **`no-actions-retry-prompt-template.txt`**: Used when the AI successfully returns JSON but hallucinates an empty actions array. Instructs the AI that it must output at least one action.

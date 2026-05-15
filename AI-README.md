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

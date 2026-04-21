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

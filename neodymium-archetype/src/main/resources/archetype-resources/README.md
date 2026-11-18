#set( $h2 = '##' )
#set( $h3 = '###' )
# ${artifactId}

Neodymium test automation project bootstrapped with `neodymium-archetype`.

${h2} Prerequisites

- **Java Development Kit (JDK 21+)**
- **Apache Maven (3.9+)**
- Supported browser (Google Chrome, Mozilla Firefox)
- **Google Gemini API Key** exported as `GEMINI_API_KEY` (or configured via Aura Manager Settings / `config/ai.properties`)

${h2} Neodymium Aura Manager (Web UI)

Aura Manager provides an interactive local dashboard to inspect, manage, edit, and execute natural language playbooks without writing Java code.

Start the Aura Manager UI:

- **Linux / macOS:**
  ```bash
  ./run-aura.sh
  ```
- **Windows:**
  ```cmd
  run-aura.bat
  ```

Open your browser at `http://localhost:8080`.

${h2} Included Neo AI YAML Playbooks (Posters Demo Store)

The archetype includes showcase natural language test playbooks targeting the Posters Demo Store in `src/test/resources/tests/`:

- **`tests/posters_homepage.yaml`**: Intensive homepage verification validating brand logo, navigation categories (*World of Nature*, *Dining*, *Transportation*, *Panoramas*), carousel, product grid, footer links, and visual element alignment.
- **`tests/posters_search.yaml`**: Parameterized search across multiple terms (`"Modern"`, `"Nature"`, `"Ocean"`, `"Bamboo"`), asserting result counts, product prices, and a 3-column visual grid layout (`(visual)`).
- **`tests/posters_registration.yaml`**: End-to-end customer registration generating a unique `@varmail.de` address, verifying green success banner styling, and cleaning up by deleting the account in the `after:` teardown phase.
- **`tests/posters_negative_login.yaml`**: Negative authentication flow verifying invalid credential alerts and checking error banner color variation (reddish background and dark red text styling).
- **`tests/fragments/cookie_consent.steps`**: Reusable step snippet demonstrating modular step inclusion (`_include:`).

${h2} Executing Tests via Command Line (Maven)

Each YAML playbook is paired with a JUnit 5 test runner under `src/test/java/tests/` using `@AiPlaybook` and `@NeodymiumAiTest`.

Run all tests:

```bash
mvn clean test
```

Run a specific AI test runner:

```bash
# Homepage validation test (opens visible browser by default)
mvn test -Dtest=PostersHomepageAiTest

# Multilingual / multi-term search test
mvn test -Dtest=PostersSearchAiTest

# Customer registration and account lifecycle test
mvn test -Dtest=PostersRegistrationAiTest

# Negative login and error styling test
mvn test -Dtest=PostersNegativeLoginAiTest
```

Run headlessly (e.g. in CI pipelines or Docker):

```bash
mvn test -Dbrowserprofile.global.headless=true
```

${h2} Configuration

- `config/neodymium.properties`: Base URLs, timeouts, screenshot capture behavior, and debugging flags.
- `config/browser.properties`: Browser definitions, resolutions, and execution profiles.
- `config/ai.properties`: AI provider settings, Gemini model selection, and agent execution parameters.

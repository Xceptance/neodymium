# Neodymium E2E Tests

`neodymium-e2e-tests` contains the end-to-end integration, regression, and benchmark test suites for the Neodymium ecosystem.

By isolating these test suites in a dedicated module, the production library JARs (`neodymium-core`, `neodymium-classic`) remain lightweight, free of heavy test fixtures, mock recordings, and visual baseline artifacts.

---

## Maven Test Profiles

Neodymium provides fine-grained Maven test profiles configured in the root `pom.xml` to execute targeted test tiers:

| Profile | Command | Description |
| :--- | :--- | :--- |
| **Non-LLM Tests** | `mvn test -pl neodymium-e2e-tests -Pnon-llm-tests` | Fast, deterministic integration tests that require zero LLM calls or API keys. |
| **VÉRLA Tests** | `mvn test -pl neodymium-e2e-tests -Pverla-tests` | Full storefront customer journey and UI integration tests against the local Javalin server. |
| **AI Mock Tests** | `mvn test -pl neodymium-e2e-tests -Pai-mock-tests` | AI state machine and playbook replay validation using pre-recorded mock LLM fixtures. |
| **AI Live Tests** | `mvn test -pl neodymium-e2e-tests -Pai-live-tests` | Real LLM tests communicating with live AI providers (requires `GEMINI_API_KEY`). |
| **AI Sandbox** | `mvn test -pl neodymium-e2e-tests -Pai-sandbox` | Visual regression, dHash baseline, and injected visual defect audits. |
| **Classic JUnit 5** | `mvn test -pl neodymium-e2e-tests -Pneo-classic-junit5` | Classic framework regressions running under JUnit 5 Jupiter engine. |
| **Classic JUnit 4** | `mvn test -pl neodymium-e2e-tests -Pneo-classic-junit4` | Classic framework regressions running under JUnit 4 Vintage engine. |

For detailed profile definitions and Surefire filter patterns, refer to [doc/TEST_PROFILES.md](../doc/TEST_PROFILES.md).

---

## Directory Structure

```
neodymium-e2e-tests/src/test/java/
├── org/neodymium/
│   ├── ai/
│   │   ├── integration/          # AI engine integration tests
│   │   │   ├── sandbox/          # AI Glance visual defect sandbox tests
│   │   │   └── mock/             # Mock LLM provider replays
│   │   ├── executor/             # Playbook step executor validation
│   │   └── tool/                 # Browser automation tool tests
│   └── verla/                    # VÉRLA demo store end-to-end tests
└── com/xceptance/neodymium/      # Neodymium Classic regression suites
```

---

## Running Specific Tests

Run any individual test class using the `-Dtest` property:

```bash
# Run a specific VÉRLA storefront test
mvn test -pl neodymium-e2e-tests -Dtest=VerlaTailwindStorefrontTest

# Run in headless browser mode
mvn test -pl neodymium-e2e-tests -Dtest=VerlaTailwindStorefrontTest -Dbrowserprofile.global.headless=true
```

---

## License

`neodymium-e2e-tests` is dual-licensed under the **GNU Affero General Public License v3 (AGPLv3)** and a **Commercial License**:

* **Community Edition (GNU AGPLv3)**: Free for development, testing, and internal applications under the terms of the GNU Affero General Public License v3.
* **Commercial Edition**: For commercial SaaS offerings, proprietary bundling, or deployments without AGPLv3 copyleft/network-disclosure requirements, a commercial license is required.

For inquiries and commercial licensing, contact **[support@xceptance.com](mailto:support@xceptance.com)**.

---

*This document was generated with the assistance of AI and reviewed by Xceptance experts.*

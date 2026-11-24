# Changelog

All notable changes to the Neodymium ecosystem are documented in this file.

## [6.0.0-BETA] — 2026-11-24

Neodymium `6.0.0-BETA` introduces **Neodymium AI** alongside a modular multi-artifact architecture and dual licensing.

### Highlights & New Features
- **Modular Artifact Architecture:**
  - `com.xceptance:neodymium-classic` — Pure code-first Java test automation (JUnit 4/5, Selenium, Selenide, Cucumber, Allure) under the permissive **MIT License** (JDK 17+).
  - `com.xceptance:neodymium-core` — Next-generation multimodal AI automation engine (`org.neodymium.*`), natural language YAML playbooks, Universal Page Model (UPM), 5-tier locator cascade, perceptual visual verification (dHash / SSIM), and deterministic zero-token replay with autonomous self-healing (JDK 21+, **GNU AGPLv3 + Commercial**).
  - `com.xceptance:neodymium-manager` — Local Spring Boot 4.1 Web UI for visual YAML playbook authoring, live test execution, interactive debugging console, MCP server integration, and multi-run failure/recovery triage reporting (JDK 21+, **GNU AGPLv3 + Commercial**).
  - `com.xceptance:neodymium-archetype` — Turnkey Maven starter archetype pre-configured with sample AI playbooks, reporting tags, and launcher scripts.
- **LLM Provider Support (Beta):**
  - Supports **Google Gemini** out of the box via the pluggable `LlmProvider` interface (additional providers planned for post-beta releases).
  - API keys are never stored in source files; configure via `GEMINI_API_KEY` environment variable or `-Dneodymium.ai.apiKey=...`.
- **Interactive Execution & Reporting:**
  - Live interactive console with step pause/resume and SSE streaming.
  - Multi-report differential failure & recovery triage dashboard widget and dimensional tag filtering.

### Security & Operational Defaults
- Embedded H2 web console (`/h2-console`) is disabled by default (`spring.h2.console.enabled=false`) while Neodymium AI Manager remains reachable on the LAN (`server.port=8080`).
- MCP server fails closed (`403`) for non-loopback clients when `neodymium.ai.mcp.auth.token` is unset.

### Migration Guide: 5.x → 6.0.0-BETA

1. **Maven Artifact Coordinates:**
   - Replace `com.xceptance:neodymium-library` with:
     - `com.xceptance:neodymium-classic:6.0.0-BETA` if you want classic Java/Selenide automation with zero AI dependencies (MIT license, JDK 17+).
     - `com.xceptance:neodymium-core:6.0.0-BETA` (and optionally `com.xceptance:neodymium-manager:6.0.0-BETA` in `test` scope) if you want Neodymium AI playbooks and the Web UI (JDK 21+).
2. **Java Version Requirement:**
   - `neodymium-classic` targets **JDK 17+**.
   - `neodymium-core` and `neodymium-manager` require **JDK 21+**.
3. **Bootstrapping a Fresh AI Project:**
   ```bash
   mvn archetype:generate \
     -DarchetypeGroupId=com.xceptance \
     -DarchetypeArtifactId=neodymium-archetype \
     -DarchetypeVersion=6.0.0-BETA \
     -DgroupId=com.example \
     -DartifactId=my-test-suite \
     -DinteractiveMode=false
   ```

---

For earlier 5.x and 4.x release notes, see [GitHub Releases](https://github.com/Xceptance/neodymium/releases).

# Neodymium Archetype

`neodymium-archetype` is a Maven archetype module that bootstraps new, turnkey test automation projects pre-configured with **Neodymium AI**, **Neodymium Classic**, and the interactive **Neodymium AI Manager** web dashboard.

---

## Generating a New Project

To generate a new test automation suite from this archetype, run:

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

## What Gets Generated

The generated project contains everything required to begin testing immediately:

1. **Neodymium AI Manager Web UI**:
   - Bundled startup scripts (`run-neodymium.sh` and `run-neodymium.bat`) to launch the local test dashboard on `http://localhost:8080`.
2. **Showcase AI Playbooks (Posters Demo Store)**:
   - `tests/posters_homepage.yaml`: Header, navigation, and visual alignment checks.
   - `tests/posters_search.yaml`: Parameterized search across multiple terms.
   - `tests/posters_registration.yaml`: Customer registration and account teardown.
   - `tests/posters_negative_login.yaml`: Negative authentication and error banner styling checks.
   - `tests/fragments/cookie_consent.steps`: Modular step inclusion (`_include:`).
3. **Paired JUnit 5 Runners**:
   - Ready-to-run test runners using `@AiPlaybook` and `@NeodymiumAiTest` under `src/test/java/tests/`.
4. **Configuration Files**:
   - `config/neodymium.properties`: Base URLs, timeouts, screenshot capture settings.
   - `config/browser.properties`: Browser definitions, resolutions, and emulation profiles.
   - `config/ai.properties`: AI provider settings, Gemini model selection, and agent flags.
5. **Project Documentation**:
   - Generated project README rendered from [src/main/resources/archetype-resources/README.md](src/main/resources/archetype-resources/README.md).

---

## Developing & Testing the Archetype

To build the archetype and execute its integration test (`archetype-it`):

```bash
mvn clean install -pl neodymium-archetype
```

The integration test creates a test project under `target/archetype-it` to verify that the generated project compiles and runs cleanly.

---

## License

`neodymium-archetype` is dual-licensed under the **GNU Affero General Public License v3 (AGPLv3)** and a **Commercial License**:

* **Community Edition (GNU AGPLv3)**: Free for development, testing, and internal applications under the terms of the GNU Affero General Public License v3.
* **Commercial Edition**: For commercial SaaS offerings, proprietary bundling, or deployments without AGPLv3 copyleft/network-disclosure requirements, a commercial license is required.

For inquiries and commercial licensing, contact **[support@xceptance.com](mailto:support@xceptance.com)**.

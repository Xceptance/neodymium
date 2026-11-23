# Neodymium AI Manager

**Neodymium AI Manager** is a standalone Spring Boot web application providing an interactive management dashboard, visual YAML playbook editor, live test runner, and test report hub for the Neodymium ecosystem.

Built with **Spring Boot 3**, **Thymeleaf 3**, **HTMX 1.9**, and **Bootstrap 5.3**, Neodymium AI Manager delivers a responsive, dynamic user experience without heavy single-page application (SPA) build chains.

---

## Key Capabilities

### 1. Interactive YAML Playbook Editor
- **Visual Palette Chips**: Quickly insert common step blocks (`open:`, `click:`, `type:`, `assert:`, `_include:`) with proper indentation and schema structure.
- **Template System**: Scaffold common flows (navigation, authentication, search, checkout) with one click.
- **Real-Time Validation**: Catch YAML syntax and schema formatting errors before execution.

### 2. Sidebar Test Runner & Live Progress
- **Segmented Progress Bar**: Real-time visualization of executing test suites, segmented by passed, failed, and queued tests.
- **Live Outcome Counters**: Instant visibility into running, passed, failed, and total test counts with server-side rendering (SSR) parity.

### 3. Execution Console & Run Summaries
- **Streaming Console**: Stream live test execution logs directly to the browser via HTMX and server-sent events.
- **Minimized Status Bar**: Compact console bar surfacing last-run outcome badge, execution duration, and log toggles.

### 4. Settings & Pre-Flight Checks
- **API Key Guard**: Automatically validates `GEMINI_API_KEY` configuration before running AI-powered playbooks, offering direct prompts to configure or override keys.
- **Dynamic Configuration**: Adjust browser settings, viewport sizes, and AI parameters without modifying source files.

### 5. Report Management & History
- View interactive Allure reports, historical execution runs, failure screenshots, and visual diff baselines.

---

## Technical Architecture

* **Frontend**: Bootstrap 5.3 grid and components with unified sizing (`--base-font-size: 0.875rem`).
* **Interactivity**: HTMX 1.9 dynamic HTML fragment swapping (`hx-get`, `hx-target`, `hx-swap`) for seamless page updates without full browser reloads.
* **Unobtrusive Scripting**: Clean separation of concerns with zero inline styles (`style="..."`) or inline JavaScript handlers (`onclick="..."`).
* **Backend**: Spring MVC controllers, Thymeleaf template engine, and asynchronous execution brokers.

---

## How to Run

### Prerequisites
- Java Development Kit (JDK 21+)
- Apache Maven (3.9+)

### Launching Neodymium AI Manager

From the monorepo root:

```bash
# Run via Maven
mvn spring-boot:run -pl aura-manager

# Or run via convenience script:
./run-neodymium.sh          # Linux / macOS
run-neodymium.bat           # Windows
```

Once started, open **`http://localhost:8080/`** in your browser.

---

## License

Neodymium AI Manager is dual-licensed under the **GNU Affero General Public License v3 (AGPLv3)** and a **Commercial License**:

* **Community Edition (GNU AGPLv3)**: Free for development, testing, and internal applications under the terms of the GNU Affero General Public License v3.
* **Commercial Edition**: For commercial SaaS offerings, proprietary bundling, or deployments without AGPLv3 copyleft/network-disclosure requirements, a commercial license is required.

For inquiries and commercial licensing, contact **[support@xceptance.com](mailto:support@xceptance.com)**.

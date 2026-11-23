# Neodymium Test Server

`neodymium-test-server` provides a self-contained, lightning-fast test server built on [Javalin](https://javalin.io/) (Jetty). It hosts realistic web applications and micro-fixtures locally so that integration tests, UI benchmarks, and visual regression suites can run completely offline without external network dependencies.

---

## Hosted Applications & Test Endpoints

### 1. VÉRLA Demo Store (`/verla/`)
A modern, responsive e-commerce storefront designed specifically for benchmarking automated test frameworks:
- **E-Commerce Capabilities**: Dynamic product catalog, faceted filtering, keyword search, cart manipulation, checkout flows, and user profile management.
- **Dual Rendering**: Server-side rendered (SSR) templates alongside a rich REST API (`/verla/api/*`).
- **Configurable Determinism**: Controlled product generators and catalog seeding for reproducible test runs.

For architectural and catalog details, see [doc/VERLA_DEMO_STORE.md](../doc/VERLA_DEMO_STORE.md).

### 2. AI Glance Visual Defect Sandbox (`/AuraGlanceTest/`)
An interactive visual testing suite engineered to evaluate and stress-test visual regression engines, dHash baselines, and visual root-cause analysis (RCA):
- **Dynamic Defect Injection**: On-the-fly toggles for layout shifts, missing elements, swapped typography, corrupted colors, and responsive viewport distortions.
- **Baseline Management**: Serves reference states and parameterized defect mutations for testing SSIM and Hamming distance gating.

For testing guides and defect injection parameters, see [doc/aura-visual-defect-sandbox.md](../doc/aura-visual-defect-sandbox.md).

### 3. Action Fixtures (`/actionfixtures/`)
Dedicated micro-pages targeted at low-level browser interaction verification:
- Dynamic delayed DOM rendering and DOM replacement.
- File upload inputs, shadow DOM boundaries, nested iframes, modal dialogs, and alert prompts.
- Complex input masks, keyboard events, and mouse hover sequences.

### 4. Session & State Isolation
Each test execution can supply a unique `X-Session-ID` header (or cookie) to interact with an isolated state context (isolated shopping cart, temporary users, and state overrides) without cross-test pollution.

---

## How to Run

### 1. Embedded Execution (Within JUnit Tests)

In automated tests, start and stop the server programmatically:

```java
import org.neodymium.ai.server.JavalinTestServer;

// Start server on free random ports (HTTP & HTTPS)
JavalinTestServer.start();

int httpPort = JavalinTestServer.getPort();
String baseUrl = JavalinTestServer.getBaseUrl();

// Stop server after test suite
JavalinTestServer.stop();
```

### 2. Standalone Manual Server Execution

To run the server indefinitely for manual browser inspection or exploratory testing:

```bash
# Start with default ports (HTTP 8542, HTTPS 8543)
mvn exec:java -pl neodymium-test-server

# Or specify custom ports:
mvn exec:java -pl neodymium-test-server -Dexec.args="--port 8080 --httpsPort 8443"
```

Once running:
- **VÉRLA Storefront**: Visit `http://localhost:8542/verla/`
- **AI Glance Visual Sandbox**: Visit `http://localhost:8542/AuraGlanceTest/`

---

## License

`neodymium-test-server` is dual-licensed under the **GNU Affero General Public License v3 (AGPLv3)** and a **Commercial License**:

* **Community Edition (GNU AGPLv3)**: Free for development, testing, and internal applications under the terms of the GNU Affero General Public License v3.
* **Commercial Edition**: For commercial SaaS offerings, proprietary bundling, or deployments without AGPLv3 copyleft/network-disclosure requirements, a commercial license is required.

For inquiries and commercial licensing, contact **[support@xceptance.com](mailto:support@xceptance.com)**.

---

*This document was generated with the assistance of AI and reviewed by Xceptance experts.*

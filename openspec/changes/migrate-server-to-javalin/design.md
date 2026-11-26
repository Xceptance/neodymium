## Context

`EmbeddedHtmlServer.java` is currently a monolithic 3,800-line class in `src/test/java/org/neodymium/ai/util/`. To enable modern, maintainable development while eliminating risk to ongoing tests, we are building a brand-new **Javalin 6.x + Thymeleaf** server side-by-side under `org.neodymium.ai.server.*`.

`EmbeddedHtmlServer.java` will remain completely untouched and active. Both implementations will coexist side-by-side until the new server is fully proven and ready to become the primary server.

## Goals / Non-Goals

**Goals:**
- Build a standalone, modern `JavalinTestServer` in `src/test/java/org/neodymium/ai/server/`.
- Decompose server logic into isolated, single-responsibility controllers (`CartController`, `CatalogController`, `CheckoutController`, `AuthController`, `OrderController`, `StorefrontController`).
- Replace inline Java string HTML generation with clean Thymeleaf template fragments (`src/test/resources/templates/fragments/*.html`).
- Ensure cold boot startup time remains under 100 ms on ephemeral ports.
- Build comprehensive test suites (`JavalinServerTest`, `JavalinEndpointsTest`) verifying full parity with all storefront features.

**Non-Goals:**
- Modifying, refactoring, or deleting `EmbeddedHtmlServer.java` (kept 100% intact).
- Forcing existing tests to switch immediately to `JavalinTestServer`.
- Adding production dependencies to `pom.xml` (all new dependencies are strictly test-scoped).

## Decisions

### Decision 1: Side-by-Side Coexistence
- **Rationale**: Building `JavalinTestServer` as a separate, parallel implementation avoids any risk of breaking existing tests during development. Developers can choose which server to run and compare both directly.
- **Alternatives Considered**: 
  - *Refactoring `EmbeddedHtmlServer` in place*: Too risky for active branches and test suites.
  - *Adapter Facade*: Unnecessary complexity when a clean, separate implementation can run side-by-side.

### Decision 2: Javalin 6.x with Embedded Jetty
- **Rationale**: Provides expressive, fluent lambda routing (`app.post(...)`, `app.get(...)`) and sub-100ms cold boot without heavy Spring framework initialization overhead.

### Decision 3: Thymeleaf Rendering Engine
- **Rationale**: Enables writing natural `.html` template fragments with loops (`th:each`), conditionals (`th:if`), and fragment includes (`th:replace`), supporting full HTML/Tailwind intellisense in IDEs.

### Decision 4: Dual HTTP + HTTPS Jetty Connector Configuration
- **Rationale**: Configures Jetty's `modifyServer` callback with two `ServerConnector` instances (one plaintext, one SSL with `keystore.p12`) to support tests requiring secure multi-port contexts.

## Risks / Trade-offs

- **[Risk]** Port collisions if running both servers simultaneously in the same JVM process.  
  → **Mitigation**: Both servers default to ephemeral port allocation (`port 0`), allowing both `EmbeddedHtmlServer` and `JavalinTestServer` to run simultaneously on distinct random ports without conflict.
- **[Risk]** Test dependency footprint.  
  → **Mitigation**: Javalin and Thymeleaf are placed strictly in Maven `<scope>test</scope>`, ensuring they never leak into downstream consumer project classpaths.

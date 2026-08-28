## Why

The current `EmbeddedHtmlServer.java` is a 3,800-line monolithic class built on low-level JDK `HttpServer`. It handles routing through giant nested `if/else` conditionals, manually parses query strings and bodies, and constructs dynamic HTML components using thousands of lines of hardcoded, escaped Java `StringBuilder` strings. This makes UI changes, new storefront variants, and endpoint updates tedious, error-prone, and devoid of IDE markup tooling.

To modernize our server infrastructure without risking existing tests, we will introduce a new **Javalin 6.x + Thymeleaf** server implementation side-by-side with `EmbeddedHtmlServer.java`. The existing server remains untouched while the new server provides clean declarative routing, dedicated controllers, real `.html` template files with full IDE support, and sub-100ms boot speeds.

## What Changes

- **Test Dependencies**: Add `io.javalin:javalin` (6.x), `io.javalin:javalin-rendering`, and `org.thymeleaf:thymeleaf` with `<scope>test</scope>` to `pom.xml`.
- **Side-by-Side Coexistence**: Implement `JavalinTestServer` in a dedicated package `org.neodymium.ai.server.*` completely independent of `EmbeddedHtmlServer.java`.
- **Modular Controller Architecture**: Decompose VÉRLA dynamic storefronts into clean domain controllers (`CartController`, `CatalogController`, `CheckoutController`, `AuthController`, `OrderController`, `StorefrontController`).
- **Thymeleaf Template Engine**: Implement reusable `.html` template fragment files under `src/test/resources/templates/` with full HTML/Tailwind IDE support.
- **Dual-Port Server Lifecycle**: Implement dual random ephemeral port listeners (HTTP and HTTPS with PKCS12 keystore) for `JavalinTestServer`.
- **Side-by-Side Validation**: Existing tests continue running against `EmbeddedHtmlServer`, while new/migrated integration test suites validate `JavalinTestServer`.

## Capabilities

### New Capabilities
- `embedded-test-server`: Defines the requirements, endpoint contracts, response formats, HTTP status codes, session/cookie handling, latency simulation, and fault injection behaviors of the new Javalin-based embedded test server and VÉRLA storefront applications.

### Modified Capabilities
<!-- No existing capabilities modified -->

## Impact

- **Affected Code**: Adds new package `src/test/java/org/neodymium/ai/server/` and templates under `src/test/resources/templates/`.
- **Preserved Code**: `src/test/java/org/neodymium/ai/util/EmbeddedHtmlServer.java` remains 100% untouched.
- **Dependencies**: `io.javalin:javalin`, `io.javalin:javalin-rendering`, `org.thymeleaf:thymeleaf` (strictly test-scoped).

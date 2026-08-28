## 1. Dependency & Configuration Setup

- [ ] 1.1 Add `io.javalin:javalin` (6.4.0), `io.javalin:javalin-rendering` (6.4.0), and `org.thymeleaf:thymeleaf` (3.1.3.RELEASE) to `pom.xml` with `<scope>test</scope>`
- [ ] 1.2 Implement `ThymeleafConfig.java` in `src/test/java/org/neodymium/ai/server/config/` with `ClassLoaderTemplateResolver` configured for `templates/` prefix

## 2. Template Extraction

- [ ] 2.1 Create Thymeleaf HTML fragment `templates/fragments/product-card.html` for PLP and search results
- [ ] 2.2 Create Thymeleaf HTML fragment `templates/fragments/cart-content.html` and `templates/fragments/cart-badge.html`
- [ ] 2.3 Create Thymeleaf HTML fragment `templates/fragments/cart-dropdown.html` for header mini-cart
- [ ] 2.4 Create Thymeleaf HTML fragment `templates/fragments/address-fields.html` with localized country rules
- [ ] 2.5 Create Thymeleaf HTML fragment `templates/fragments/order-confirmation.html` with summary and items

## 3. Domain Model Extraction

- [ ] 3.1 Extract clean domain models (`Product.java`, `Country.java`, `CatalogConfig.java`, `Cart.java`, `Order.java`, `OrderItem.java`, `User.java`, `Address.java`, `Card.java`) into `src/test/java/org/neodymium/ai/server/model/`
- [ ] 3.2 Implement thread-safe repository stores for in-memory catalog, user sessions, inventory, and orders

## 4. Controller Implementation

- [ ] 4.1 Implement `CartController.java` (`/api/cart/add`, `/api/cart/update`, `/api/cart/remove`, `/api/cart/coupon`, `/api/cart/dropdown`)
- [ ] 4.2 Implement `CatalogController.java` (`/api/snippets/products`, `/api/search/suggest`, `/api/countries`, `/api/country/select`)
- [ ] 4.3 Implement `CheckoutController.java` (`/api/address-form`, `/api/checkout/purchase`, `/api/order/lookup`)
- [ ] 4.4 Implement `AuthController.java` (`/api/auth/login`, `/api/auth/register`, `/api/auth/logout`, `/api/auth/change-password`, address/card management)
- [ ] 4.5 Implement `StorefrontController.java` (dynamic page rendering across all 8 storefront quality variants)

## 5. Server Lifecycle & Standalone Implementation

- [ ] 5.1 Implement `JavalinTestServer.java` with dual Jetty connectors for HTTP and HTTPS (PKCS12 certificate) on dynamic ephemeral ports (port 0)
- [ ] 5.2 Implement standalone launcher `main()` in `JavalinTestServer.java` for local interactive sandbox development
- [ ] 5.3 Implement latency integration with `VerlaConfiguration` simulated latencies and phantom traps

## 6. Verification & Test Suite

- [ ] 6.1 Implement `ThymeleafRenderingTest.java` verifying isolated template fragment rendering
- [ ] 6.2 Implement `JavalinEndpointsTest.java` verifying all 11 endpoint test scenarios against `JavalinTestServer`
- [ ] 6.3 Implement `JavalinServerTest.java` verifying dual-port HTTP and HTTPS lifecycles
- [ ] 6.4 Execute full test suite ensuring all existing `EmbeddedHtmlServer` tests (`EndpointsTest`, `AddToCartTest`, `GuestCheckoutTest`) continue passing side-by-side with 0 regressions
- [ ] 6.5 Benchmark and verify cold boot startup time of `JavalinTestServer` remains under 100 ms

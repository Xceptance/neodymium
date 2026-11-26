## Purpose

Provides a self-contained, lightweight embedded HTTP and HTTPS test server with full-featured multi-country VÉRLA e-commerce storefront applications, dynamic HTMX endpoints, and fault injection for automated and manual testing.

## ADDED Requirements

### Requirement: Dual Port HTTP and HTTPS Server Lifecycle
The embedded test server SHALL support concurrent HTTP and HTTPS listeners binding to dynamic ephemeral ports (port 0) or specified ports using a self-signed PKCS12 keystore.

#### Scenario: Server starts on ephemeral ports
- **WHEN** the server is started with port 0 for HTTP and HTTPS
- **THEN** both HTTP and HTTPS listeners bind successfully and expose active random port numbers greater than 0

#### Scenario: HTTPS TLS handshake with self-signed certificate
- **WHEN** a client establishes a TLS connection to the HTTPS port
- **THEN** the server performs a valid TLS handshake using the configured keystore certificate

### Requirement: Dynamic Cart Operations
The server SHALL provide HTMX-compatible cart management endpoints supporting adding items by SKU and size, updating quantities, removing items, applying promotional coupons, and rendering mini-cart dropdown fragments.

#### Scenario: Add item to cart
- **WHEN** a client sends a POST request to `/api/cart/add` with `productId`, `size`, and `quantity`
- **THEN** the server increments cart items for the active session and returns a 200 OK with the rendered cart badge HTML fragment

#### Scenario: Apply valid discount coupon
- **WHEN** a client sends a POST request to `/api/cart/coupon` with `couponCode=10p-off`
- **THEN** the server applies a 10% discount to cart subtotal and returns the updated cart content HTML fragment

### Requirement: Dynamic Catalog Filtering and Search Autocomplete
The server SHALL support live search suggestions and catalog snippet filtering with pagination, sorting, BOPIS (buy-online-pickup-in-store), and facet filtering.

#### Scenario: Search suggestions query
- **WHEN** a client sends a GET request to `/api/search/suggest?q=shirt`
- **THEN** the server returns an HTML fragment listing matching product suggestions up to the configured limit

#### Scenario: Product catalog snippet filtering
- **WHEN** a client sends a GET request to `/api/snippets/products?category=apparel&sort=price-asc`
- **THEN** the server returns the filtered and sorted product grid HTML fragment

### Requirement: Checkout and Payment Simulation
The server SHALL process checkout purchase requests, generate unique 7-digit order numbers (`V-XXXXXXX-CC`), simulate payment outcomes based on credit card rules, and decrement product inventory.

#### Scenario: Successful purchase with standard card
- **WHEN** a client sends a POST request to `/api/checkout/purchase` with valid customer details and a card number not ending in 200
- **THEN** the server decrements inventory, creates a persistent order record, clears the cart, and returns the order confirmation page with order number format `V-XXXXXXX-CC`

#### Scenario: Declined payment simulation
- **WHEN** a client sends a POST request to `/api/checkout/purchase` with a card number ending in 200
- **THEN** the server rejects the transaction, retains cart items, and returns a 400 Bad Request with the payment failure alert HTML fragment

### Requirement: User Authentication and Profile Management
The server SHALL support user registration via PUT requests, session login via POST requests, password updates, and saved address/card management.

#### Scenario: User registration with PUT request
- **WHEN** a client sends a PUT request to `/api/auth/register` with valid email, password, and confirmation
- **THEN** the server creates the user profile, sets the `verla_session_id` session cookie, and returns a 200 OK with `HX-Redirect` header

#### Scenario: User registration with non-PUT method rejected
- **WHEN** a client sends a POST request to `/api/auth/register`
- **THEN** the server rejects the request with HTTP 405 Method Not Allowed

### Requirement: Runtime Latency and Fault Injection
The server SHALL apply configurable simulated latencies and fault injection hooks (including phantom order traps and chaos mutations) configured via `VerlaConfiguration`.

#### Scenario: Simulated cart operation latency
- **WHEN** `verla.latency.enabled` is true and a client requests `/api/cart/add`
- **THEN** the server delays response dispatch by the configured min/max milliseconds scaled by `verla.latency.scale`

#### Scenario: Phantom order trap execution
- **WHEN** `verla.trap.phantomOrder` is true and a client submits a purchase
- **THEN** the server returns a successful order confirmation page to the client but omits writing the order into the persistent order repository

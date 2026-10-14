/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.server.session.SessionManager;
import org.neodymium.ai.server.session.TestSession;
import org.neodymium.ai.server.verla.VerlaApp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Integration and unit tests for {@link JavalinTestServer} validating VÉRLA mock e-commerce
 * storefront routes, all 11 flavors, catalog APIs, cart, auth, checkout, headless PWA endpoints,
 * and multi-tenant test isolation.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class JavalinVerlaServerTest
{
    private static JavalinTestServer server;
    private static HttpClient client;
    private static final Gson GSON = new Gson();

    @BeforeAll
    public static void setUp() throws Exception
    {
        server = new JavalinTestServer(0, -1);
        server.start();

        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    @AfterAll
    public static void tearDown()
    {
        if (server != null)
        {
            server.stop();
        }
    }

    @Test
    public void testDirectoryRedirects() throws IOException, InterruptedException
    {
        for (final String flavor : VerlaApp.FLAVORS)
        {
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + server.getPort() + "/" + flavor))
                .GET()
                .build();

            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(302, response.statusCode(), "Expected 302 redirect for /" + flavor);
            assertTrue(response.headers().firstValue("location").orElse("").endsWith("/" + flavor + "/index.html"),
                "Redirect target should point to index.html for " + flavor);
        }
    }

    @Test
    public void testAllStorefrontFlavorsServeIndex() throws IOException, InterruptedException
    {
        for (final String flavor : VerlaApp.FLAVORS)
        {
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + server.getPort() + "/" + flavor + "/index.html"))
                .GET()
                .build();

            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), "Expected 200 OK for flavor: " + flavor);
            final String ct = response.headers().firstValue("content-type").orElse("");
            assertTrue(ct.contains("text/html"),
                "Content-Type should be text/html for " + flavor + ", but got: " + ct);
            final String body = response.body();
            assertTrue(body.contains("VÉRLA") || body.contains("VERLA") || body.contains("Apocalypse") || body.contains("headless"),
                "Body should contain storefront branding or structure for " + flavor);
        }
    }

    @Test
    public void testCategoryPlpAndProductPdpPages() throws IOException, InterruptedException
    {
        final HttpRequest plpRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/c/tops.html"))
            .GET()
            .build();

        final HttpResponse<String> plpResponse = client.send(plpRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, plpResponse.statusCode());
        assertTrue(plpResponse.body().contains("tops") || plpResponse.body().contains("Tops"));

        final HttpRequest pdpRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/p/any-product.html"))
            .GET()
            .build();

        final HttpResponse<String> pdpResponse = client.send(pdpRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pdpResponse.statusCode());
    }

    @Test
    public void testCartOperations() throws IOException, InterruptedException
    {
        // 1. Add product to cart
        final HttpRequest addReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/api/cart/add"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("productId=SKU-TOP-1000&size=M&quantity=2"))
            .build();

        final HttpResponse<String> addRes = client.send(addReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, addRes.statusCode());
        assertTrue(addRes.body().contains("(M)"), "Cart response should include item size");
        assertFalse(addRes.body().contains("$0.00"), "Subtotal should be non-zero after adding items to cart");
        final String cartCookie = extractCookie(addRes, "verla_cart_id");

        // Verify cart page renders item details and non-zero subtotal
        final HttpRequest cartPageReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/cart.html"))
            .header("Cookie", "verla_cart_id=" + cartCookie)
            .GET()
            .build();
        final HttpResponse<String> cartPageRes = client.send(cartPageReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, cartPageRes.statusCode());
        assertTrue(cartPageRes.body().contains("cart-item") || cartPageRes.body().contains("SKU-TOP-1000") || cartPageRes.body().contains("(M)"));
        assertFalse(cartPageRes.body().contains("Subtotal</span><span>$0.00"));

        // 2. Update cart quantity
        final HttpRequest updateReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/api/cart/update"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Cookie", "verla_cart_id=" + cartCookie)
            .POST(HttpRequest.BodyPublishers.ofString("productId=SKU-TOP-1000:M&quantity=3"))
            .build();

        final HttpResponse<String> updateRes = client.send(updateReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, updateRes.statusCode());

        // 3. Apply coupon
        final HttpRequest couponReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/api/cart/coupon"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Cookie", "verla_cart_id=" + cartCookie)
            .POST(HttpRequest.BodyPublishers.ofString("couponCode=10p-off"))
            .build();

        final HttpResponse<String> couponRes = client.send(couponReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, couponRes.statusCode());

        // 4. Remove item
        final HttpRequest removeReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/api/cart/remove?productId=SKU-TOP-1000:M"))
            .header("Cookie", "verla_cart_id=" + cartCookie)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        final HttpResponse<String> removeRes = client.send(removeReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, removeRes.statusCode());
    }

    @Test
    public void testCountrySelectionAndSearch() throws IOException, InterruptedException
    {
        final HttpRequest selectReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/api/country/select?code=DE"))
            .GET()
            .build();

        final HttpResponse<String> selectRes = client.send(selectReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, selectRes.statusCode());
        assertTrue(selectRes.headers().allValues("set-cookie").stream().anyMatch(c -> c.contains("verla_country=DE")));
        assertEquals("true", selectRes.headers().firstValue("hx-refresh").orElse(""));

        final HttpRequest searchReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/api/search/suggest?q=shirt"))
            .GET()
            .build();

        final HttpResponse<String> searchRes = client.send(searchReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, searchRes.statusCode());
        assertTrue(searchRes.body().contains("shirt") || searchRes.body().contains("Shirt"));

        final HttpRequest selectCaReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/api/country/select?code=CA_FR"))
            .GET()
            .build();

        final HttpResponse<String> selectCaRes = client.send(selectCaReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, selectCaRes.statusCode());
        assertTrue(selectCaRes.headers().allValues("set-cookie").stream().anyMatch(c -> c.contains("verla_country=CA_FR")));

        final HttpRequest homeCaReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/index.html"))
            .header("Cookie", "verla_country=CA_FR")
            .GET()
            .build();
        final HttpResponse<String> homeCaRes = client.send(homeCaReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, homeCaRes.statusCode());
        assertTrue(homeCaRes.body().contains("🇨🇦"), "Page should render Canadian flag emoji for CA_FR");
    }

    @Test
    public void testAuthAndCheckoutFlow() throws IOException, InterruptedException
    {
        final String email = "buyer-" + System.currentTimeMillis() + "@example.com";
        final String pass = "secret123";

        // 1. Register new user via PUT
        final HttpRequest regReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/api/auth/register"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .PUT(HttpRequest.BodyPublishers.ofString("email=" + email + "&password=" + pass + "&confirmPassword=" + pass))
            .build();

        final HttpResponse<String> regRes = client.send(regReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, regRes.statusCode());
        final String sessionCookie = extractCookie(regRes, "verla_session_id");
        assertNotNull(sessionCookie);

        // 2. Add an item to cart with that session
        final HttpRequest addReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/api/cart/add"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Cookie", "verla_session_id=" + sessionCookie)
            .POST(HttpRequest.BodyPublishers.ofString("productId=SKU-TOP-1000&size=M&quantity=1"))
            .build();

        final HttpResponse<String> addRes = client.send(addReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, addRes.statusCode());
        final String cartCookie = extractCookie(addRes, "verla_cart_id");

        // 3. Purchase checkout
        final String purchaseBody = "firstName=Alice&lastName=Smith&email=" + email +
            "&cardNumber=1111222233334100&cardType=Visa&cardExpiry=12/29&cardCvv=123" +
            "&address=100+Market+St&city=San+Francisco&state=CA&zipCode=94105&country=US";

        final HttpRequest buyReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/api/checkout/purchase"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Cookie", "verla_session_id=" + sessionCookie + "; verla_cart_id=" + cartCookie)
            .POST(HttpRequest.BodyPublishers.ofString(purchaseBody))
            .build();

        final HttpResponse<String> buyRes = client.send(buyReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, buyRes.statusCode());
        assertTrue(buyRes.body().contains("V-") || buyRes.body().contains("Thank you") || buyRes.body().contains("order-number-value"),
            "Purchase response should confirm order");
    }

    @Test
    public void testHeadlessPwaEndpoints() throws IOException, InterruptedException
    {
        final HttpRequest tokReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-headless/api/scapi/token"))
            .GET()
            .build();

        final HttpResponse<String> tokRes = client.send(tokReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, tokRes.statusCode());
        final JsonObject tokJson = GSON.fromJson(tokRes.body(), JsonObject.class);
        assertTrue(tokJson.has("access_token"));
        assertTrue(tokJson.has("customer_id"));

        final HttpRequest sessReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-headless/api/scapi/session"))
            .GET()
            .build();

        final HttpResponse<String> sessRes = client.send(sessReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, sessRes.statusCode());
        final JsonObject sessJson = GSON.fromJson(sessRes.body(), JsonObject.class);
        assertTrue(sessJson.has("state"));
        assertTrue(sessJson.has("customerId"));
    }

    @Test
    public void testMultiTenantSessionIsolation() throws IOException, InterruptedException
    {
        final String sessionTokenA = server.getSessionManager().getOrCreateOpaqueToken("tenant-A");
        final String sessionTokenB = server.getSessionManager().getOrCreateOpaqueToken("tenant-B");

        // Add item to tenant A
        final HttpRequest addReqA = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/api/cart/add"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Cookie", SessionManager.COOKIE_NAME + "=" + sessionTokenA + "; verla_cart_id=shared-cart-id")
            .POST(HttpRequest.BodyPublishers.ofString("productId=SKU-TOP-1000&size=M&quantity=5"))
            .build();

        final HttpResponse<String> resA = client.send(addReqA, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resA.statusCode());

        final TestSession tenantASession = server.getSessionManager().getOrCreateSession("tenant-A");
        final TestSession tenantBSession = server.getSessionManager().getOrCreateSession("tenant-B");

        // Tenant A must have items in cart
        assertTrue(tenantASession.getActiveCarts().containsKey("shared-cart-id"));
        assertEquals(5, tenantASession.getActiveCarts().get("shared-cart-id").items.get("SKU-TOP-1000:M"));

        // Tenant B must NOT have items in cart
        assertTrue(!tenantBSession.getActiveCarts().containsKey("shared-cart-id") ||
                   tenantBSession.getActiveCarts().get("shared-cart-id").items.isEmpty());
    }

    private static String extractCookie(final HttpResponse<?> response, final String cookieName)
    {
        for (final String header : response.headers().allValues("set-cookie"))
        {
            for (final String part : header.split(";"))
            {
                final String trimmed = part.trim();
                if (trimmed.startsWith(cookieName + "="))
                {
                    return trimmed.substring((cookieName + "=").length());
                }
            }
        }
        return null;
    }
}

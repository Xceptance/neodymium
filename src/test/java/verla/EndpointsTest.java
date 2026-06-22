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
package verla;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Endpoint integration test class testing the VÉRLA e-commerce backend APIs.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Tag("integration")
@Tag("verla")
public class EndpointsTest extends BaseAiTest
{
    private final HttpClient client = HttpClient.newBuilder()
        .cookieHandler(new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL))
        .build();

    /**
     * Setup method to ensure temporary playbook directory is used.
     */
    @BeforeEach
    public void setup()
    {
        useTempPlaybookDirectory();
    }

    /**
     * Test routing for PLP, PDP, and missing resource cases.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testProductAndCategoryRouting() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Test home page loading
        final HttpRequest homeReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/index.html"))
            .GET()
            .build();
        final HttpResponse<String> homeResp = client.send(homeReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, homeResp.statusCode());
        Assertions.assertTrue(homeResp.body().contains("VÉRLA"));

        // 2. Test category PLP routing
        final HttpRequest plpReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/c/tops.html"))
            .GET()
            .build();
        final HttpResponse<String> plpResp = client.send(plpReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, plpResp.statusCode());

        // 3. Test dynamic PDP page routing with a missing SKU slug
        final HttpRequest badPdpReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/p/missing-product-slug-999.html"))
            .GET()
            .build();
        final HttpResponse<String> badPdpResp = client.send(badPdpReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(404, badPdpResp.statusCode() == 404 || badPdpResp.body().contains("Not Found") ? 404 : 200);
    }

    /**
     * Test dynamic country address fields API.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testAddressFormFieldsApi() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Get US fields
        final HttpRequest usReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/api/address-form?country=US"))
            .GET()
            .build();
        final HttpResponse<String> usResp = client.send(usReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, usResp.statusCode());
        Assertions.assertTrue(usResp.body().contains("state"));

        // 2. Get JP fields
        final HttpRequest jpReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/api/address-form?country=JP"))
            .GET()
            .build();
        final HttpResponse<String> jpResp = client.send(jpReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, jpResp.statusCode());
        Assertions.assertTrue(jpResp.body().contains("prefecture"));
    }

    /**
     * Test cart operations: Add, Update, and Coupon calculations.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testCartAndCouponApi() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Add to cart
        final HttpRequest addReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/api/cart/add"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("productId=SKU-TOP-1000&quantity=2"))
            .build();
        final HttpResponse<String> addResp = client.send(addReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, addResp.statusCode());
        Assertions.assertTrue(addResp.body().contains("cart-badge"));

        // 2. Add invalid coupon
        final HttpRequest badCouponReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/api/cart/coupon"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("couponCode=invalidcode"))
            .build();
        final HttpResponse<String> badCouponResp = client.send(badCouponReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, badCouponResp.statusCode());
        Assertions.assertTrue(badCouponResp.body().contains("expired or invalid") || badCouponResp.body().contains("invalid"));

        // 3. Add valid coupon (10p-off)
        final HttpRequest goodCouponReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/api/cart/coupon"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("couponCode=10p-off"))
            .build();
        final HttpResponse<String> goodCouponResp = client.send(goodCouponReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, goodCouponResp.statusCode());
        Assertions.assertTrue(goodCouponResp.body().contains("Discount (10P-OFF)"));
    }

    /**
     * Test checkout payment processing simulation rules (card ending in 100 vs 200).
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testPaymentSimulationRules() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // Ensure we have an item in cart
        final HttpRequest addReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/api/cart/add"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("productId=SKU-TOP-1000&quantity=1"))
            .build();
        client.send(addReq, HttpResponse.BodyHandlers.discarding());

        // 1. Try checkout with a card ending in 200 (payment decline simulation)
        final HttpRequest declineReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/api/checkout/purchase"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("firstName=Jane&lastName=Doe&email=jane@example.com&country=US&street=123+St&city=Boston&state=MA&postcode=02108&cardType=Visa&cardNumber=1111222233334200&cardExpiry=12/29&cardCvv=123"))
            .build();
        final HttpResponse<String> declineResp = client.send(declineReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(400, declineResp.statusCode());
        Assertions.assertTrue(declineResp.body().contains("declined") || declineResp.body().contains("Declined"));

        // 2. Try checkout with a card ending in 100 (payment success simulation)
        final HttpRequest successReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/api/checkout/purchase"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("firstName=Jane&lastName=Doe&email=jane@example.com&country=US&street=123+St&city=Boston&state=MA&postcode=02108&cardType=Visa&cardNumber=1111222233334100&cardExpiry=12/29&cardCvv=123"))
            .build();
        final HttpResponse<String> successResp = client.send(successReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, successResp.statusCode());
        Assertions.assertTrue(successResp.body().contains("Success") || successResp.body().contains("success") || successResp.body().contains("Order Number") || successResp.body().contains("V-"));
    }

    /**
     * Test user registration endpoint simulation using PUT request,
     * and verify method validation restricts other HTTP methods with 405.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testUserRegistration() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Send POST request (should be rejected with 405)
        final HttpRequest postReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/api/auth/register"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("email=testregister@example.com&password=test99&confirmPassword=test99"))
            .build();
        final HttpResponse<String> postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(405, postResp.statusCode());

        // 2. Send PUT request with mismatched passwords (should fail with 400 validation error)
        final HttpRequest mismatchedReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/api/auth/register"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .PUT(HttpRequest.BodyPublishers.ofString("email=testregister@example.com&password=test99&confirmPassword=test100"))
            .build();
        final HttpResponse<String> mismatchedResp = client.send(mismatchedReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(400, mismatchedResp.statusCode());

        // 3. Send valid PUT request (should succeed, set cookie and redirect via HX-Redirect header)
        final HttpRequest registerSuccessReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/api/auth/register"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .PUT(HttpRequest.BodyPublishers.ofString("email=testregister@example.com&password=test99&confirmPassword=test99"))
            .build();
        final HttpResponse<String> registerSuccessResp = client.send(registerSuccessReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, registerSuccessResp.statusCode());
        Assertions.assertTrue(registerSuccessResp.headers().firstValue("HX-Redirect").isPresent());
        Assertions.assertTrue(registerSuccessResp.headers().firstValue("Set-Cookie").orElse("").contains("verla_session_id="));
    }

    /**
     * Test category-specific dynamic size selection selectors on PDP.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testCategorySpecificSizes() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Tops product: /verla-perfect/p/premium-off-white-shirts-0.html -> should contain sizes XS, S, M, L, XL
        final HttpRequest topsReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/p/premium-off-white-shirts-0.html"))
            .GET()
            .build();
        final HttpResponse<String> topsResp = client.send(topsReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, topsResp.statusCode());
        Assertions.assertTrue(topsResp.body().contains("value=\"XS\""));
        Assertions.assertTrue(topsResp.body().contains("value=\"XL\""));
        Assertions.assertFalse(topsResp.body().contains("value=\"28\""));

        // 2. Bottoms product: /verla-perfect/p/tailored-terracotta-shorts-1.html -> should contain sizes 28 to 38
        final HttpRequest bottomsReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/p/tailored-terracotta-shorts-1.html"))
            .GET()
            .build();
        final HttpResponse<String> bottomsResp = client.send(bottomsReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, bottomsResp.statusCode());
        Assertions.assertTrue(bottomsResp.body().contains("value=\"28\""));
        Assertions.assertTrue(bottomsResp.body().contains("value=\"38\""));
        Assertions.assertFalse(bottomsResp.body().contains("value=\"XS\""));

        // 3. Footwear product: /verla-perfect/p/wool-blend-taupe-boots-3.html -> should contain sizes 39 to 44
        final HttpRequest footwearReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/p/wool-blend-taupe-boots-3.html"))
            .GET()
            .build();
        final HttpResponse<String> footwearResp = client.send(footwearReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, footwearResp.statusCode());
        Assertions.assertTrue(footwearResp.body().contains("value=\"39\""));
        Assertions.assertTrue(footwearResp.body().contains("value=\"44\""));
        Assertions.assertFalse(footwearResp.body().contains("value=\"28\""));

        // 4. Accessories product: /verla-perfect/p/classic-navy-bags-4.html -> should bypass size selection (not contain size options at all)
        final HttpRequest accReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/p/classic-navy-bags-4.html"))
            .GET()
            .build();
        final HttpResponse<String> accResp = client.send(accReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, accResp.statusCode());
        Assertions.assertFalse(accResp.body().contains("id=\"size\""));
    }

    /**
     * Test that PLP endpoints return a full HTML page for standard requests,
     * but return only the product card list fragment for HTMX requests.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testPlpHtmxResponse() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Send normal GET request (without HX-Request header) -> should return the full HTML page with layout wrapper
        final HttpRequest plpReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/c/tops.html"))
            .GET()
            .build();
        final HttpResponse<String> plpResp = client.send(plpReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, plpResp.statusCode());
        Assertions.assertTrue(plpResp.body().contains("<!DOCTYPE html>"));
        Assertions.assertTrue(plpResp.body().contains("VÉRLA"));

        // 2. Send GET request with HX-Request = true -> should return ONLY the product list fragment
        final HttpRequest htmxReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/c/tops.html"))
            .header("HX-Request", "true")
            .GET()
            .build();
        final HttpResponse<String> htmxResp = client.send(htmxReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, htmxResp.statusCode());
        // Verify that it doesn't contain layout wrappers
        Assertions.assertFalse(htmxResp.body().contains("<!DOCTYPE html>"));
        Assertions.assertFalse(htmxResp.body().contains("<header"));
        Assertions.assertFalse(htmxResp.body().contains("<footer"));
        // Verify that it contains product card content
        Assertions.assertTrue(htmxResp.body().contains("product-card") || htmxResp.body().contains("Quick Add"));
    }

    /**
     * Test that the homepage content is correctly localized based on the verla_country cookie.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testHomepageLocalization() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Request homepage with German (DE) country cookie -> should return German translations
        final HttpRequest deReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/index.html"))
            .header("Cookie", "verla_country=DE")
            .GET()
            .build();
        final HttpResponse<String> deResp = client.send(deReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, deResp.statusCode());
        Assertions.assertTrue(deResp.body().contains("Sorgfältig kuratierter täglicher Komfort."));
        Assertions.assertTrue(deResp.body().contains("Sommer-Essentials"));

        // 2. Request homepage with Japanese (JP) country cookie -> should return Japanese translations
        final HttpRequest jpReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-perfect/index.html"))
            .header("Cookie", "verla_country=JP")
            .GET()
            .build();
        final HttpResponse<String> jpResp = client.send(jpReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, jpResp.statusCode());
        Assertions.assertTrue(jpResp.body().contains("入念にセレクトされた日常の快適さ。"));
        Assertions.assertTrue(jpResp.body().contains("サマー・エッセンシャル"));
    }
}

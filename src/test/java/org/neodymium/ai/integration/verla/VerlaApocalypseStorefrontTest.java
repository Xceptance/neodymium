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
package org.neodymium.ai.integration.verla;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.junit5.NeodymiumTest;

/**
 * End-to-end integration and DOM verification test suite for the VÉRLA Apocalypse storefront variant (/verla-apocalypse/).
 * Validates the extreme-chaos architecture patterns: Web Components, Progressive Hydration traps,
 * Cumulative Layout Shift (CLS) banner, Pure CSS pseudo-element text, 4-box split credit card inputs,
 * bouncing live chat widgets, social proof toasts, and spin-to-win discount overlays.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("integration")
@Tag("verla")
public class VerlaApocalypseStorefrontTest extends BaseAiTest
{
    private final HttpClient client = HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .build();

    /**
     * Constructs a default VerlaApocalypseStorefrontTest instance.
     */
    public VerlaApocalypseStorefrontTest()
    {
    }

    /**
     * Test that the homepage loads with the full Apocalypse shell, CLS banner, progressive hydration triggers,
     * pure CSS pseudo-element text spans, live chat concierge, social proof toast, and spin-to-win modal.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testApocalypseHomepageStructure() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        final HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-apocalypse/index.html"))
            .GET()
            .build();
        final HttpResponse<String> resp = this.client.send(req, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, resp.statusCode());
        final String body = resp.body();

        // 1. Root DOM & Apocalypse Luxury Styling
        Assertions.assertTrue(body.contains("apocalypse-shell"));
        Assertions.assertTrue(body.contains("VÉRLA <span>HAUTE</span>"));

        // 2. CLS Layout Shift Announcement Bar
        Assertions.assertTrue(body.contains("id=\"apoc-cls-announcement\""));
        Assertions.assertTrue(body.contains("APOCALYPSE15"));

        // 3. Progressive Hydration Markers
        Assertions.assertTrue(body.contains("data-interactive=\"true\""));
        Assertions.assertTrue(body.contains("hydration-indicator"));
        Assertions.assertTrue(body.contains("__HYDRATED__ = false"));

        // 4. Pure CSS Pseudo-Element Text Labels
        Assertions.assertTrue(body.contains("ps-txt-shop-now"));
        Assertions.assertTrue(body.contains("ps-txt-trending"));
        Assertions.assertTrue(body.contains("ps-txt-add-to-bag"));

        // 5. Floating Overlays & Widgets
        Assertions.assertTrue(body.contains("id=\"apoc-chat-widget\""));
        Assertions.assertTrue(body.contains("id=\"apoc-chat-trigger-btn\""));
        Assertions.assertTrue(body.contains("id=\"apoc-social-toast\""));
        Assertions.assertTrue(body.contains("id=\"apoc-spin-modal-overlay\""));
        Assertions.assertTrue(body.contains("id=\"apoc-gdpr-banner\""));
        Assertions.assertTrue(body.contains("id=\"apoc-mini-cart-drawer\""));

        // 6. Featured Product Cards
        Assertions.assertTrue(body.contains("class=\"apoc-product-tile\""));
        Assertions.assertTrue(body.contains("class=\"apoc-btn-gold product-quick-add\""));
        Assertions.assertTrue(body.contains("quickAddApocalypseProduct"));

        // 7. Country Selector Trigger & Resolved Flag/Name
        Assertions.assertTrue(body.contains("id=\"country-trigger-btn\""));
        Assertions.assertTrue(body.contains("United States ($)"));
        Assertions.assertFalse(body.contains("${activeCountry"));
        Assertions.assertFalse(body.contains("${country_"));
    }

    /**
     * Test country switching on Apocalypse storefront via cookie update.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testApocalypseCountrySwitch() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Call api/country/select?code=DE using a raw client to assert the Set-Cookie header
        final HttpClient rawClient = HttpClient.newHttpClient();
        final HttpRequest selectReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-apocalypse/api/country/select?code=DE"))
            .GET()
            .build();
        final HttpResponse<String> selectResp = rawClient.send(selectReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, selectResp.statusCode());
        final boolean hasCountryCookie = selectResp.headers().allValues("Set-Cookie").stream().anyMatch(s -> s.contains("verla_country=DE"));
        Assertions.assertTrue(hasCountryCookie, "Set-Cookie header for verla_country=DE was not found in: " + selectResp.headers().allValues("Set-Cookie"));

        // 2. Query with Germany (DE) country cookie
        final HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-apocalypse/index.html"))
            .header("Cookie", "verla_country=DE")
            .GET()
            .build();
        final HttpResponse<String> resp = rawClient.send(req, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, resp.statusCode());
        final String body = resp.body();

        Assertions.assertTrue(body.contains("Deutschland (€)") || body.contains("Germany (€)"));
        Assertions.assertTrue(body.contains("selectApocalypseCountry('DE')"));
        Assertions.assertFalse(body.contains("${activeCountry"));
        Assertions.assertFalse(body.contains("${country_"));
    }

    /**
     * Test Category PLP filtration and sorting on Apocalypse variant.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testApocalypseCategoryPlp() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        final HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-apocalypse/c/tops.html"))
            .GET()
            .build();
        final HttpResponse<String> resp = this.client.send(req, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, resp.statusCode());
        final String body = resp.body();

        Assertions.assertTrue(body.contains("id=\"apoc-filter-form\""));
        Assertions.assertTrue(body.contains("id=\"plp-product-grid\""));
        Assertions.assertTrue(body.contains("apoc-product-tile"));
    }

    /**
     * Test PDP Product Detail page structure and size selection.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testApocalypsePdpStructure() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        final HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-apocalypse/p/premium-off-white-shirts-0.html"))
            .GET()
            .build();
        final HttpResponse<String> resp = this.client.send(req, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, resp.statusCode());
        final String body = resp.body();

        Assertions.assertTrue(body.contains("id=\"apoc-pdp-add-form\""));
        Assertions.assertTrue(body.contains("name=\"size\""));
        Assertions.assertTrue(body.contains("id=\"apoc-pdp-add-btn\""));
        Assertions.assertTrue(body.contains("ps-txt-in-stock"));
        Assertions.assertTrue(body.contains("submitApocalypsePdpAdd"));
        Assertions.assertFalse(body.contains("${product_"));
    }

    /**
     * Test Cart operations and Checkout page with split 4-box credit card inputs.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testApocalypseCartAndCheckoutFlow() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Add item to cart
        final HttpRequest addReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-apocalypse/api/cart/add"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("productId=SKU-TOP-1000&size=L"))
            .build();
        final HttpResponse<String> addResp = this.client.send(addReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertTrue(addResp.statusCode() == 200 || addResp.statusCode() == 302);

        // 1b. View Mini-Cart Drawer
        final HttpRequest drawerReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-apocalypse/api/cart/drawer"))
            .GET()
            .build();
        final HttpResponse<String> drawerResp = this.client.send(drawerReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, drawerResp.statusCode());
        Assertions.assertTrue(drawerResp.body().contains("drawer-item"));
        Assertions.assertTrue(drawerResp.body().contains("SKU-TOP-1000"));

        // 2. View Cart
        final HttpRequest cartReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-apocalypse/cart.html"))
            .GET()
            .build();
        final HttpResponse<String> cartResp = this.client.send(cartReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, cartResp.statusCode());
        Assertions.assertTrue(cartResp.body().contains("id=\"cart-checkout-btn\""));
        Assertions.assertTrue(cartResp.body().contains("cart-item-row"));

        // 3. View Checkout
        final HttpRequest checkoutReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-apocalypse/checkout.html"))
            .GET()
            .build();
        final HttpResponse<String> checkoutResp = this.client.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, checkoutResp.statusCode());
        final String checkoutBody = checkoutResp.body();

        // Verify Split Credit Card inputs, cardholder name, and hidden compatibility field
        Assertions.assertTrue(checkoutBody.contains("id=\"shipping-country\""));
        Assertions.assertTrue(checkoutBody.contains("id=\"shipping-address-fields\""));
        Assertions.assertTrue(checkoutBody.contains("id=\"cardName\""));
        Assertions.assertTrue(checkoutBody.contains("id=\"card-part-1\""));
        Assertions.assertTrue(checkoutBody.contains("id=\"card-part-2\""));
        Assertions.assertTrue(checkoutBody.contains("id=\"card-part-3\""));
        Assertions.assertTrue(checkoutBody.contains("id=\"card-part-4\""));
        Assertions.assertTrue(checkoutBody.contains("id=\"cardNumber\""));
        Assertions.assertTrue(checkoutBody.contains("id=\"apoc-purchase-btn\""));

        // 3b. Verify dynamic address form endpoint for JP and DE
        final HttpRequest jpAddrReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-apocalypse/api/address-form?country=JP&prefix=shipping-"))
            .GET()
            .build();
        final HttpResponse<String> jpAddrResp = this.client.send(jpAddrReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, jpAddrResp.statusCode());
        Assertions.assertTrue(jpAddrResp.body().contains("id=\"shipping-prefecture\""));
        Assertions.assertTrue(jpAddrResp.body().contains("class=\"apoc-form-control\""));

        // 4. Submit Purchase
        final HttpRequest purchaseReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-apocalypse/api/checkout/purchase"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(
                "firstName=John&lastName=Doe&email=john@example.com&cardName=John+Doe&address=123+Broadway&city=New+York&state=NY&postcode=10001&country=US&cardNumber=4111111111111100&cardExpiry=12/29&cardCvv=111"))
            .build();
        final HttpResponse<String> purchaseResp = this.client.send(purchaseReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, purchaseResp.statusCode());
        Assertions.assertTrue(purchaseResp.body().contains("Thank you for your purchase!"));
    }

    /**
     * Test auxiliary pages (About, FAQ, Contact, Careers, Shipping, Stores) render correctly.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testApocalypseAuxiliaryPages() throws IOException, InterruptedException
    {
        final int port = server.getPort();
        final String[] pages = { "about.html", "contact.html", "faq.html", "careers.html", "shipping.html", "stores.html", "login.html", "register.html", "track-orders.html" };

        for (final String page : pages)
        {
            final HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/verla-apocalypse/" + page))
                .GET()
                .build();
            final HttpResponse<String> resp = this.client.send(req, HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(200, resp.statusCode(), "Failed loading page: " + page);
        }
    }
}

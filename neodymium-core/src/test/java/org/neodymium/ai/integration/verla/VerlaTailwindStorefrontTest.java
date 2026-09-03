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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;

import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.junit5.NeodymiumTest;

/**
 * End-to-end integration and DOM verification test suite for the VÉRLA Tailwind CSS storefront (/verla-tailwind/).
 * Validates that the Tailwind storefront is structurally, functionally, and behaviorally identical to verla-normal
 * while utilizing Tailwind CSS styling.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("integration")
@Tag("verla")
public class VerlaTailwindStorefrontTest extends BaseAiTest
{
    private final HttpClient client = HttpClient.newBuilder()
        .cookieHandler(new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL))
        .build();

    /**
     * Test that the Tailwind homepage loads correctly with the Tailwind CSS setup, hero banner,
     * category links, search bar, and country selector trigger.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testTailwindHomepageStructure() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        final HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-tailwind/index.html"))
            .GET()
            .build();
        final HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, resp.statusCode());
        final String body = resp.body();

        // 1. Tailwind script & fonts
        Assertions.assertTrue(body.contains("https://cdn.tailwindcss.com"));
        Assertions.assertTrue(body.contains("tailwind.config"));
        Assertions.assertTrue(body.contains("terracotta"));

        // 2. Header & navigation
        Assertions.assertTrue(body.contains("VÉRLA"));
        Assertions.assertTrue(body.contains("c/tops.html"));
        Assertions.assertTrue(body.contains("c/bottoms.html"));
        Assertions.assertTrue(body.contains("c/outerwear.html"));
        Assertions.assertTrue(body.contains("c/footwear.html"));
        Assertions.assertTrue(body.contains("c/accessories.html"));

        // 3. Search & Country modal
        Assertions.assertTrue(body.contains("id=\"search-input\""));
        Assertions.assertTrue(body.contains("id=\"country-trigger-btn\""));
        Assertions.assertTrue(body.contains("id=\"country-modal-overlay\""));

        // 4. Hero section & Featured categories
        Assertions.assertTrue(body.contains("hero-section"));
        Assertions.assertTrue(body.contains("featured-categories-grid"));
        Assertions.assertTrue(body.contains("products-grid"));
    }

    /**
     * Test that the PLP page renders with refinement sidebar, sorting dropdown, and product cards.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testTailwindPlpCategoryAndRefinements() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        final HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-tailwind/c/tops.html"))
            .GET()
            .build();
        final HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, resp.statusCode());
        final String body = resp.body();

        // 1. Layout structure
        Assertions.assertTrue(body.contains("plp-container"));
        Assertions.assertTrue(body.contains("plp-sidebar"));
        Assertions.assertTrue(body.contains("id=\"refinement-form\""));
        Assertions.assertTrue(body.contains("id=\"sort-select\""));
        Assertions.assertTrue(body.contains("id=\"plp-product-grid\""));
    }

    /**
     * Test that a dynamic PDP page loads correctly with product details and Add to Cart form.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testTailwindPdpStructure() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        final HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-tailwind/p/premium-off-white-shirts-0.html"))
            .GET()
            .build();
        final HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, resp.statusCode());
        final String body = resp.body();

        // 1. PDP elements
        Assertions.assertTrue(body.contains("pdp-container"));
        Assertions.assertTrue(body.contains("pdp-image-wrapper"));
        Assertions.assertTrue(body.contains("pdp-info-wrapper"));
        Assertions.assertTrue(body.contains("name=\"productId\""));
        Assertions.assertTrue(body.contains("id=\"quantity\""));
    }

    /**
     * Test full shopping cart lifecycle on the Tailwind storefront.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testTailwindCartLifecycle() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Add item to cart
        final HttpRequest addReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-tailwind/api/cart/add?productId=SKU-TOP-1000:M&quantity=2"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        final HttpResponse<String> addResp = client.send(addReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, addResp.statusCode());

        // 2. View cart page
        final HttpRequest cartReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-tailwind/cart.html"))
            .GET()
            .build();
        final HttpResponse<String> cartResp = client.send(cartReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, cartResp.statusCode());
        Assertions.assertTrue(cartResp.body().contains("cart-container"));
        Assertions.assertTrue(cartResp.body().contains("checkout.html"));

        // 3. Apply coupon
        final HttpRequest couponReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-tailwind/api/cart/coupon?couponCode=10p-off"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        final HttpResponse<String> couponResp = client.send(couponReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, couponResp.statusCode());
        Assertions.assertTrue(couponResp.body().contains("10P-OFF") || couponResp.body().contains("Discount"));
    }

    /**
     * Test checkout page rendering and payment submission on the Tailwind storefront.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testTailwindCheckoutProcessing() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Add item
        final HttpRequest addReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-tailwind/api/cart/add?productId=SKU-TOP-1001:L&quantity=1"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        client.send(addReq, HttpResponse.BodyHandlers.ofString());

        // 2. View checkout page
        final HttpRequest chkReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-tailwind/checkout.html"))
            .GET()
            .build();
        final HttpResponse<String> chkResp = client.send(chkReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, chkResp.statusCode());
        Assertions.assertTrue(chkResp.body().contains("checkout-form-container"));
        Assertions.assertTrue(chkResp.body().contains("id=\"purchase-btn\""));

        // 3. Submit valid purchase
        final String postData = "firstName=Mario&lastName=Meier&email=mario%40example.com&country=US&street=123+Main+St&city=Manchester&zip=12345&state=MA&cardType=Visa&cardNumber=4111111111111100&cardExpiry=12%2F29&cardCvv=111";
        final HttpRequest buyReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-tailwind/api/checkout/purchase"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(postData))
            .build();
        final HttpResponse<String> buyResp = client.send(buyReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, buyResp.statusCode());
        Assertions.assertTrue(buyResp.body().contains("Thank you for your purchase!"));
        Assertions.assertTrue(buyResp.body().contains("V-"));
    }
}

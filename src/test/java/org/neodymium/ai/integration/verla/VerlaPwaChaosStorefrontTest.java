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
 * End-to-end integration and DOM verification test suite for the VÉRLA PWA Chaos storefront variant (/verla-pwa-chaos/).
 * Validates the Bath &amp; Body Works / Wick Design System / Emotion CSS / Contentstack architecture patterns.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Tag("integration")
@Tag("verla")
public class VerlaPwaChaosStorefrontTest extends BaseAiTest
{
    private final HttpClient client = HttpClient.newBuilder()
        .cookieHandler(new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL))
        .build();

    /**
     * Test that the homepage loads with the full PWA shell, Emotion CSS classes, Wick design system elements,
     * marquee slider ticker, VideoJS error modal, OneTrust cookie banner, and mini-cart drawer.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testPwaChaosHomepageStructure() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        final HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-pwa-chaos/index.html"))
            .GET()
            .build();
        final HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, resp.statusCode());
        final String body = resp.body();

        // 1. Root DOM & CSS-in-JS emotion wrappers
        Assertions.assertTrue(body.contains("homepage-redesign react chakra-ui-light"));
        Assertions.assertTrue(body.contains("react-target"));
        Assertions.assertTrue(body.contains("wick-header__root"));
        Assertions.assertTrue(body.contains("wick-header__bannerWrapper"));
        Assertions.assertTrue(body.contains("wick-banner__root"));

        // 2. Marquee banner slider & Zag.js machine attributes
        Assertions.assertTrue(body.contains("data-scope=\"marquee\""));
        Assertions.assertTrue(body.contains("data-part=\"root\""));
        Assertions.assertTrue(body.contains("promo-banner-slider"));

        // 3. Search bar with trigger prompt rotating text
        Assertions.assertTrue(body.contains("wick-global-search__triggerPrompt"));

        // 4. VideoJS Decode Error Modal dialog
        Assertions.assertTrue(body.contains("vjs-error-modal"));
        Assertions.assertTrue(body.contains("MEDIA_ERR_DECODE"));
        Assertions.assertTrue(body.contains("The video is bad or in a format that cannot be played on your browser"));

        // 5. OneTrust Cookie Banner
        Assertions.assertTrue(body.contains("onetrust-banner"));
        Assertions.assertTrue(body.contains("Accept All Cookies"));

        // 6. Delayed Promotional Modal
        Assertions.assertTrue(body.contains("marketing-modal-overlay"));
        Assertions.assertTrue(body.contains("Get 20% Off!"));

        // 7. Mini-cart slide-over drawer
        Assertions.assertTrue(body.contains("pwa-drawer"));
        Assertions.assertTrue(body.contains("pwa-drawer-backdrop"));

        // 8. Contentstack modules & Geometric reveal footer SVG pattern
        Assertions.assertTrue(body.contains("data-contentstack-uid=\"blt68bf107b23c3f073\""));
        Assertions.assertTrue(body.contains("verla-pattern-reveal"));
        Assertions.assertTrue(body.contains("pattern id=\"verla-geom-pat\""));
    }

    /**
     * Test PLP product grid rendering with 12-layer deep Wick Linkbox, Emotion tiles, Zag.js steppers, and sticky BOPIS bar.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testPwaChaosPlpRendering() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Tops Category Page
        final HttpRequest plpReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-pwa-chaos/c/tops.html"))
            .GET()
            .build();
        final HttpResponse<String> plpResp = client.send(plpReq, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, plpResp.statusCode());
        final String body = plpResp.body();

        Assertions.assertTrue(body.contains("sf-product-list-page"));
        Assertions.assertTrue(body.contains("breadcrumb-wrapper-mobile"));
        Assertions.assertTrue(body.contains("breadcrumb-wrapper-desktop"));
        Assertions.assertTrue(body.contains("wick-sticky-scroll__root"));
        Assertions.assertTrue(body.contains("bopis-filter--checkbox"));
        Assertions.assertTrue(body.contains("data-dan-component=\"product-tile\""));
        Assertions.assertTrue(body.contains("data-dan-component=\"product-price--now-price\""));
        Assertions.assertTrue(body.contains("wick-number-stepper__root"));
        Assertions.assertTrue(body.contains("product-quick-add"));

        // 2. Tops Category Page via PWA Router Navigation (X-PWA-Router: true)
        final HttpRequest pwaNavReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-pwa-chaos/c/tops.html"))
            .header("X-PWA-Router", "true")
            .GET()
            .build();
        final HttpResponse<String> pwaNavResp = client.send(pwaNavReq, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, pwaNavResp.statusCode());
        final String pwaBody = pwaNavResp.body();

        Assertions.assertTrue(pwaBody.contains("sf-product-list-page"));
        Assertions.assertTrue(pwaBody.contains("plp-layout-container"));
        Assertions.assertTrue(pwaBody.contains("sf-refinement-sidebar"));
        Assertions.assertTrue(pwaBody.contains("products-grid"));
        Assertions.assertTrue(pwaBody.contains("data-dan-component=\"product-tile\""));
    }

    /**
     * Test PWA Micro-router asynchronous snippet endpoint for live product grid filtering.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testPwaRouterSnippetLoading() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        final HttpRequest snippetReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-pwa-chaos/api/snippets/products?category=tops&sort=price-asc"))
            .header("X-PWA-Router", "true")
            .GET()
            .build();
        final HttpResponse<String> snippetResp = client.send(snippetReq, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, snippetResp.statusCode());
        final String body = snippetResp.body();

        Assertions.assertTrue(body.contains("data-dan-component=\"product-tile\""));
        Assertions.assertTrue(body.contains("wick-number-stepper__root"));
    }

    /**
     * Test PDP product details page structure.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testPwaChaosPdpRendering() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        final HttpRequest pdpReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-pwa-chaos/p/premium-off-white-shirts-0.html"))
            .GET()
            .build();
        final HttpResponse<String> pdpResp = client.send(pdpReq, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, pdpResp.statusCode());
        final String body = pdpResp.body();

        Assertions.assertTrue(body.contains("pdp-container"));
        Assertions.assertTrue(body.contains("pdp-image-wrapper"));
        Assertions.assertTrue(body.contains("pdp-info-wrapper"));
        Assertions.assertTrue(body.contains("api/cart/add"));
    }

    /**
     * Test full cart and checkout purchase flow on PWA Chaos storefront.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testPwaChaosCheckoutFlow() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Add item to cart
        final HttpRequest addReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-pwa-chaos/api/cart/add"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("productId=SKU-TOP-1000&quantity=2"))
            .build();
        final HttpResponse<String> addResp = client.send(addReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, addResp.statusCode());

        // 2. View Cart page
        final HttpRequest cartReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-pwa-chaos/cart.html"))
            .GET()
            .build();
        final HttpResponse<String> cartResp = client.send(cartReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, cartResp.statusCode());
        final String cartBody = cartResp.body();
        Assertions.assertTrue(cartBody.contains("sf-cart-container"));
        Assertions.assertTrue(cartBody.contains("cart-layout-grid"));
        Assertions.assertTrue(cartBody.contains("cart-main-col"));
        Assertions.assertTrue(cartBody.contains("cart-sidebar-col"));
        Assertions.assertTrue(cartBody.contains("data-cart-product-row=\"true\""));
        Assertions.assertTrue(cartBody.contains("sf-order-summary"));
        Assertions.assertTrue(cartBody.contains("cart--checkout-button"));

        // 3. View Checkout page
        final HttpRequest checkoutReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-pwa-chaos/checkout.html"))
            .GET()
            .build();
        final HttpResponse<String> checkoutResp = client.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, checkoutResp.statusCode());
        final String checkoutBody = checkoutResp.body();
        Assertions.assertTrue(checkoutBody.contains("sf-checkout-page"));
        Assertions.assertTrue(checkoutBody.contains("checkout--progress-bar"));
        Assertions.assertTrue(checkoutBody.contains("sf-toggle-card-step-1"));
        Assertions.assertTrue(checkoutBody.contains("sf-order-summary"));

        // 4. Place order with valid simulation credit card
        final HttpRequest purchaseReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-pwa-chaos/api/checkout/purchase"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("firstName=Jane&lastName=Doe&email=jane@example.com&country=US&street=123+Main+St&city=Columbus&state=OH&postcode=43219&cardType=Visa&cardNumber=1111222233334100&cardExpiry=12/29&cardCvv=123"))
            .build();
        final HttpResponse<String> purchaseResp = client.send(purchaseReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, purchaseResp.statusCode());
        Assertions.assertTrue(purchaseResp.body().contains("Order Number") || purchaseResp.body().contains("V-"));
    }

    /**
     * Test all informational and auxiliary public pages load correctly in PWA Chaos variant.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testPwaChaosInformationalPages() throws IOException, InterruptedException
    {
        final int port = server.getPort();
        final String[] pages = new String[] {
            "about.html", "careers.html", "contact.html", "faq.html", "shipping.html",
            "stores.html", "track-orders.html", "login.html", "register.html"
        };

        for (final String page : pages)
        {
            final HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/verla-pwa-chaos/" + page))
                .GET()
                .build();
            final HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(200, resp.statusCode(), "Failed to load page: " + page);
            Assertions.assertTrue(resp.body().contains("wick-header__root"), "Page " + page + " missing PWA shell header");
            Assertions.assertTrue(resp.body().contains("verla-geom-pat"), "Page " + page + " missing footer pattern");
        }
    }

    /**
     * Test account dashboard rendering when authenticated in PWA Chaos variant.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testPwaChaosAccountDashboard() throws IOException, InterruptedException
    {
        final int port = server.getPort();

        // 1. Register a new user session
        final HttpRequest regReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-pwa-chaos/api/auth/register"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .PUT(HttpRequest.BodyPublishers.ofString("email=pwauser@example.com&password=secret123&confirmPassword=secret123"))
            .build();
        final HttpResponse<String> regResp = client.send(regReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, regResp.statusCode());

        // 2. Load account.html with active session cookie
        final HttpRequest accReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/verla-pwa-chaos/account.html"))
            .GET()
            .build();
        final HttpResponse<String> accResp = client.send(accReq, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, accResp.statusCode());
        final String body = accResp.body();
        Assertions.assertTrue(body.contains("account-page-container"));
        Assertions.assertTrue(body.contains("account-sidebar"));
        Assertions.assertTrue(body.contains("wick-header__root"));
    }
}

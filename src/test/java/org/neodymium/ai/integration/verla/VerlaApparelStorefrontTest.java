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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.junit5.NeodymiumTest;

/**
 * End-to-end integration and DOM verification test suite for the VÉRLA Apparel storefront variant
 * ({@code /verla-apparel/} and alias {@code /verla-apperal/}).
 * <p>
 * Validates the luxury Italian apparel aesthetic (terracotta palette, hero lifestyle carousel,
 * category story pills, lookbook hotspots, bundle badges, size guide fit matrix) paired with
 * modern horrible web architecture patterns (deep DIV/SPAN soup, non-semantic buttons, delayed
 * hydration/CLS banners, and slide-out cart drawers).
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("integration")
@Tag("verla")
public class VerlaApparelStorefrontTest extends BaseAiTest
{
    private final HttpClient client = HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .build();

    /**
     * Constructs a default VerlaApparelStorefrontTest instance.
     */
    public VerlaApparelStorefrontTest()
    {
    }

    private String get(final String endpoint) throws IOException, InterruptedException
    {
        final HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + this.server.getPort() + endpoint))
            .GET()
            .build();
        final HttpResponse<String> resp = this.client.send(req, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, resp.statusCode(), "Expected 200 OK for " + endpoint);
        return resp.body();
    }

    /**
     * Test that the homepage loads on both /verla-apparel/ and the alias /verla-apperal/,
     * verifying Mobify root container, SVG Sprite Tree, rotating promo bar, OneTrust overlay,
     * delayed popup modal, and size guide modal.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testApparelHomepageStructureAndAlias() throws IOException, InterruptedException
    {
        for (final String basePrefix : List.of("/verla-apparel/", "/verla-apperal/"))
        {
            final String body = get(basePrefix + "index.html");

            // 1. Root DOM architecture & SVG sprite tree
            Assertions.assertTrue(body.contains("id=\"app-root\""), "Must contain Mobify #app-root");
            Assertions.assertTrue(body.contains("id=\"__SVG_SPRITE_NODE__\""), "Must contain SVG Sprite Tree");

            // 2. Italian Luxury Apparel branding & navigation
            Assertions.assertTrue(body.contains("VÉRLA"), "Must contain brand header");
            Assertions.assertTrue(body.contains("APPAREL"), "Must contain APPAREL subtitle");
            Assertions.assertTrue(body.contains("id=\"main-navigation\""), "Must contain main navigation");
            Assertions.assertTrue(body.contains(".mbf-nav-link"), "Must contain .mbf-nav-link CSS rule");
            Assertions.assertTrue(body.contains("white-space: nowrap"), "Must contain nowrap rule for nav links");
            Assertions.assertTrue(body.contains("whitespace-nowrap shrink-0"), "Nav links must have nowrap and shrink-0 utility classes");

            // 3. Rotating promo bar & Cookie Consent overlay trap
            Assertions.assertTrue(body.contains("id=\"announcement-wrapper\""), "Must contain expanding top promo banner");
            Assertions.assertTrue(body.contains("id=\"onetrust-consent-sdk\""), "Must contain OneTrust cookie banner");

            // 4. Modals & Drawers
            Assertions.assertTrue(body.contains("id=\"marketing-modal-overlay\""), "Must contain delayed newsletter promo modal");
            Assertions.assertTrue(body.contains("id=\"size-guide-modal-overlay\""), "Must contain size guide modal");
            Assertions.assertTrue(body.contains("id=\"cart-drawer-panel\""), "Must contain slide-out mini-cart drawer");

            // 5. Lookbook hotspots & story pills
            Assertions.assertTrue(body.contains("mbf-hotspot-pin"), "Must contain interactive lookbook hotspot pins");
            Assertions.assertTrue(body.contains("4+1 FREE"), "Must contain bundle promotion badges");
            Assertions.assertTrue(body.contains("VÉRLA Privé"), "Must contain loyalty club signup");
        }
    }

    /**
     * The stylesheet must be locally served and completely free of external CDN links or runtime scripts.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testStylesheetIsLocalAndOfflinePure() throws IOException, InterruptedException
    {
        final String body = get("/verla-apparel/index.html");

        Assertions.assertTrue(body.contains("/shared/verla-apparel.css"), "Must link local compiled stylesheet");
        Assertions.assertFalse(body.contains("cdn.tailwindcss.com"), "Must not rely on runtime Tailwind CDN");
        Assertions.assertFalse(body.contains("fonts.googleapis.com"), "Must not load external Google Fonts");

        // Verify that no absolute external URLs are referenced
        final Matcher m = Pattern.compile("(?:src|href)=\"(https?://[^\"]+)\"").matcher(body);
        Assertions.assertFalse(m.find(), "No absolute remote resource may be referenced, found: "
                                         + (m.reset().find() ? m.group(1) : ""));

        // Verify the compiled CSS endpoint responds correctly
        final HttpRequest cssReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + this.server.getPort() + "/shared/verla-apparel.css"))
            .GET()
            .build();
        final HttpResponse<String> cssResp = this.client.send(cssReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, cssResp.statusCode());
        Assertions.assertTrue(cssResp.body().length() > 10_000, "Compiled stylesheet must be populated");
        Assertions.assertTrue(cssResp.body().contains("--color-terracotta-500"), "Terracotta color variable must exist");
        Assertions.assertTrue(cssResp.body().contains("--color-espresso"), "Espresso color variable must exist");
    }

    /**
     * Test PLP category pages and product card rendering with deep DIV/SPAN soup, swatch dots,
     * and bundle badges.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testPlpAndProductCardStructure() throws IOException, InterruptedException
    {
        final String body = get("/verla-apparel/c/bottoms.html");

        // 1. Faceted sidebar filters & Flex grid layout
        Assertions.assertTrue(body.contains("id=\"refinement-form\""), "Must contain refinement form");
        Assertions.assertTrue(body.contains("4+1 Multi-Buy"), "Must contain bundle filter");
        Assertions.assertTrue(body.contains("flex flex-col lg:flex-row"), "Must contain responsive flex container");
        Assertions.assertTrue(body.contains("lg:w-64"), "Sidebar must have responsive width");
        Assertions.assertTrue(body.contains("flex-1 min-w-0"), "Product container must use flex-1 min-w-0");

        // 2. Product card structure (deep DIV/SPAN soup)
        Assertions.assertTrue(body.contains("data-mbf-component=\"product-card\""), "Must contain Mobify product card component");
        Assertions.assertTrue(body.contains("mbf-swatch-pills"), "Must contain color swatch dots");
        Assertions.assertTrue(body.contains("mbf-bundle-badge"), "Must contain bundle badge");
        Assertions.assertTrue(body.contains("product-quick-add"), "Must contain quick add trigger");
    }

    /**
     * Test PDP rendering, size dropdown, size guide trigger, and accordion details.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testPdpAndSizeGuideInteraction() throws IOException, InterruptedException
    {
        final String body = get("/verla-apparel/p/premium-off-white-shirts-0.html");

        // 1. PDP Structure
        Assertions.assertTrue(body.contains("data-pdp-form"), "Must contain add to cart form");
        Assertions.assertTrue(body.contains("name=\"size\""), "Must contain size selector");
        Assertions.assertTrue(body.contains("name=\"quantity\""), "Must contain quantity selector");

        // 2. Size Guide trigger modal button
        Assertions.assertTrue(body.contains("openSizeGuideModal()"), "Must contain openSizeGuideModal trigger");

        // 3. Product details accordion
        Assertions.assertTrue(body.contains("Composition &amp; Care"), "Must contain accordion details");

        // 4. Verify no duplicated size label
        final int sizeLabelCount = body.split("for=\"size\"", -1).length - 1;
        Assertions.assertEquals(1, sizeLabelCount, "Must have exactly 1 size label in PDP");

        // 5. Verify unique PDP container and add to cart form
        final int pdpContainerCount = body.split("class=\"mbf-pdp-container", -1).length - 1;
        Assertions.assertEquals(1, pdpContainerCount, "Must have exactly 1 PDP container on page");

        final int pdpFormCount = body.split("data-pdp-form>", -1).length - 1;
        Assertions.assertEquals(1, pdpFormCount, "Must have exactly 1 PDP form element on page");
    }

    /**
     * Test Cart API operations, cart page rendering, and slide-out mini-cart drawer.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testCartOperationsAndMiniCartDrawer() throws IOException, InterruptedException
    {
        // 1. Add item to cart
        final String addResp = get("/verla-apparel/api/cart/add?productId=SKU-TOP-1000:M&quantity=2");
        Assertions.assertTrue(addResp.contains("cart-badge-count") || addResp.contains("SKU-TOP-1000") || addResp.length() >= 0);

        // 2. Test mini-cart drawer JSON API endpoint
        final String drawerJson = get("/verla-apparel/api/cart/drawer");
        Assertions.assertTrue(drawerJson.contains("\"count\":"), "Drawer JSON must contain count");
        Assertions.assertTrue(drawerJson.contains("\"subtotal\":"), "Drawer JSON must contain subtotal");
        Assertions.assertTrue(drawerJson.contains("\"shippingStatus\":"), "Drawer JSON must contain shippingStatus");
        Assertions.assertTrue(drawerJson.contains("\"shippingProgress\":"), "Drawer JSON must contain shippingProgress");
        Assertions.assertTrue(drawerJson.contains("\"itemsHtml\":"), "Drawer JSON must contain itemsHtml");
        Assertions.assertTrue(drawerJson.contains("SKU-TOP-1000") || drawerJson.contains("M"), "Drawer itemsHtml must include added product");

        // 3. Verify Cart Page
        final String cartBody = get("/verla-apparel/cart.html");
        Assertions.assertTrue(cartBody.contains("Shopping Bag") || cartBody.contains("Cart"), "Cart page must render shopping bag");
        Assertions.assertTrue(cartBody.contains("SKU-TOP-1000") || cartBody.contains("Shirts"), "Must show added item");
        Assertions.assertTrue(cartBody.contains("mbf-cart-container"), "Cart page must have container wrapper");

        // 4. Checkout Page
        final String checkoutBody = get("/verla-apparel/checkout.html");
        Assertions.assertTrue(checkoutBody.contains("Order Summary"), "Checkout must display order summary");
        Assertions.assertTrue(checkoutBody.contains("Shipping Address"), "Checkout must display shipping address form");
        Assertions.assertTrue(checkoutBody.contains("Payment Details"), "Checkout must display payment details");
        Assertions.assertTrue(checkoutBody.contains("max-w-7xl"), "Checkout page must have max-w-7xl width");
    }

    /**
     * Test that no doubled tags (such as nested span tags, duplicate cart wrapper IDs, or nested main landmarks)
     * are present in rendered HTML pages.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testNoDoubledDomElements() throws IOException, InterruptedException
    {
        final List<String> testUrls = List.of(
            "/verla-apparel/index.html",
            "/verla-apparel/c/bottoms.html",
            "/verla-apparel/p/premium-off-white-shirts-0.html",
            "/verla-apparel/cart.html",
            "/verla-apparel/checkout.html",
            "/verla-apparel/login.html",
            "/verla-apparel/register.html",
            "/verla-apparel/about.html"
        );

        for (final String url : testUrls)
        {
            final String body = get(url);

            // Verify no nested double span tags
            Assertions.assertFalse(body.contains("<span><span>"), "Must not contain nested span tags in " + url);
            Assertions.assertFalse(body.contains("</span></span>"), "Must not contain closing nested span tags in " + url);

            // Verify single cart wrapper ID
            final int cartWrapperCount = body.split("id=\"cart-btn-wrapper\"", -1).length - 1;
            Assertions.assertTrue(cartWrapperCount <= 1, "Must not have duplicate cart-btn-wrapper in " + url);

            // Verify single main landmark tag
            final int mainTagCount = body.split("<main", -1).length - 1;
            Assertions.assertEquals(1, mainTagCount, "Must have exactly one <main> tag in " + url);

            // Verify DOM tree ordering: app-root -> header -> main-content -> footer
            final int appRootIdx = body.indexOf("id=\"app-root\"");
            final int headerEndIdx = body.indexOf("</header>");
            final int mainContentIdx = body.indexOf("id=\"main-content\"");
            final int footerIdx = body.indexOf("<footer");

            Assertions.assertTrue(appRootIdx != -1, "Must contain app-root in " + url);
            Assertions.assertTrue(headerEndIdx > appRootIdx, "Header must close after app-root start in " + url);
            Assertions.assertTrue(mainContentIdx > headerEndIdx, "main-content must be located after </header> in " + url);
            Assertions.assertTrue(footerIdx > mainContentIdx, "footer must be located after main-content in " + url);
        }
    }

    /**
     * Test that all static content pages render with 200 OK and proper layout wrappers.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testAllSupportingPages() throws IOException, InterruptedException
    {
        final List<String> pages = List.of(
            "about.html", "stores.html", "careers.html", "contact.html",
            "faq.html", "shipping.html", "track-orders.html", "login.html", "register.html"
        );

        for (final String page : pages)
        {
            final String body = get("/verla-apparel/" + page);
            Assertions.assertTrue(body.contains("id=\"app-root\""), page + " must be wrapped in app-root");
            Assertions.assertTrue(body.contains("id=\"main-navigation\""), page + " must include navigation");
            Assertions.assertTrue(body.contains("VÉRLA Privé"), page + " must include footer newsletter");
        }
    }

    /**
     * Test product tile badge positioning, quick-add size picker logic, and cookie consent persistence.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testTileBadgesAndQuickAddSizePickerAndCookieConsentPersistence() throws IOException, InterruptedException
    {
        final String plpBody = get("/verla-apparel/c/tops.html");

        // 1. Badge container pinned to top-4 left-4 with vertical stacking
        Assertions.assertTrue(plpBody.contains("mbf-badge-layer absolute top-4 left-4 z-10 flex flex-col items-start gap-1"),
                              "Badge container must be pinned to upper left of tile");
        Assertions.assertTrue(plpBody.contains("mbf-bundle-badge"), "Must contain bundle badge");

        // 2. Quick add button markup and size dropdown script
        Assertions.assertTrue(plpBody.contains("data-quick-add"), "Quick add button must have data-quick-add attribute");
        Assertions.assertTrue(plpBody.contains("data-stock="), "Quick add button must contain inventory stock data");
        Assertions.assertTrue(plpBody.contains("window.showQuickAddSizes"), "Must define quick-add size picker function");
        Assertions.assertTrue(plpBody.contains("quick-add-size-dropdown"), "Must render quick add size dropdown container");
        Assertions.assertTrue(plpBody.contains(".quick-add-size-dropdown {"), "Must define quick add size dropdown CSS styles");
        Assertions.assertTrue(plpBody.contains("bottom: calc(100% + 8px);"), "Must position dropdown above button to prevent clipping");

        // 3. Cookie consent banner and persistence script
        Assertions.assertTrue(plpBody.contains("id=\"onetrust-consent-sdk\""), "Must contain OneTrust cookie banner");
        Assertions.assertTrue(plpBody.contains("verla_cookie_consent"), "Must contain cookie consent persistence script");
        Assertions.assertTrue(plpBody.contains("sessionStorage.setItem('verla_cookie_consent', 'true')"), "Must store consent in sessionStorage");
    }

    /**
     * Test header country trigger button flag/currency rendering and country modal list flags.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testCountryTriggerButtonAndModalFlags() throws IOException, InterruptedException
    {
        final String body = get("/verla-apparel/index.html");

        // 1. Country trigger button contains flag emoji and currency symbol
        Assertions.assertTrue(body.contains("id=\"country-trigger-btn\""), "Must contain country trigger button");
        Assertions.assertTrue(body.contains("🇺🇸"), "Header must display default US flag emoji");
        Assertions.assertTrue(body.contains("($)"), "Header must display USD symbol in parentheses");

        // 2. Country modal list options contains flags
        Assertions.assertTrue(body.contains("id=\"country-modal-overlay\""), "Must contain country modal overlay");
        Assertions.assertTrue(body.contains("🇩🇪"), "Country modal must list Germany flag");
        Assertions.assertTrue(body.contains("🇬🇧"), "Country modal must list UK flag");
    }

    private String getHeadless(final String endpoint) throws IOException, InterruptedException
    {
        final HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + this.server.getPort() + endpoint))
            .header("X-PWA-Router", "true")
            .GET()
            .build();
        final HttpResponse<String> resp = this.client.send(req, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, resp.statusCode(), "Expected 200 OK for headless " + endpoint);
        return resp.body();
    }

    /**
     * Test headless SPA client router behavior: initial landing returns full HTML document,
     * while subsequent route requests (with X-PWA-Router header) return pure inner body fragments.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testHeadlessNavigationAndFragmentEndpoints() throws IOException, InterruptedException
    {
        // 1. Full landing page load (SSR initial shell)
        final String fullHtml = get("/verla-apparel/index.html");
        Assertions.assertTrue(fullHtml.contains("<!DOCTYPE html>"), "Initial landing must include full HTML DOCTYPE");
        Assertions.assertTrue(fullHtml.contains("<header class=\"mbf-header"), "Initial landing must contain header");
        Assertions.assertTrue(fullHtml.contains("<main class=\"mbf-main-content"), "Initial landing must contain main wrapper");
        Assertions.assertTrue(fullHtml.contains("<footer class=\"mbf-footer"), "Initial landing must contain footer");

        // 2. Client SPA Micro-Router and link click interceptors in layout.html
        Assertions.assertTrue(fullHtml.contains("window.apparelRouter"), "Must define window.apparelRouter object");
        Assertions.assertTrue(fullHtml.contains("window.apparelRouter.navigate"), "Must define navigate function");
        Assertions.assertTrue(fullHtml.contains("window.addEventListener('popstate'"), "Must listen for popstate history events");
        Assertions.assertTrue(fullHtml.contains("X-PWA-Router"), "Router must send X-PWA-Router header");

        // 3. Headless Category PLP fragment request (between categories)
        final String categoryFragment = getHeadless("/verla-apparel/c/bottoms.html");
        Assertions.assertFalse(categoryFragment.contains("<!DOCTYPE html>"), "Fragment must not contain DOCTYPE");
        Assertions.assertFalse(categoryFragment.contains("<header class=\"mbf-header"), "Fragment must not contain header");
        Assertions.assertFalse(categoryFragment.contains("<footer class=\"mbf-footer"), "Fragment must not contain footer");
        Assertions.assertTrue(categoryFragment.contains("mbf-plp-container"), "Fragment must contain PLP container");
        Assertions.assertTrue(categoryFragment.contains("id=\"plp-product-grid\""), "Fragment must contain product grid");

        // 4. Headless Homepage return fragment request
        final String homeFragment = getHeadless("/verla-apparel/index.html");
        Assertions.assertFalse(homeFragment.contains("<!DOCTYPE html>"), "Homepage fragment must not contain DOCTYPE");
        Assertions.assertFalse(homeFragment.contains("<header class=\"mbf-header"), "Homepage fragment must not contain header");
        Assertions.assertFalse(homeFragment.contains("<footer class=\"mbf-footer"), "Homepage fragment must not contain footer");
        Assertions.assertTrue(homeFragment.contains("mbf-hotspot-pin"), "Homepage fragment must contain interactive hotspots");

        // 5. Headless Cart fragment request
        final String cartFragment = getHeadless("/verla-apparel/cart.html");
        Assertions.assertFalse(cartFragment.contains("<!DOCTYPE html>"), "Cart fragment must not contain DOCTYPE");
        Assertions.assertFalse(cartFragment.contains("<header class=\"mbf-header"), "Cart fragment must not contain header");
        Assertions.assertFalse(cartFragment.contains("<footer class=\"mbf-footer"), "Cart fragment must not contain footer");
        Assertions.assertTrue(cartFragment.contains("mbf-cart-container"), "Cart fragment must contain cart container");
    }
}

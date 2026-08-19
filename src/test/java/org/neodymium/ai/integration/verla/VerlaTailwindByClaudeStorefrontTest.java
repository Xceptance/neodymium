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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;

import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.junit5.NeodymiumTest;

/**
 * Verification suite for the utility-first VÉRLA storefront at /verla-tailwind-by-claude/.
 * <p>
 * Unlike /verla-tailwind/, this variant is Tailwind all the way down: the stylesheet is a
 * locally built, committed artifact (no CDN, no Node at runtime), the markup carries only
 * utility classes, and component state is expressed through data attributes rather than
 * stateful CSS classes. These tests guard exactly those properties, because they are the
 * ones that silently regress.
 *
 * @author Xceptance GmbH 2026
 */
@Tag("integration")
@Tag("verla")
public class VerlaTailwindByClaudeStorefrontTest extends BaseAiTest
{
    /** Semantic class names that must never reappear in this SUT. */
    private static final List<String> FORBIDDEN_CLASSES = List.of(
        "product-card", "product-media", "product-title", "product-price", "product-badge",
        "product-quick-add", "products-grid", "featured-categories-grid", "hero-section",
        "cart-container", "cart-main", "cart-sidebar", "cart-table", "cart-dropdown",
        "checkout-container", "checkout-main", "checkout-sidebar",
        "plp-container", "plp-sidebar", "plp-main", "pdp-container", "pdp-info-wrapper",
        "account-container", "account-sidebar", "account-main", "auth-card",
        "form-group", "form-label", "form-control", "form-grid-2", "form-grid-1-2",
        "error-message", "btn-primary", "btn-secondary", "utility-btn", "icon-svg",
        "announcement-bar", "header-grid", "nav-list", "nav-link", "footer-grid",
        "footer-heading", "footer-links", "footer-link", "modal-overlay", "search-bar-input",
        "search-suggestion-item", "country-item", "cart-badge", "size-btn");

    private static final Pattern CLASS_ATTR = Pattern.compile("class=\"([^\"]*)\"");

    private final HttpClient client = HttpClient.newBuilder()
        .cookieHandler(new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL))
        .build();

    private String get(final String path) throws IOException, InterruptedException
    {
        final HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/verla-tailwind-by-claude/" + path))
            .GET()
            .build();
        final HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, resp.statusCode(), "Expected 200 for " + path);
        return resp.body();
    }

    /**
     * The stylesheet must be a locally served artifact. If this fails, someone reintroduced a
     * CDN and the SUT can no longer run offline.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testStylesheetIsLocalAndNoRemoteResources() throws IOException, InterruptedException
    {
        final String body = get("index.html");

        Assertions.assertTrue(body.contains("/shared/verla-tailwind.css"), "must link the locally built stylesheet");
        Assertions.assertFalse(body.contains("cdn.tailwindcss.com"), "must not load the Tailwind Play CDN");
        Assertions.assertFalse(body.contains("tailwind.config"), "must not ship a runtime Tailwind config");

        // No external origins at all: every src/href is same-origin or relative.
        final Matcher m = Pattern.compile("(?:src|href)=\"(https?://[^\"]+)\"").matcher(body);
        Assertions.assertFalse(m.find(), "no absolute remote resource may be referenced, found: "
                                         + (m.reset().find() ? m.group(1) : ""));

        // And the stylesheet actually serves.
        final HttpRequest cssReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + "/shared/verla-tailwind.css"))
            .GET()
            .build();
        final HttpResponse<String> cssResp = client.send(cssReq, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, cssResp.statusCode());
        Assertions.assertTrue(cssResp.body().length() > 10_000, "built stylesheet looks truncated");
        Assertions.assertTrue(cssResp.body().contains("--color-terracotta-500"), "theme tokens must be present");
    }

    /**
     * Every page - including the fragments rendered from Java - must be free of inline style
     * attributes and of the semantic class names used by the other VÉRLA variants.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testMarkupIsUtilityFirstOnEveryPage() throws IOException, InterruptedException
    {
        // Fill the cart first so the cart/checkout pages render their populated variants.
        get("api/cart/add?productId=SKU-TOP-1000:M&quantity=2");

        for (final String page : List.of("index.html", "c/tops.html", "plp.html", "cart.html",
                                         "checkout.html", "login.html", "register.html",
                                         "track-orders.html", "about.html", "faq.html",
                                         "p/premium-off-white-shirts-0.html"))
        {
            final String body = get(page);

            Assertions.assertFalse(body.contains("style=\""), page + " must not contain inline style attributes");

            final Matcher m = CLASS_ATTR.matcher(body);
            while (m.find())
            {
                for (final String token : m.group(1).trim().split("\\s+"))
                {
                    Assertions.assertFalse(FORBIDDEN_CLASSES.contains(token),
                                           page + " still uses the semantic class '" + token + "'");
                }
            }
        }
    }

    /**
     * Component state must be carried by data attributes so that the styling stays in the
     * utility class list (data-[state=...]:, data-[open=true]:, ...).
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testStateIsExpressedAsDataAttributes() throws IOException, InterruptedException
    {
        final String home = get("index.html");

        Assertions.assertTrue(home.contains("data-open=\"false\""), "country modal needs a data-open state");
        Assertions.assertTrue(home.contains("data-[open=true]:visible"), "modal open state must be a utility variant");
        Assertions.assertTrue(home.contains("data-quick-add"), "quick-add button needs its data hook");
        Assertions.assertTrue(home.contains("data-[state=added]:bg-success"), "added state must be a utility variant");
        Assertions.assertTrue(home.contains("data-cart-dropdown"), "mini cart needs its data hook");
        Assertions.assertTrue(home.contains("group-hover:translate-y-0"), "quick-add reveal must use group-hover");

        final String account = get("login.html");
        Assertions.assertTrue(account.contains("focus:border-terracotta-500"), "focus styling must be a utility variant");
    }

    /**
     * The storefront still has to work: product listing, cart, coupon and checkout.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testStorefrontFlowStillFunctions() throws IOException, InterruptedException
    {
        final String plp = get("c/tops.html");
        Assertions.assertTrue(plp.contains("id=\"plp-product-grid\""));
        Assertions.assertTrue(plp.contains("id=\"refinement-form\""));
        Assertions.assertTrue(plp.contains("id=\"sort-select\""));
        Assertions.assertTrue(plp.contains("<article"), "product cards must render");

        get("api/cart/add?productId=SKU-TOP-1000:M&quantity=2");

        final String cart = get("cart.html");
        Assertions.assertTrue(cart.contains("id=\"cart-content-wrapper\""));
        Assertions.assertTrue(cart.contains("id=\"checkout-btn\""));
        Assertions.assertTrue(cart.contains("id=\"couponCode\""));

        final String checkout = get("checkout.html");
        Assertions.assertTrue(checkout.contains("id=\"checkout-form-container\""));
        Assertions.assertTrue(checkout.contains("id=\"purchase-btn\""));
        Assertions.assertTrue(checkout.contains("id=\"shipping-address-fields\""));
        Assertions.assertTrue(checkout.contains("id=\"cardNumber\""));
    }

    /**
     * The layout chrome - the part that the older Tailwind variant left untouched - must be
     * utility driven here as well.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if thread is interrupted
     */
    @NeodymiumTest
    public final void testLayoutChromeIsConverted() throws IOException, InterruptedException
    {
        final String body = get("index.html");

        // Header, footer and modal are all utilities now.
        Assertions.assertTrue(body.contains("sticky top-0 z-[100]"), "sticky header via utilities");
        Assertions.assertTrue(body.contains("mx-auto w-full max-w-7xl px-6"), "explicit container utilities");
        Assertions.assertFalse(body.contains("class=\"container\""), "must not rely on Tailwind's container utility");
        Assertions.assertTrue(body.contains("empty:hidden"), "search dropdown uses the empty: variant");

        // Navigation underline is an after: variant rather than a hand-written ::after rule.
        Assertions.assertTrue(body.contains("after:content-['']"), "nav underline must use the after: variant");

        // The page must carry no embedded stylesheet at all.
        Assertions.assertFalse(body.contains("<style"), "layout must not embed a <style> block");
    }
}

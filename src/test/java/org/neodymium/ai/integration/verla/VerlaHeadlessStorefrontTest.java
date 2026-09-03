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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.junit5.NeodymiumTest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Verifies the runtime quality profile of the VÉRLA headless storefront variant
 * (<code>/verla-headless/</code>).
 * <p>
 * The variant models a composable commerce SPA: the server ships a shell plus a large hydration
 * blob, and the browser assembles the page from per-entity API calls. These tests pin the traps
 * that distinguish it from the server-rendered variants - product markup that carries no product
 * data, a session handshake that rejects work until it settles, and content slots that are absent
 * from the delivered document.
 *
 * @author Xceptance GmbH 2026
 */
@Tag("integration")
@Tag("verla")
public class VerlaHeadlessStorefrontTest extends BaseAiTest
{
    private static final Pattern TILE_PATTERN = Pattern.compile("<article[^>]*data-testid=\"product-tile\"");

    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("\"customer_id\"\\s*:\\s*\"([^\"]+)\"");

    private static final Pattern SESSION_CUSTOMER_PATTERN = Pattern.compile("\"customerId\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient client = HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .build();

    /**
     * Constructs a default VerlaHeadlessStorefrontTest instance.
     */
    public VerlaHeadlessStorefrontTest()
    {
    }

    private String baseUrl()
    {
        return "http://localhost:" + server.getPort() + "/verla-headless/";
    }

    private HttpResponse<String> get(final String path) throws IOException, InterruptedException
    {
        return this.client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl() + path)).GET().build(),
                                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(final String path) throws IOException, InterruptedException
    {
        return this.client.send(HttpRequest.newBuilder()
                                           .uri(URI.create(baseUrl() + path))
                                           .POST(HttpRequest.BodyPublishers.noBody())
                                           .build(),
                                HttpResponse.BodyHandlers.ofString());
    }

    private static String firstGroup(final Pattern pattern, final String body)
    {
        final Matcher m = pattern.matcher(body);
        Assertions.assertTrue(m.find(), "Expected pattern " + pattern.pattern() + " in response");
        return m.group(1);
    }

    /**
     * The delivered document must carry the shell, the hydration blob and the inlined sprite, but
     * no rendered product data: tiles are skeletons that name only the product id.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if the thread is interrupted
     */
    @NeodymiumTest
    public final void testShellShipsSkeletonsRatherThanProductData() throws IOException, InterruptedException
    {
        final HttpResponse<String> resp = get("index.html");
        Assertions.assertEquals(200, resp.statusCode());
        final String body = resp.body();

        // The shell announces that it is not ready yet.
        Assertions.assertTrue(body.contains("data-session-state=\"BOOTSTRAPPING\""),
                              "Shell must start in the BOOTSTRAPPING state");
        Assertions.assertTrue(body.contains("data-hydrated=\"false\""), "Shell must start unhydrated");

        // Tiles are present but empty.
        final Matcher tiles = TILE_PATTERN.matcher(body);
        int tileCount = 0;
        while (tiles.find())
        {
            tileCount++;
        }
        Assertions.assertTrue(tileCount > 0, "Homepage must render product tile skeletons");
        Assertions.assertTrue(body.contains("data-state=\"pending\""), "Tiles must start pending");
        Assertions.assertTrue(body.contains("<span class=\"css-"), "Tiles must use CSS-in-JS style hashes");

        // The hydration blob is present and holds the data the tiles do not show.
        Assertions.assertTrue(body.contains("id=\"__PRELOADED_STATE__\""), "Hydration blob must be inlined");
        Assertions.assertTrue(body.contains("__INITIAL_CORRELATION_ID__"), "Hydration blob must carry app state");

        // Icon sprite is inlined, so icon-only controls have no text node.
        Assertions.assertTrue(body.contains("__SVG_SPRITE_NODE__"), "Icon sprite must be inlined");

        // Content slots are declared but empty in the delivered document.
        Assertions.assertTrue(body.contains("data-cms-slot=\"PWA_StripBanner\""), "Strip banner must be a deferred slot");
        Assertions.assertTrue(body.contains("data-cms-slot=\"PWA_FooterLinkList\""), "Footer links must be a deferred slot");

        // The shell carries a client router, so navigation never replaces the document.
        Assertions.assertTrue(body.contains("pushState"), "Shell must carry a client-side router");
        Assertions.assertTrue(body.contains("popstate"), "Router must handle browser history navigation");
        Assertions.assertTrue(body.contains("X-PWA-Router"), "Router must request route bodies as fragments");
        Assertions.assertTrue(body.contains("data-route-state=\"ready\""), "Shell must expose a route readiness signal");
    }

    /**
     * A route requested by the client router must come back as a bare body fragment. If the shell
     * came back with it, every navigation would rebuild the header, footer and preloaded state -
     * which is what separates this variant from the server-rendered ones.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if the thread is interrupted
     */
    @NeodymiumTest
    public final void testRouterReceivesFragmentsRatherThanDocuments() throws IOException, InterruptedException
    {
        for (final String route : new String[] { "c/tops.html", "cart.html", "checkout.html", "faq.html" })
        {
            final HttpResponse<String> fragment = this.client.send(
                HttpRequest.newBuilder()
                           .uri(URI.create(baseUrl() + route))
                           .header("X-PWA-Router", "true")
                           .header("HX-Request", "true")
                           .GET()
                           .build(),
                HttpResponse.BodyHandlers.ofString());

            Assertions.assertEquals(200, fragment.statusCode(), route + " must resolve for the router");
            final String body = fragment.body();

            Assertions.assertFalse(body.contains("<html"), route + " fragment must not carry a document shell");
            Assertions.assertFalse(body.contains("__PRELOADED_STATE__"),
                                   route + " fragment must not repeat the hydration blob");
            Assertions.assertFalse(body.contains("__SVG_SPRITE_NODE__"),
                                   route + " fragment must not repeat the icon sprite");

            // The same route requested as a document must include the shell.
            final HttpResponse<String> document = get(route);
            Assertions.assertTrue(document.body().contains("<html"), route + " must still deep-link as a document");
            Assertions.assertTrue(document.body().length() > body.length(),
                                  route + " document must be larger than its fragment");
        }
    }

    /**
     * The router resolves each route through the API before rendering it, so route classification
     * must be served rather than inferred from the URL by the client.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if the thread is interrupted
     */
    @NeodymiumTest
    public final void testRouteResolutionEndpoint() throws IOException, InterruptedException
    {
        final String[][] cases = {
            { "c/tops.html", "category" },
            { "p/modern-sage-sweaters-2.html", "product" },
            { "cart.html", "cart" },
            { "checkout.html", "checkout" },
            { "index.html", "home" },
            { "faq.html", "content" }
        };

        for (final String[] testCase : cases)
        {
            final HttpResponse<String> resp = get("api/scapi/route?path=" + testCase[0]);
            Assertions.assertEquals(200, resp.statusCode(), "Route " + testCase[0] + " must resolve");
            final JsonObject route = JsonParser.parseString(resp.body()).getAsJsonObject();
            Assertions.assertEquals(testCase[1], route.get("type").getAsString(),
                                    "Route " + testCase[0] + " must classify as " + testCase[1]);
        }
    }

    /**
     * A product document must be retrievable per tile, which is the N+1 fan-out the variant exists
     * to reproduce.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if the thread is interrupted
     */
    @NeodymiumTest
    public final void testPerTileProductDocumentFanOut() throws IOException, InterruptedException
    {
        final HttpResponse<String> doc = get("api/scapi/product?id=SKU-TOP-1000");
        Assertions.assertEquals(200, doc.statusCode());

        final JsonObject parsed = JsonParser.parseString(doc.body()).getAsJsonObject();
        Assertions.assertEquals("SKU-TOP-1000", parsed.get("id").getAsString(), "Document must identify the product");
        Assertions.assertTrue(parsed.has("imageGroups"), "Document must publish an image rendition");

        // Only the large rendition exists, so every tile pulls it regardless of display size.
        final String link = parsed.getAsJsonArray("imageGroups").get(0).getAsJsonObject()
                                  .getAsJsonArray("images").get(0).getAsJsonObject()
                                  .get("link").getAsString();
        Assertions.assertTrue(link.contains("sw=960"), "Only the 960px rendition is published, was: " + link);

        // That rendition must actually be servable and must carry real weight.
        final HttpResponse<String> rendition = get(link);
        Assertions.assertEquals(200, rendition.statusCode(), "Published rendition must resolve");
        Assertions.assertTrue(rendition.body().length() > 4096,
                              "An oversized rendition must cost something, was " + rendition.body().length() + " bytes");

        final HttpResponse<String> missing = get("api/scapi/product?id=SKU-DOES-NOT-EXIST");
        Assertions.assertEquals(404, missing.statusCode(), "Unknown products must 404 rather than render empty");
    }

    /**
     * The bootstrap handshake must hand out a customer id that the BFF endpoints reject, and only
     * accept work once the session reports itself settled.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if the thread is interrupted
     */
    @NeodymiumTest
    public final void testSessionRaceRejectsWorkUntilSettled() throws IOException, InterruptedException
    {
        get("index.html");

        final HttpResponse<String> token = post("api/scapi/token");
        Assertions.assertEquals(200, token.statusCode());
        final String bootstrapId = firstGroup(CUSTOMER_ID_PATTERN, token.body());

        // Before the handshake settles the id on hand is a bootstrap id the BFF will not accept.
        final HttpResponse<String> earlyBasket = get("api/bff/basket?customerId=" + bootstrapId);
        Assertions.assertEquals(400, earlyBasket.statusCode(), "Basket must reject the bootstrap customer id");
        Assertions.assertTrue(earlyBasket.body().contains("invalid-customer"), "Rejection must name the cause");

        // Wait on the readiness signal rather than sleeping a fixed amount.
        String settledId = null;
        for (int attempt = 0; attempt < 100; attempt++)
        {
            final HttpResponse<String> status = get("api/scapi/session");
            Assertions.assertEquals(200, status.statusCode());
            if (status.body().contains("\"ready\":true"))
            {
                settledId = firstGroup(SESSION_CUSTOMER_PATTERN, status.body());
                break;
            }
            Thread.sleep(50L);
        }
        Assertions.assertNotNull(settledId, "Session must eventually settle");
        Assertions.assertNotEquals(bootstrapId, settledId, "The settled id must differ from the bootstrap id");

        final HttpResponse<String> settledBasket = get("api/bff/basket?customerId=" + settledId);
        Assertions.assertEquals(200, settledBasket.statusCode(), "Basket must accept the settled customer id");
    }

    /**
     * Quick add on the grid is driven by the product document, not by the markup. The variant list
     * must carry per-size availability so the client can build the size selector, and out-of-stock
     * sizes must be distinguishable.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if the thread is interrupted
     */
    @NeodymiumTest
    public final void testQuickAddVariantsComeFromTheProductDocument() throws IOException, InterruptedException
    {
        // A sized product publishes its size ladder with per-size stock.
        final JsonObject top = JsonParser.parseString(get("api/scapi/product?id=SKU-TOP-1000").body()).getAsJsonObject();
        Assertions.assertTrue(top.get("requiresSize").getAsBoolean(), "Apparel must require a size choice");

        final JsonArray variants = top.getAsJsonArray("variants");
        Assertions.assertFalse(variants.isEmpty(), "Apparel must publish size variants");

        boolean sawOrderable = false;
        boolean sawOutOfStock = false;
        for (int i = 0; i < variants.size(); i++)
        {
            final JsonObject variant = variants.get(i).getAsJsonObject();
            Assertions.assertTrue(variant.has("size"), "Variant must name its size");
            Assertions.assertTrue(variant.has("stock"), "Variant must publish its stock level");
            Assertions.assertEquals(variant.get("stock").getAsInt() > 0, variant.get("orderable").getAsBoolean(),
                                    "Orderability must follow the stock level");
            sawOrderable |= variant.get("orderable").getAsBoolean();
            sawOutOfStock |= !variant.get("orderable").getAsBoolean();
        }
        Assertions.assertTrue(sawOrderable, "Seeded catalog must expose at least one orderable size");
        Assertions.assertTrue(sawOutOfStock, "Seeded catalog must expose at least one exhausted size");

        // Accessories skip size selection entirely, as they do in the server-rendered variants.
        final JsonObject accessory = JsonParser.parseString(get("api/scapi/product?id=SKU-ACC-1280").body()).getAsJsonObject();
        Assertions.assertFalse(accessory.get("requiresSize").getAsBoolean(), "Accessories must not require a size");
        Assertions.assertTrue(accessory.getAsJsonArray("variants").isEmpty(), "Accessories must publish no size variants");
    }

    /**
     * The quick add control must be absent from the delivered grid markup. It is assembled from
     * each tile's product document, so it cannot exist before that request returns - which is the
     * behaviour that separates this variant from the server-rendered grids.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if the thread is interrupted
     */
    @NeodymiumTest
    public final void testQuickAddIsAbsentFromDeliveredMarkup() throws IOException, InterruptedException
    {
        final String headless = get("c/tops.html").body();
        Assertions.assertTrue(headless.contains("data-testid=\"product-tile\""), "Grid must ship tiles");
        Assertions.assertFalse(headless.contains("class=\"product-quick-add"),
                               "Quick add must not be server-rendered into the headless grid");
        Assertions.assertFalse(headless.contains("data-stock="),
                               "Stock must travel in the product document, not in tile markup");

        // The server-rendered baseline does the opposite, which is the contrast being modelled.
        final HttpResponse<String> perfect = this.client.send(
            HttpRequest.newBuilder()
                       .uri(URI.create("http://localhost:" + server.getPort() + "/verla-perfect/c/tops.html"))
                       .GET()
                       .build(),
            HttpResponse.BodyHandlers.ofString());
        Assertions.assertTrue(perfect.body().contains("product-quick-add"),
                              "Baseline variant must server-render its quick add control");
    }

    /**
     * Content slots must be served as separate documents, which is what makes the footer and banner
     * arrive after first paint.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if the thread is interrupted
     */
    @NeodymiumTest
    public final void testContentSlotsAreSeparateDocuments() throws IOException, InterruptedException
    {
        for (final String slot : new String[] { "PWA_StripBanner", "PWA_FooterLinkList", "PWA_FooterLegal" })
        {
            final HttpResponse<String> resp = get("api/cms/slot?id=" + slot);
            Assertions.assertEquals(200, resp.statusCode(), "Slot " + slot + " must resolve");
            Assertions.assertTrue(resp.body().contains("\"html\""), "Slot " + slot + " must return markup");
            Assertions.assertTrue(resp.body().length() > 20, "Slot " + slot + " must not be empty");
        }
    }

    /**
     * Category pages must fan out too, and must not leak server-rendered product markup into the
     * skeleton grid.
     *
     * @throws IOException if network fails
     * @throws InterruptedException if the thread is interrupted
     */
    @NeodymiumTest
    public final void testCategoryPageRendersSkeletonGrid() throws IOException, InterruptedException
    {
        final HttpResponse<String> resp = get("c/tops.html");
        Assertions.assertEquals(200, resp.statusCode());
        final String body = resp.body();

        final Matcher tiles = TILE_PATTERN.matcher(body);
        int tileCount = 0;
        while (tiles.find())
        {
            tileCount++;
        }
        Assertions.assertTrue(tileCount > 0, "Category page must render skeleton tiles");

        // A skeleton grid must not also contain server-rendered cards for the same products.
        Assertions.assertFalse(body.contains("class=\"product-card\""),
                               "Client-hydrated tiles must not be mixed with server-rendered cards");
    }
}

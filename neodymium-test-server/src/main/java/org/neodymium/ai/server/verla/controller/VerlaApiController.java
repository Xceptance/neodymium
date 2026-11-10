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
package org.neodymium.ai.server.verla.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.neodymium.ai.server.session.SessionManager;
import org.neodymium.ai.server.session.TestSession;
import org.neodymium.ai.server.verla.catalog.VerlaCatalog;
import org.neodymium.ai.server.verla.model.Address;
import org.neodymium.ai.server.verla.model.Card;
import org.neodymium.ai.server.verla.model.Cart;
import org.neodymium.ai.server.verla.model.Country;
import org.neodymium.ai.server.verla.model.Order;
import org.neodymium.ai.server.verla.model.OrderItem;
import org.neodymium.ai.server.verla.model.Product;
import org.neodymium.ai.server.verla.model.User;
import org.neodymium.ai.server.verla.renderer.VerlaRenderer;
import org.neodymium.ai.util.VerlaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.HttpStatus;

/**
 * Handles all VÉRLA dynamic REST and HTML snippet API endpoints.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class VerlaApiController
{
    private static final Logger LOG = LoggerFactory.getLogger(VerlaApiController.class);

    private static final Gson GSON = new Gson();

    private final SessionManager sessionManager;
    private final VerlaCatalog catalog;
    private final VerlaRenderer renderer;

    public VerlaApiController(final SessionManager sessionManager, final VerlaCatalog catalog, final VerlaRenderer renderer)
    {
        this.sessionManager = sessionManager != null ? sessionManager : new SessionManager();
        this.catalog = catalog != null ? catalog : VerlaCatalog.getInstance();
        this.renderer = renderer != null ? renderer : new VerlaRenderer(this.catalog);
    }

    /**
     * Dispatches an incoming API request to the appropriate handler.
     *
     * @param ctx the Javalin context
     * @param qualitySuffix the SUT quality flavor
     * @param apiMethod the API endpoint method relative to /api/
     */
    public void handle(final Context ctx, final String qualitySuffix, final String apiMethod)
    {
        final TestSession session = resolveSession(ctx);
        final String cartId = resolveCartId(ctx);
        final Cart cart = session.getActiveCarts().computeIfAbsent(cartId, Cart::new);
        final User currentUser = resolveUser(ctx, session);
        final Country activeCountry = resolveCountry(ctx);
        final String locale = activeCountry.locale;
        final Map<String, String> trans = catalog.getTranslations(locale);
        final Map<String, String> params = extractParams(ctx);

        final boolean isPwaRouter = "true".equalsIgnoreCase(ctx.header("X-PWA-Router"));
        final String hxTarget = ctx.header("HX-Target");
        final boolean isHtmx = "true".equalsIgnoreCase(ctx.header("HX-Request")) || isPwaRouter;

        if (LOG.isDebugEnabled())
        {
            LOG.debug("VerlaApiController: method='{}', quality='{}', apiMethod='{}', cartId='{}'",
                ctx.method().name(), qualitySuffix, apiMethod, cartId);
        }

        if ("snippets/products".equals(apiMethod))
        {
            handleSnippetsProducts(ctx, qualitySuffix, activeCountry, trans, session);
        }
        else if ("scapi/token".equals(apiMethod))
        {
            handleScapiToken(ctx, cartId, session);
        }
        else if ("scapi/session".equals(apiMethod))
        {
            handleScapiSession(ctx, cartId, session);
        }
        else if ("scapi/product".equals(apiMethod))
        {
            handleScapiProduct(ctx, activeCountry, session);
        }
        else if ("scapi/route".equals(apiMethod))
        {
            handleScapiRoute(ctx, activeCountry);
        }
        else if ("scapi/image".equals(apiMethod))
        {
            handleScapiImage(ctx);
        }
        else if (apiMethod.startsWith("bff/"))
        {
            handleBff(ctx, apiMethod, cartId, cart, session);
        }
        else if ("cms/slot".equals(apiMethod))
        {
            handleCmsSlot(ctx, trans);
        }
        else if ("country/select".equals(apiMethod))
        {
            handleCountrySelect(ctx, params);
        }
        else if ("countries".equals(apiMethod))
        {
            handleCountries(ctx, qualitySuffix, activeCountry);
        }
        else if ("address-form".equals(apiMethod))
        {
            handleAddressForm(ctx, qualitySuffix, activeCountry, locale, trans, params);
        }
        else if ("cart/add".equals(apiMethod))
        {
            handleCartAdd(ctx, qualitySuffix, activeCountry, trans, cartId, cart, params, session);
        }
        else if ("cart/update".equals(apiMethod))
        {
            handleCartUpdate(ctx, qualitySuffix, activeCountry, trans, cart, params);
        }
        else if ("cart/drawer".equals(apiMethod))
        {
            handleCartDrawer(ctx, qualitySuffix, activeCountry, trans, cart);
        }
        else if ("cart/badge".equals(apiMethod))
        {
            ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML)
               .result(renderer.getCartBadgeWrapperHtml(cart, trans, activeCountry, qualitySuffix));
        }
        else if ("cart/remove".equals(apiMethod))
        {
            handleCartRemove(ctx, qualitySuffix, activeCountry, trans, cart, hxTarget, params);
        }
        else if ("cart/coupon".equals(apiMethod))
        {
            handleCartCoupon(ctx, qualitySuffix, activeCountry, trans, cart, params);
        }
        else if ("cart/dropdown".equals(apiMethod))
        {
            VerlaConfiguration.getInstance().simulateCartDropdown();
            ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML)
               .result(renderer.getCartDropdownHtml(cart, trans, activeCountry, qualitySuffix));
        }
        else if ("search/suggest".equals(apiMethod))
        {
            handleSearchSuggest(ctx, qualitySuffix, activeCountry, locale, params);
        }
        else if ("auth/login".equals(apiMethod))
        {
            handleAuthLogin(ctx, qualitySuffix, activeCountry, trans, currentUser, cart, isHtmx, params, session);
        }
        else if ("auth/register".equals(apiMethod))
        {
            handleAuthRegister(ctx, qualitySuffix, activeCountry, trans, currentUser, cart, isHtmx, params, session);
        }
        else if ("auth/change-password".equals(apiMethod))
        {
            handleAuthChangePassword(ctx, qualitySuffix, activeCountry, trans, currentUser, cart, isHtmx, params, session);
        }
        else if ("auth/address/add".equals(apiMethod))
        {
            handleAuthAddressAdd(ctx, qualitySuffix, activeCountry, trans, currentUser, cart, isHtmx, params, session);
        }
        else if ("auth/address/delete".equals(apiMethod))
        {
            handleAuthAddressDelete(ctx, qualitySuffix, activeCountry, trans, currentUser, cart, isHtmx, params, session);
        }
        else if ("auth/card/add".equals(apiMethod))
        {
            handleAuthCardAdd(ctx, qualitySuffix, activeCountry, trans, currentUser, cart, isHtmx, params, session);
        }
        else if ("auth/card/delete".equals(apiMethod))
        {
            handleAuthCardDelete(ctx, qualitySuffix, activeCountry, trans, currentUser, cart, isHtmx, params, session);
        }
        else if ("checkout/purchase".equals(apiMethod))
        {
            handleCheckoutPurchase(ctx, qualitySuffix, activeCountry, locale, trans, currentUser, cart, isHtmx, params, session);
        }
        else if ("order/lookup".equals(apiMethod))
        {
            handleOrderLookup(ctx, activeCountry, trans, params, session);
        }
        else if ("auth/logout".equals(apiMethod))
        {
            handleAuthLogout(ctx, qualitySuffix, session);
        }
        else
        {
            ctx.status(HttpStatus.NOT_FOUND).contentType(ContentType.TEXT_PLAIN).result("API endpoint not found: " + apiMethod);
        }
    }

    private void handleSnippetsProducts(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                        final Map<String, String> trans, final TestSession session)
    {
        VerlaConfiguration.getInstance().simulateProductFilter();
        final String cat = queryParamOrDefault(ctx, "category", "");
        final String sort = queryParamOrDefault(ctx, "sort", "");
        final String bopis = queryParamOrDefault(ctx, "bopis", "");
        final List<String> activeColors = ctx.queryParams("color");
        final List<String> activePrices = ctx.queryParams("price");
        final List<String> activeSales = ctx.queryParams("sale");

        final List<Product> filtered = catalog.getProducts().stream().filter(p ->
        {
            if (!cat.isEmpty() && !"all".equalsIgnoreCase(cat) && !p.category.equalsIgnoreCase(cat))
            {
                return false;
            }
            if ("true".equalsIgnoreCase(bopis)
                && session.getProductInventory().getOrDefault(p.id, Map.of()).values().stream().mapToInt(Integer::intValue).sum() <= 0)
            {
                return false;
            }

            if (!activeColors.isEmpty())
            {
                boolean colorMatch = false;
                for (final String col : activeColors)
                {
                    if (p.color != null && p.color.equalsIgnoreCase(col))
                    {
                        colorMatch = true;
                        break;
                    }
                }
                if (!colorMatch)
                {
                    return false;
                }
            }

            if (!activePrices.isEmpty())
            {
                final double price = p.salePrice != null ? p.salePrice : p.basePrice;
                boolean priceMatch = false;
                for (final String pr : activePrices)
                {
                    if ("0-50".equals(pr) && price < 50.0)
                    {
                        priceMatch = true;
                    }
                    else if ("50-100".equals(pr) && price >= 50.0 && price <= 100.0)
                    {
                        priceMatch = true;
                    }
                    else if ("100-200".equals(pr) && price > 100.0)
                    {
                        priceMatch = true;
                    }
                }
                if (!priceMatch)
                {
                    return false;
                }
            }

            if (!activeSales.isEmpty() && activeSales.contains("true"))
            {
                if (p.salePrice == null)
                {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());

        if ("price-asc".equals(sort))
        {
            filtered.sort((p1, p2) -> Double.compare(p1.salePrice != null ? p1.salePrice : p1.basePrice,
                p2.salePrice != null ? p2.salePrice : p2.basePrice));
        }
        else if ("price-desc".equals(sort))
        {
            filtered.sort((p1, p2) -> Double.compare(p2.salePrice != null ? p2.salePrice : p2.basePrice,
                p1.salePrice != null ? p1.salePrice : p1.basePrice));
        }

        final VerlaConfiguration snippetCfg = VerlaConfiguration.getInstance();
        VerlaRenderer.headlessTileBudget.get()[0] = (VerlaRenderer.isHeadless(qualitySuffix) && snippetCfg.isHeadlessEnabled())
                                      ? snippetCfg.getHeadlessTileFanout()
                                      : 0;
        VerlaRenderer.headlessRenderedTiles.get().clear();

        final StringBuilder sb = new StringBuilder();
        for (final Product p : filtered)
        {
            sb.append(renderer.renderProductCard(p, activeCountry, trans, qualitySuffix, session));
        }
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result(sb.toString());
    }

    private void handleScapiToken(final Context ctx, final String cartId, final TestSession session)
    {
        VerlaConfiguration.getInstance().simulateHeadlessToken();
        session.ensureHeadlessSession(cartId);
        final Map<String, Object> tok = new LinkedHashMap<>();
        tok.put("access_token", UUID.randomUUID().toString().replace("-", ""));
        tok.put("token_type", "BEARER");
        tok.put("expires_in", 1800);
        tok.put("usid", UUID.randomUUID().toString());
        tok.put("customer_id", session.headlessCustomerId(cartId));
        tok.put("enc_user_id", Long.toHexString(cartId.hashCode() & 0xFFFFFFFFL));
        ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(GSON.toJson(tok));
    }

    private void handleScapiSession(final Context ctx, final String cartId, final TestSession session)
    {
        final boolean settled = session.isHeadlessSessionSettled(cartId);
        final Map<String, Object> st = new LinkedHashMap<>();
        st.put("ready", settled);
        st.put("customerId", session.headlessCustomerId(cartId));
        st.put("state", settled ? "SETTLED" : "BOOTSTRAPPING");
        ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(GSON.toJson(st));
    }

    private void handleScapiProduct(final Context ctx, final Country activeCountry, final TestSession session)
    {
        VerlaConfiguration.getInstance().simulateHeadlessTileFetch();
        final String pid = ctx.queryParam("id");
        final Product found = catalog.lookupProductById(pid);
        if (found == null)
        {
            ctx.status(HttpStatus.NOT_FOUND).contentType(ContentType.APPLICATION_JSON)
               .result("{\"title\":\"Not Found\",\"type\":\"product-not-found\",\"detail\":\"No product with id "
                   + VerlaRenderer.escapeHtml(pid != null ? pid : "") + "\"}");
            return;
        }
        ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON)
           .result(GSON.toJson(renderer.headlessProductDocument(found, activeCountry, session)));
    }

    private void handleScapiRoute(final Context ctx, final Country activeCountry)
    {
        VerlaConfiguration.getInstance().simulateHeadlessCmsSlot();
        final String path = queryParamOrDefault(ctx, "path", "");
        final String stripped = path.contains("?") ? path.substring(0, path.indexOf('?')) : path;
        final String cleaned = stripped.startsWith("/") ? stripped : "/" + stripped;
        final String leaf = cleaned.substring(cleaned.lastIndexOf('/') + 1);

        final String routeType;
        if (cleaned.contains("/c/"))
        {
            routeType = "category";
        }
        else if (cleaned.contains("/p/"))
        {
            routeType = "product";
        }
        else if (leaf.startsWith("cart"))
        {
            routeType = "cart";
        }
        else if (leaf.startsWith("checkout"))
        {
            routeType = "checkout";
        }
        else if (leaf.startsWith("index") || leaf.isEmpty())
        {
            routeType = "home";
        }
        else
        {
            routeType = "content";
        }

        final Map<String, Object> route = new LinkedHashMap<>();
        route.put("path", cleaned);
        route.put("type", routeType);
        route.put("identifier", leaf.replace(".html", ""));
        route.put("locale", activeCountry.locale);
        route.put("ssr", false);
        ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(GSON.toJson(route));
    }

    private void handleScapiImage(final Context ctx)
    {
        VerlaConfiguration.getInstance().simulateHeadlessTileFetch();
        final String pid = ctx.queryParam("id");
        final Product img = catalog.lookupProductById(pid);
        if (img == null)
        {
            ctx.status(HttpStatus.NOT_FOUND).contentType(ContentType.TEXT_PLAIN).result("Not Found");
            return;
        }
        int sw;
        try
        {
            sw = Integer.parseInt(queryParamOrDefault(ctx, "sw", "960"));
        }
        catch (final NumberFormatException ignored)
        {
            sw = 960;
        }
        sw = Math.max(64, Math.min(2048, sw));

        final StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\" width=\"").append(sw)
           .append("\" height=\"").append(Math.round(sw * 4 / 3.0)).append("\">")
           .append("<rect width=\"100\" height=\"100\" fill=\"").append(VerlaCatalog.getColorHex(img.color)).append("\" opacity=\"0.18\"/>")
           .append("<g fill=\"").append(VerlaCatalog.getColorHex(img.color)).append("\">").append(img.svgPath).append("</g>");

        final int renditionBytes = Math.round(VerlaConfiguration.getInstance().getHeadlessImageKb() * 1024f * (sw / 960f));
        final StringBuilder meta = new StringBuilder();
        final Random rnd = new Random(img.id.hashCode());
        while (meta.length() < renditionBytes)
        {
            meta.append(Integer.toHexString(rnd.nextInt(16)));
        }
        svg.append("<metadata>").append(meta).append("</metadata></svg>");

        ctx.header("Cache-Control", "public, max-age=60");
        ctx.status(HttpStatus.OK).contentType("image/svg+xml; charset=utf-8").result(svg.toString());
    }

    private void handleBff(final Context ctx, final String apiMethod, final String cartId, final Cart cart, final TestSession session)
    {
        final String claimed = ctx.queryParam("customerId");
        session.ensureHeadlessSession(cartId);
        final boolean settled = session.isHeadlessSessionSettled(cartId);
        final String valid = session.getHeadlessSettledCustomerId(cartId);
        if (!settled || claimed == null || claimed.isEmpty() || !claimed.equals(valid))
        {
            ctx.status(HttpStatus.BAD_REQUEST).contentType(ContentType.APPLICATION_JSON)
               .result("{\"title\":\"Bad Request\",\"type\":\"invalid-customer\",\"detail\":\"Customer id is not valid for this session\"}");
            return;
        }

        final String resource = apiMethod.substring(4);
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerId", valid);
        if (resource.startsWith("basket"))
        {
            int units = 0;
            for (final Integer q : cart.items.values())
            {
                units += q;
            }
            payload.put("basketId", cartId);
            payload.put("productItems", cart.items.size());
            payload.put("totalQuantity", units);
        }
        else
        {
            payload.put("wishlistId", "wl-" + Long.toHexString(cartId.hashCode() & 0xFFFFFFL));
            payload.put("items", List.of());
        }
        ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(GSON.toJson(payload));
    }

    private void handleCmsSlot(final Context ctx, final Map<String, String> trans)
    {
        VerlaConfiguration.getInstance().simulateHeadlessCmsSlot();
        final String slotId = queryParamOrDefault(ctx, "id", "");
        final Map<String, Object> slot = new LinkedHashMap<>();
        slot.put("id", slotId);
        slot.put("type", "content-asset");
        switch (slotId)
        {
            case "PWA_StripBanner":
                slot.put("html", "<span class=\"" + VerlaRenderer.emotionHash("strip-banner") + "\">"
                                 + VerlaRenderer.escapeHtml(trans.getOrDefault("promoBanner", "Free worldwide shipping on orders over $150 USD")) + "</span>");
                break;
            case "PWA_FooterLinkList":
                slot.put("html", "<ul class=\"" + VerlaRenderer.emotionHash("footer-links") + "\">"
                                 + "<li><a href=\"shipping.html\">" + VerlaRenderer.escapeHtml(trans.getOrDefault("shippingReturns", "Shipping &amp; Returns")) + "</a></li>"
                                 + "<li><a href=\"faq.html\">FAQ</a></li>"
                                 + "<li><a href=\"contact.html\">" + VerlaRenderer.escapeHtml(trans.getOrDefault("contact", "Contact")) + "</a></li>"
                                 + "<li><a href=\"stores.html\">" + VerlaRenderer.escapeHtml(trans.getOrDefault("ourStores", "Our Stores")) + "</a></li>"
                                 + "<li><a href=\"careers.html\">" + VerlaRenderer.escapeHtml(trans.getOrDefault("careers", "Careers")) + "</a></li>"
                                 + "</ul>");
                break;
            case "PWA_FooterLegal":
                slot.put("html", "<p class=\"" + VerlaRenderer.emotionHash("footer-legal") + "\">&copy; 2026 V&Eacute;RLA. "
                                 + VerlaRenderer.escapeHtml(trans.getOrDefault("allRightsReserved", "All rights reserved.")) + "</p>");
                break;
            default:
                slot.put("html", "");
                break;
        }
        ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(GSON.toJson(slot));
    }

    private void handleCountrySelect(final Context ctx, final Map<String, String> params)
    {
        final String queryCode = ctx.queryParam("code");
        final String selectCode = (queryCode != null && !queryCode.isEmpty()) ? queryCode : params.getOrDefault("code", "US");
        final Cookie cookie = new Cookie("verla_country", selectCode, "/", 31536000, false);
        ctx.cookie(cookie);
        ctx.header("HX-Refresh", "true");
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result("");
    }

    private void handleCountries(final Context ctx, final String qualitySuffix, final Country activeCountry)
    {
        final String q = queryParamOrDefault(ctx, "q", "").toLowerCase();
        final StringBuilder sb = new StringBuilder();
        for (final Country c : catalog.getCountries())
        {
            if (q.isEmpty() || c.name.toLowerCase().contains(q) || c.code.toLowerCase().contains(q))
            {
                final String selectedClass = c.code.equals(activeCountry.code) ? "selected" : "";
                if (VerlaRenderer.isTailwindByClaude(qualitySuffix) || VerlaRenderer.isApparel(qualitySuffix))
                {
                    sb.append("<li data-selected=\"").append(c.code.equals(activeCountry.code)).append("\" class=\"")
                      .append(VerlaRenderer.TW_COUNTRY_ITEM).append("\" hx-get=\"api/country/select?code=").append(c.code).append("\">")
                      .append(c.name).append(" (").append(c.symbol).append(")</li>");
                }
                else if ("apocalypse".equals(qualitySuffix))
                {
                    sb.append("<li class=\"country-item ").append(selectedClass).append("\" data-country=\"").append(c.code)
                      .append("\" style=\"display:flex; align-items:center; gap:8px; cursor:pointer;\" onclick=\"selectApocalypseCountry('")
                      .append(c.code).append("');\">")
                      .append("<span style=\"font-size:16px;\">").append(VerlaCatalog.getCountryFlag(c.code)).append("</span> ")
                      .append("<span>").append(c.name).append(" (").append(c.symbol).append(")</span></li>");
                }
                else if ("bad".equals(qualitySuffix) || "modern-bad-nowcag".equals(qualitySuffix))
                {
                    sb.append("<div class=\"country-item ").append(selectedClass)
                      .append("\" style=\"padding:10px;cursor:pointer;\" onclick=\"document.cookie='verla_country=").append(c.code)
                      .append(";path=/';location.reload();\">")
                      .append(c.name).append(" (").append(c.symbol).append(")</div>");
                }
                else
                {
                    sb.append("<li class=\"country-item ").append(selectedClass).append("\" hx-get=\"api/country/select?code=").append(c.code).append("\">")
                      .append(c.name).append(" (").append(c.symbol).append(")</li>");
                }
            }
        }
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result(sb.toString());
    }

    private void handleAddressForm(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                   final String locale, final Map<String, String> trans, final Map<String, String> params)
    {
        final String country = queryParamOrDefault(ctx, "country", activeCountry.code);
        final String prefix = params.getOrDefault("prefix", "shipping-");
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML)
           .result(renderer.getAddressFieldsHtml(country, locale, trans, prefix, qualitySuffix));
    }

    private void handleCartAdd(final Context ctx, final String qualitySuffix, final Country activeCountry,
                               final Map<String, String> trans, final String cartId, final Cart cart,
                               final Map<String, String> params, final TestSession session)
    {
        VerlaConfiguration.getInstance().simulateCartAdd();

        if (VerlaRenderer.isHeadless(qualitySuffix) && !session.isHeadlessSessionSettled(cartId))
        {
            ctx.status(HttpStatus.BAD_REQUEST).contentType(ContentType.APPLICATION_JSON)
               .result("{\"title\":\"Bad Request\",\"type\":\"session-not-ready\",\"detail\":\"Basket is not available until the session has settled\"}");
            return;
        }

        String productId = params.get("productId");
        final String size = params.get("size");
        if (productId != null)
        {
            if (size != null && !size.isEmpty() && !productId.contains(":"))
            {
                productId = productId + ":" + size;
            }
            int qty = 1;
            try
            {
                qty = Integer.parseInt(params.getOrDefault("quantity", "1"));
            }
            catch (final NumberFormatException ignored)
            {
            }
            cart.items.merge(productId, qty, Integer::sum);
        }
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML)
           .result(renderer.getCartBadgeWrapperHtml(cart, trans, activeCountry, qualitySuffix, true));
    }

    private void handleCartUpdate(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                  final Map<String, String> trans, final Cart cart, final Map<String, String> params)
    {
        VerlaConfiguration.getInstance().simulateCartUpdate();
        final String productId = params.get("productId");
        int qty = 0;
        try
        {
            qty = Integer.parseInt(params.getOrDefault("quantity", "0"));
        }
        catch (final NumberFormatException ignored)
        {
        }
        if (productId != null)
        {
            if (qty <= 0)
            {
                cart.items.remove(productId);
            }
            else
            {
                cart.items.put(productId, qty);
            }
        }
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML)
           .result(renderer.getCartContentHtml(cart, activeCountry, trans, qualitySuffix));
    }

    private void handleCartDrawer(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                  final Map<String, String> trans, final Cart cart)
    {
        if (VerlaRenderer.isApparel(qualitySuffix))
        {
            ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON)
               .result(renderer.getCartDrawerHtmlApparel(cart, activeCountry, trans));
            return;
        }
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML)
           .result(renderer.getCartDrawerHtmlApocalypse(cart, activeCountry, trans));
    }

    private void handleCartRemove(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                  final Map<String, String> trans, final Cart cart, final String hxTarget,
                                  final Map<String, String> params)
    {
        VerlaConfiguration.getInstance().simulateCartRemove();
        final String queryProd = ctx.queryParam("productId");
        final String productId = (queryProd != null && !queryProd.isEmpty()) ? queryProd : params.get("productId");
        if (productId != null)
        {
            cart.items.remove(productId);
        }
        if ("cart-btn-wrapper".equals(hxTarget))
        {
            ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML)
               .result(renderer.getCartBadgeWrapperHtml(cart, trans, activeCountry, qualitySuffix));
        }
        else
        {
            ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML)
               .result(renderer.getCartContentHtml(cart, activeCountry, trans, qualitySuffix));
        }
    }

    private void handleCartCoupon(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                  final Map<String, String> trans, final Cart cart, final Map<String, String> params)
    {
        VerlaConfiguration.getInstance().simulateCartCoupon();
        final String coupon = params.getOrDefault("couponCode", "").trim();
        if (coupon.isEmpty())
        {
            cart.coupon = null;
            ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML)
               .result(renderer.getCartContentHtml(cart, activeCountry, trans, qualitySuffix));
            return;
        }

        final String lower = coupon.toLowerCase();
        if ("10p-off".equals(lower) || "freegift".equals(lower) || "freeship".equals(lower) || "bogo".equals(lower))
        {
            cart.coupon = lower;
            ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML)
               .result(renderer.getCartContentHtml(cart, activeCountry, trans, qualitySuffix));
        }
        else
        {
            final String errorMsg = trans.getOrDefault("error", "Error") + ": This promo code is expired or invalid.";
            ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML)
               .result(renderer.getCartContentHtml(cart, activeCountry, trans, qualitySuffix, errorMsg));
        }
    }

    private void handleSearchSuggest(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                     final String locale, final Map<String, String> params)
    {
        VerlaConfiguration.getInstance().simulateSearchSuggest();
        final String query = params.getOrDefault("q", "").trim().toLowerCase();
        if (query.isEmpty())
        {
            ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result("");
            return;
        }

        final List<Product> matches = new ArrayList<>();
        for (final Product p : catalog.getProducts())
        {
            final String enName = p.names.getOrDefault("en", "").toLowerCase();
            final String localName = p.names.getOrDefault(locale, "").toLowerCase();
            if (enName.contains(query) || localName.contains(query) || p.id.toLowerCase().contains(query))
            {
                matches.add(p);
            }
        }

        final StringBuilder sb = new StringBuilder();
        final int limit = Math.min(6, matches.size());
        for (int i = 0; i < limit; i++)
        {
            final Product p = matches.get(i);
            final double price = p.salePrice != null ? p.salePrice : p.basePrice;
            final String name = p.names.getOrDefault(locale, p.names.get("en"));
            if (VerlaRenderer.isTailwindByClaude(qualitySuffix) || VerlaRenderer.isApparel(qualitySuffix))
            {
                sb.append("<a href=\"p/").append(p.slug).append(".html\" class=\"flex items-center gap-3 border-b border-sand-200 px-4 py-2.5 text-ink transition-colors last:border-b-0 hover:bg-sand-50\">")
                  .append("  <div class=\"flex h-9 w-9 shrink-0 items-center justify-center rounded border border-sand-200 bg-sand-50\">")
                  .append("    <svg viewBox=\"0 0 100 100\" class=\"h-[70%] w-[70%] text-terracotta-500\">").append(p.svgPath).append("</svg>")
                  .append("  </div>")
                  .append("  <div class=\"flex flex-col text-left\">")
                  .append("    <span class=\"text-[13px] font-medium\">").append(VerlaRenderer.escapeHtml(name)).append("</span>")
                  .append("    <span class=\"text-[11px] text-muted\">").append(VerlaRenderer.formatPrice(price, activeCountry)).append("</span>")
                  .append("  </div>")
                  .append("</a>");
            }
            else
            {
                sb.append("<a href=\"p/").append(p.slug).append(".html\" class=\"search-suggestion-item\">")
                  .append("  <div class=\"search-suggestion-thumb\">")
                  .append("    <svg viewBox=\"0 0 100 100\">").append(p.svgPath).append("</svg>")
                  .append("  </div>")
                  .append("  <div class=\"search-suggestion-info\">")
                  .append("    <span class=\"search-suggestion-name\">").append(VerlaRenderer.escapeHtml(name)).append("</span>")
                  .append("    <span class=\"search-suggestion-price\">").append(VerlaRenderer.formatPrice(price, activeCountry)).append("</span>")
                  .append("  </div>")
                  .append("</a>");
            }
        }

        if (matches.size() > 6)
        {
            final int extra = matches.size() - 6;
            final String viewAllUrl = "plp.html?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            final String moreLinkClass = (VerlaRenderer.isTailwindByClaude(qualitySuffix) || VerlaRenderer.isApparel(qualitySuffix))
                ? "block bg-sand-50 p-2.5 text-center text-xs font-medium text-terracotta-500 transition-colors hover:text-terracotta-700"
                : "search-more-link";
            sb.append("<a href=\"").append(viewAllUrl).append("\" class=\"").append(moreLinkClass).append("\">")
              .append("+ ").append(extra).append(" more products. View all.")
              .append("</a>");
        }

        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result(sb.toString());
    }

    private void handleAuthLogin(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                 final Map<String, String> trans, final User currentUser, final Cart cart,
                                 final boolean isHtmx, final Map<String, String> params, final TestSession session)
    {
        VerlaConfiguration.getInstance().simulateAuthLogin();
        final String email = params.getOrDefault("email", "").trim();
        final String password = params.getOrDefault("password", "");

        final User user = session.getUsersDb().get(email);
        if (user != null && user.password.equals(password))
        {
            final String newSessionId = UUID.randomUUID().toString();
            session.getActiveSessions().put(newSessionId, email);
            final Cookie cookie = new Cookie("verla_session_id", newSessionId, "/", 3600, false);
            cookie.setHttpOnly(true);
            ctx.cookie(cookie);

            ctx.header("HX-Redirect", "/verla-" + qualitySuffix + "/account.html");
            ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result("");
        }
        else
        {
            final Map<String, String> model = new HashMap<>();
            model.put("auth_error", "Invalid email or password.");
            model.put("input_email", VerlaRenderer.escapeHtml(email));
            final String rendered = renderer.renderTemplate(qualitySuffix, "login.html", model, activeCountry, trans,
                currentUser, cart, "", isHtmx, session);
            ctx.status(HttpStatus.BAD_REQUEST).contentType(ContentType.TEXT_HTML).result(rendered != null ? rendered : "");
        }
    }

    private void handleAuthRegister(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                    final Map<String, String> trans, final User currentUser, final Cart cart,
                                    final boolean isHtmx, final Map<String, String> params, final TestSession session)
    {
        if (!"PUT".equalsIgnoreCase(ctx.method().name()))
        {
            ctx.status(HttpStatus.METHOD_NOT_ALLOWED).contentType(ContentType.TEXT_PLAIN)
               .result("Method Not Allowed: Registration requires PUT");
            return;
        }
        VerlaConfiguration.getInstance().simulateAuthRegister();
        final String email = params.getOrDefault("email", "").trim();
        final String password = params.getOrDefault("password", "");
        final String confirmPassword = params.getOrDefault("confirmPassword", "");

        final Map<String, String> model = new HashMap<>();
        model.put("input_email", VerlaRenderer.escapeHtml(email));

        if (email.isEmpty() || !email.contains("@"))
        {
            model.put("err_email", "Please enter a valid email address.");
            final String rendered = renderer.renderTemplate(qualitySuffix, "register.html", model, activeCountry, trans,
                currentUser, cart, "", isHtmx, session);
            ctx.status(HttpStatus.BAD_REQUEST).contentType(ContentType.TEXT_HTML).result(rendered != null ? rendered : "");
            return;
        }
        if (password.length() < 6)
        {
            model.put("err_password", "Password must be at least 6 characters.");
            final String rendered = renderer.renderTemplate(qualitySuffix, "register.html", model, activeCountry, trans,
                currentUser, cart, "", isHtmx, session);
            ctx.status(HttpStatus.BAD_REQUEST).contentType(ContentType.TEXT_HTML).result(rendered != null ? rendered : "");
            return;
        }
        if (!password.equals(confirmPassword))
        {
            model.put("err_confirmPassword", "Passwords do not match.");
            final String rendered = renderer.renderTemplate(qualitySuffix, "register.html", model, activeCountry, trans,
                currentUser, cart, "", isHtmx, session);
            ctx.status(HttpStatus.BAD_REQUEST).contentType(ContentType.TEXT_HTML).result(rendered != null ? rendered : "");
            return;
        }
        if (session.getUsersDb().containsKey(email))
        {
            model.put("auth_error", "An account with this email already exists.");
            final String rendered = renderer.renderTemplate(qualitySuffix, "register.html", model, activeCountry, trans,
                currentUser, cart, "", isHtmx, session);
            ctx.status(HttpStatus.BAD_REQUEST).contentType(ContentType.TEXT_HTML).result(rendered != null ? rendered : "");
            return;
        }

        final User newUser = new User(email, password);
        session.getUsersDb().put(email, newUser);

        final String newSessionId = UUID.randomUUID().toString();
        session.getActiveSessions().put(newSessionId, email);
        final Cookie cookie = new Cookie("verla_session_id", newSessionId, "/", 3600, false);
        cookie.setHttpOnly(true);
        ctx.cookie(cookie);

        ctx.header("HX-Redirect", "/verla-" + qualitySuffix + "/account.html");
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result("");
    }

    private void handleAuthChangePassword(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                          final Map<String, String> trans, final User currentUser, final Cart cart,
                                          final boolean isHtmx, final Map<String, String> params, final TestSession session)
    {
        final String current = params.get("currentPassword");
        final String newPass = params.get("newPassword");
        final String confirm = params.get("confirmNewPassword");

        final Map<String, String> model = new HashMap<>();
        if (currentUser == null)
        {
            ctx.header("HX-Redirect", "/verla-" + qualitySuffix + "/login.html");
            ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result("");
            return;
        }
        if (!currentUser.password.equals(current))
        {
            model.put("password_error", "Current password is incorrect.");
        }
        else if (newPass == null || newPass.length() < 6)
        {
            model.put("password_error", "New password must be at least 6 characters.");
        }
        else if (!newPass.equals(confirm))
        {
            model.put("password_error", "Passwords do not match.");
        }
        else
        {
            currentUser.password = newPass;
            model.put("password_status", "Password updated successfully!");
        }
        final String rendered = renderer.renderTemplate(qualitySuffix, "account.html", model, activeCountry, trans,
            currentUser, cart, "", isHtmx, session);
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result(rendered != null ? rendered : "");
    }

    private void handleAuthAddressAdd(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                      final Map<String, String> trans, final User currentUser, final Cart cart,
                                      final boolean isHtmx, final Map<String, String> params, final TestSession session)
    {
        final Map<String, String> model = new HashMap<>();
        if (currentUser != null)
        {
            final String street = params.get("street");
            final String city = params.get("city");
            final String state = params.getOrDefault("state", "");
            final String postcode = params.get("postcode");
            final String country = params.get("country");
            final String id = "addr-" + UUID.randomUUID().toString().substring(0, 8);

            currentUser.addresses.add(new Address(id, street, city, state, postcode, country));
        }
        final String rendered = renderer.renderTemplate(qualitySuffix, "account.html", model, activeCountry, trans,
            currentUser, cart, "", isHtmx, session);
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result(rendered != null ? rendered : "");
    }

    private void handleAuthAddressDelete(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                         final Map<String, String> trans, final User currentUser, final Cart cart,
                                         final boolean isHtmx, final Map<String, String> params, final TestSession session)
    {
        final Map<String, String> model = new HashMap<>();
        final String id = params.get("id");
        if (currentUser != null && id != null)
        {
            currentUser.addresses.removeIf(a -> a.id.equals(id));
        }
        final String rendered = renderer.renderTemplate(qualitySuffix, "account.html", model, activeCountry, trans,
            currentUser, cart, "", isHtmx, session);
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result(rendered != null ? rendered : "");
    }

    private void handleAuthCardAdd(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                   final Map<String, String> trans, final User currentUser, final Cart cart,
                                   final boolean isHtmx, final Map<String, String> params, final TestSession session)
    {
        final Map<String, String> model = new HashMap<>();
        if (currentUser != null)
        {
            final String number = params.get("cardNumber");
            final String type = params.get("cardType");
            final String expiry = params.get("cardExpiry");
            final String cvv = params.get("cardCvv");
            final String id = "card-" + UUID.randomUUID().toString().substring(0, 8);

            currentUser.cards.add(new Card(id, number, type, expiry, cvv));
        }
        final String rendered = renderer.renderTemplate(qualitySuffix, "account.html", model, activeCountry, trans,
            currentUser, cart, "", isHtmx, session);
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result(rendered != null ? rendered : "");
    }

    private void handleAuthCardDelete(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                      final Map<String, String> trans, final User currentUser, final Cart cart,
                                      final boolean isHtmx, final Map<String, String> params, final TestSession session)
    {
        final Map<String, String> model = new HashMap<>();
        final String id = params.get("id");
        if (currentUser != null && id != null)
        {
            currentUser.cards.removeIf(c -> c.id.equals(id));
        }
        final String rendered = renderer.renderTemplate(qualitySuffix, "account.html", model, activeCountry, trans,
            currentUser, cart, "", isHtmx, session);
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result(rendered != null ? rendered : "");
    }

    private void handleCheckoutPurchase(final Context ctx, final String qualitySuffix, final Country activeCountry,
                                        final String locale, final Map<String, String> trans, final User currentUser,
                                        final Cart cart, final boolean isHtmx, final Map<String, String> params,
                                        final TestSession session)
    {
        VerlaConfiguration.getInstance().simulatePurchase();

        final String firstName = params.getOrDefault("firstName", "").trim();
        final String lastName = params.getOrDefault("lastName", "").trim();
        final String email = params.getOrDefault("email", "").trim();
        final String cardType = params.getOrDefault("cardType", "");
        final String cardNumber = params.getOrDefault("cardNumber", "").trim().replace(" ", "");
        final String cardExpiry = params.getOrDefault("cardExpiry", "");
        final String cardCvv = params.getOrDefault("cardCvv", "");

        final String street = params.getOrDefault("address", params.getOrDefault("street", ""));
        final String city = params.getOrDefault("city", "");
        final String postcode = params.getOrDefault("postcode", params.getOrDefault("zipCode", ""));
        final String country = params.getOrDefault("country", "US");
        final String state = params.getOrDefault("state", "");

        final Map<String, String> model = new HashMap<>();
        model.put("input_firstName", VerlaRenderer.escapeHtml(firstName));
        model.put("input_lastName", VerlaRenderer.escapeHtml(lastName));
        model.put("input_email", VerlaRenderer.escapeHtml(email));
        model.put("input_cardNumber", VerlaRenderer.escapeHtml(cardNumber));
        model.put("input_cardExpiry", VerlaRenderer.escapeHtml(cardExpiry));
        model.put("input_cardCvv", VerlaRenderer.escapeHtml(cardCvv));

        boolean hasErrors = false;
        if (firstName.isEmpty())
        {
            model.put("err_firstName", trans.getOrDefault("errFirstNameRequired", "First name is required."));
            hasErrors = true;
        }
        if (lastName.isEmpty())
        {
            model.put("err_lastName", trans.getOrDefault("errLastNameRequired", "Last name is required."));
            hasErrors = true;
        }
        if (email.isEmpty() || !email.contains("@"))
        {
            model.put("err_email", trans.getOrDefault("errEmailRequired", "Valid email is required."));
            hasErrors = true;
        }

        if (cardNumber.isEmpty())
        {
            model.put("err_cardNumber", trans.getOrDefault("errCardNumberRequired", "Card number is required."));
            hasErrors = true;
        }
        else if (cardNumber.endsWith("200"))
        {
            model.put("err_cardNumber", trans.getOrDefault("errCardDeclined", "Payment declined for testing purposes."));
            hasErrors = true;
        }
        else if (!cardNumber.replaceAll("\\s+", "").matches("\\d+"))
        {
            model.put("err_cardNumber", trans.getOrDefault("errCardNumberRequired", "Card number must contain digits only."));
            hasErrors = true;
        }

        if (cardExpiry.isEmpty())
        {
            model.put("err_cardExpiry", trans.getOrDefault("errCardExpiryRequired", "Expiry date is required."));
            hasErrors = true;
        }
        if (cardCvv.isEmpty() || !cardCvv.matches("\\d{3,4}"))
        {
            model.put("err_cardCvv", trans.getOrDefault("errCardCvvRequired", "CVV is required."));
            hasErrors = true;
        }

        if (hasErrors)
        {
            final String rendered = renderer.renderTemplate(qualitySuffix, "checkout.html", model, activeCountry, trans,
                currentUser, cart, "", isHtmx, session);
            ctx.status(HttpStatus.BAD_REQUEST).contentType(ContentType.TEXT_HTML).result(rendered != null ? rendered : "");
            return;
        }

        final double subtotal = renderer.calculateSubtotal(cart);
        final double discount = renderer.calculateDiscount(cart, subtotal);
        final double shipping = VerlaRenderer.calculateShipping(cart, subtotal);
        final double tax = Math.round((subtotal - discount) * 0.1 * 100.0) / 100.0;
        final double total = subtotal - discount + shipping + tax;

        final int orderRand = ThreadLocalRandom.current().nextInt(1000000, 10000000);
        final String orderNum = "V-" + orderRand + "-" + activeCountry.code;
        final Address shippingAddr = new Address("addr-order", street, city, state, postcode, country);
        final Card paymentCard = new Card("card-order", cardNumber, cardType, cardExpiry, cardCvv);
        final boolean giftAdded = "freegift".equals(cart.coupon);

        final List<OrderItem> items = new ArrayList<>();
        for (final Map.Entry<String, Integer> entry : cart.items.entrySet())
        {
            final String cartKey = entry.getKey();
            final String prodId;
            final String size;
            if (cartKey.contains(":"))
            {
                final String[] parts = cartKey.split(":");
                prodId = parts[0];
                size = parts[1];
            }
            else
            {
                prodId = cartKey;
                size = "";
            }

            final Product prod = catalog.lookupProductById(prodId);
            if (prod != null)
            {
                final double itemPrice = prod.salePrice != null ? prod.salePrice : prod.basePrice;
                items.add(new OrderItem(prod.id, prod.names.getOrDefault(locale, prod.names.get("en")), entry.getValue(), itemPrice));

                final Map<String, Integer> stockMap = session.getProductInventory().get(prodId);
                if (stockMap != null)
                {
                    final int currentStock = stockMap.getOrDefault(size, 0);
                    final int qtyBought = entry.getValue();
                    final int newStock = Math.max(0, currentStock - qtyBought);
                    stockMap.put(size, newStock);
                }
            }
        }

        final Order order = new Order(orderNum, email, items, subtotal, shipping, tax, total, "Processing", shippingAddr,
            paymentCard, cart.coupon, giftAdded);
        session.getOrdersDb().put(orderNum, order);

        if (currentUser != null)
        {
            currentUser.orders.add(order);
        }

        cart.items.clear();
        cart.coupon = null;

        final String thankYouMsg = trans.getOrDefault("thankYou", "Thank you for your purchase!");
        final String orderPlacedMsg = trans.getOrDefault("orderPlaced", "Your order has been placed successfully.");
        final String orderNumLabel = trans.getOrDefault("orderNumber", "Order Number");
        final String zipCodeLabel = trans.getOrDefault("shippingZipCode", "Shipping Zip Code");
        final String totalPaidLabel = trans.getOrDefault("totalPaid", "Total Paid");
        final String trackMsg = trans.getOrDefault("trackInstructions",
            "Use the Order Number and Shipping Zip Code to track your package on the <a href=\"track-orders.html\" style=\"color: var(--color-accent); font-weight: 600;\">Track Orders</a> page.");
        final String continueShoppingMsg = trans.getOrDefault("continueShopping", "Continue Shopping");

        final String successHtml;
        if ("apocalypse".equals(qualitySuffix))
        {
            successHtml = "<div style=\"text-align:center; padding: 40px 20px; background: var(--apoc-bg-surface); border: 1px solid var(--apoc-border); border-radius: 12px; max-width: 600px; margin: 0 auto;\">" +
                          "  <svg class=\"success-icon\" width=\"64\" height=\"64\" viewBox=\"0 0 64 64\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\" style=\"margin: 0 auto 20px auto; display: block;\">" +
                          "    <circle cx=\"32\" cy=\"32\" r=\"30\" fill=\"var(--apoc-gold)\" />" +
                          "    <path d=\"M20 32L28 40L44 24\" stroke=\"#000\" stroke-width=\"6\" stroke-linecap=\"round\" stroke-linejoin=\"round\" />" +
                          "  </svg>" +
                          "  <h2 id=\"success-message\" style=\"font-family: var(--apoc-font-serif); font-size: 28px; color: #fff; margin-bottom: 12px;\">" + thankYouMsg + "</h2>" +
                          "  <p id=\"order-placed-message\" style=\"color: var(--apoc-text-secondary); margin-bottom: 24px;\">" + orderPlacedMsg + "</p>" +
                          "  <div style=\"background-color: var(--apoc-bg-elevated); border: 1px solid var(--apoc-border); padding: 24px; border-radius: 8px; text-align: left; max-width: 480px; margin: 0 auto 30px auto;\">" +
                          "    <div style=\"margin-bottom:10px; color:var(--apoc-text-secondary);\"><strong>" + orderNumLabel + ":</strong> <span id=\"order-number-value\" style=\"color:var(--apoc-gold); font-weight:700;\">" + orderNum + "</span></div>" +
                          "    <div style=\"margin-bottom:10px; color:var(--apoc-text-secondary);\"><strong>" + zipCodeLabel + ":</strong> <span id=\"zip-code-value\" style=\"color:#fff;\">" + postcode + "</span></div>" +
                          "    <div style=\"margin-bottom:10px; color:var(--apoc-text-secondary);\"><strong>" + totalPaidLabel + ":</strong> <span style=\"color:var(--apoc-gold); font-weight:700;\">" + VerlaRenderer.formatPrice(total, activeCountry) + "</span></div>" +
                          "    <div style=\"font-size: 12px; color: var(--apoc-text-muted); margin-top: 16px;\">" + trackMsg + "</div>" +
                          "  </div>" +
                          "  <a href=\"index.html\" class=\"apoc-btn-gold\" style=\"display:inline-block; padding: 12px 24px;\">" + continueShoppingMsg + "</a>" +
                          "</div>";
        }
        else
        {
            successHtml = "<div style=\"text-align:center; padding: 40px 20px;\">" +
                          "  <svg class=\"success-icon\" width=\"64\" height=\"64\" viewBox=\"0 0 64 64\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\" style=\"margin: 0 auto 20px auto; display: block;\">" +
                          "    <circle cx=\"32\" cy=\"32\" r=\"30\" fill=\"#5F8766\" />" +
                          "    <path d=\"M20 32L28 40L44 24\" stroke=\"white\" stroke-width=\"6\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-linejoin=\"round\" />" +
                          "  </svg>" +
                          "  <h2 id=\"success-message\" style=\"font-family: var(--font-family-serif); font-size: 28px; margin-bottom: 12px;\">" + thankYouMsg + "</h2>" +
                          "  <p id=\"order-placed-message\" style=\"color: var(--color-text-secondary); margin-bottom: 24px;\">" + orderPlacedMsg + "</p>" +
                          "  <div style=\"background-color: var(--color-bg-secondary); border: 1px solid var(--color-border); padding: 24px; border-radius: var(--border-radius); text-align: left; max-width: 480px; margin: 0 auto 30px auto;\">" +
                          "    <div style=\"margin-bottom:10px;\"><strong>" + orderNumLabel + ":</strong> <span id=\"order-number-value\">" + orderNum + "</span></div>" +
                          "    <div style=\"margin-bottom:10px;\"><strong>" + zipCodeLabel + ":</strong> <span id=\"zip-code-value\">" + postcode + "</span></div>" +
                          "    <div style=\"margin-bottom:10px;\"><strong>" + totalPaidLabel + ":</strong> " + VerlaRenderer.formatPrice(total, activeCountry) + "</div>" +
                          "    <div style=\"font-size: 12px; color: var(--color-text-secondary); margin-top: 16px;\">" + trackMsg + "</div>" +
                          "  </div>" +
                          "  <a href=\"index.html\" class=\"btn-primary\" style=\"display:inline-block;\">" + continueShoppingMsg + "</a>" +
                          "</div>";
        }
        ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result(successHtml);
    }

    private void handleOrderLookup(final Context ctx, final Country activeCountry, final Map<String, String> trans,
                                   final Map<String, String> params, final TestSession session)
    {
        VerlaConfiguration.getInstance().simulateOrderLookup();
        final String orderNumber = params.getOrDefault("orderNumber", "").trim();
        final String zipCode = params.getOrDefault("zipCode", "").trim();

        final String statusLabel = trans.getOrDefault("status", "Status");
        final String recipientLabel = trans.getOrDefault("recipient", "Recipient");
        final String itemsLabel = trans.getOrDefault("items", "Items");
        final String freeGiftLabel = trans.getOrDefault("freeGift", "Free Gift");
        final String totalLabel = trans.getOrDefault("total", "Total");

        final Order order = session.getOrdersDb().get(orderNumber);
        if (order != null && order.shippingAddress.postcode.equalsIgnoreCase(zipCode))
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("<div style=\"background-color: var(--color-bg-primary); border: 1px solid var(--color-border); padding: 24px; border-radius: var(--border-radius); margin-top: 20px;\">")
              .append("  <h4 style=\"font-family: var(--font-family-serif); font-size: 18px; margin-bottom: 16px;\">Order ").append(order.orderNumber).append("</h4>")
              .append("  <div style=\"margin-bottom: 12px;\"><strong>").append(statusLabel).append(":</strong> <span style=\"color: var(--color-success); font-weight: 600;\">").append(order.status).append("</span></div>")
              .append("  <div style=\"margin-bottom: 12px;\"><strong>").append(recipientLabel).append(":</strong> ").append(order.shippingAddress.street).append(", ").append(order.shippingAddress.city).append("</div>")
              .append("  <h5 style=\"font-size: 13px; font-weight: 600; text-transform: uppercase; margin-bottom: 10px;\">").append(itemsLabel).append("</h5>")
              .append("  <ul style=\"list-style: none; display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px;\">");

            for (final OrderItem item : order.items)
            {
                sb.append("    <li style=\"display:flex; justify-content: space-between; font-size: 13px;\">")
                  .append("      <span>").append(item.productName).append(" &times; ").append(item.quantity).append("</span>")
                  .append("      <span>").append(VerlaRenderer.formatPrice(item.price * item.quantity, activeCountry)).append("</span>")
                  .append("    </li>");
            }
            if (order.freeGiftAdded)
            {
                sb.append("    <li style=\"display:flex; justify-content: space-between; font-size: 13px; color: var(--color-success); font-weight: 500;\">")
                  .append("      <span>VÉRLA Signature Tote Bag &times; 1</span>")
                  .append("      <span>").append(freeGiftLabel).append("</span>")
                  .append("    </li>");
            }
            sb.append("  </ul>")
              .append("  <div style=\"display: flex; justify-content: space-between; font-weight: 600; border-top: 1px solid var(--color-border); padding-top: 12px;\">")
              .append("    <span>").append(totalLabel).append("</span>")
              .append("    <span>").append(VerlaRenderer.formatPrice(order.total, activeCountry)).append("</span>")
              .append("  </div>")
              .append("</div>");
            ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML).result(sb.toString());
        }
        else
        {
            ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_HTML)
               .result("<div class=\"error-message\" style=\"margin-top: 20px; text-align: center;\">Order not found or invalid Zip Code.</div>");
        }
    }

    private void handleAuthLogout(final Context ctx, final String qualitySuffix, final TestSession session)
    {
        final String sessionId = ctx.cookie("verla_session_id");
        if (sessionId != null)
        {
            session.getActiveSessions().remove(sessionId);
        }
        ctx.removeCookie("verla_session_id", "/");
        ctx.redirect("/verla-" + qualitySuffix + "/index.html", HttpStatus.FOUND);
    }

    private TestSession resolveSession(final Context ctx)
    {
        final TestSession session = ctx.attribute("testSession");
        if (session != null)
        {
            return session;
        }
        final String cookieToken = ctx.cookie(SessionManager.COOKIE_NAME);
        return sessionManager.resolveSession(cookieToken);
    }

    private String resolveCartId(final Context ctx)
    {
        String cartId = ctx.cookie("verla_cart_id");
        if (cartId == null || cartId.isBlank())
        {
            cartId = UUID.randomUUID().toString();
            final Cookie cookie = new Cookie("verla_cart_id", cartId, "/", -1, false);
            cookie.setHttpOnly(true);
            ctx.cookie(cookie);
        }
        return cartId;
    }

    private User resolveUser(final Context ctx, final TestSession session)
    {
        final String sessionId = ctx.cookie("verla_session_id");
        if (sessionId != null)
        {
            final String email = session.getActiveSessions().get(sessionId);
            if (email != null)
            {
                return session.getUsersDb().get(email);
            }
        }
        return null;
    }

    private Country resolveCountry(final Context ctx)
    {
        final String countryCode = ctx.cookie("verla_country");
        return catalog.getCountry(countryCode != null ? countryCode : "US");
    }

    private static Map<String, String> extractParams(final Context ctx)
    {
        final Map<String, String> params = new HashMap<>();
        ctx.queryParamMap().forEach((k, v) ->
        {
            if (!v.isEmpty())
            {
                params.put(k, v.get(0));
            }
        });
        ctx.formParamMap().forEach((k, v) ->
        {
            if (!v.isEmpty())
            {
                params.put(k, v.get(0));
            }
        });
        return params;
    }

    private static String queryParamOrDefault(final Context ctx, final String key, final String defaultValue)
    {
        final String val = ctx.queryParam(key);
        return val != null ? val : defaultValue;
    }
}

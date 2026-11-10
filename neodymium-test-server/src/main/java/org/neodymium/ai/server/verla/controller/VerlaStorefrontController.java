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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.neodymium.ai.server.session.SessionManager;
import org.neodymium.ai.server.session.TestSession;
import org.neodymium.ai.server.verla.catalog.VerlaCatalog;
import org.neodymium.ai.server.verla.model.Cart;
import org.neodymium.ai.server.verla.model.Country;
import org.neodymium.ai.server.verla.model.User;
import org.neodymium.ai.server.verla.renderer.VerlaRenderer;
import org.neodymium.ai.util.VerlaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.HttpStatus;

/**
 * Handles VÉRLA dynamic storefront pages (PDP, PLP, Cart, Checkout, Account, etc.) and static asset fallbacks.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class VerlaStorefrontController
{
    private static final Logger LOG = LoggerFactory.getLogger(VerlaStorefrontController.class);

    private final SessionManager sessionManager;
    private final VerlaCatalog catalog;
    private final VerlaRenderer renderer;

    public VerlaStorefrontController(final SessionManager sessionManager, final VerlaCatalog catalog, final VerlaRenderer renderer)
    {
        this.sessionManager = sessionManager != null ? sessionManager : new SessionManager();
        this.catalog = catalog != null ? catalog : VerlaCatalog.getInstance();
        this.renderer = renderer != null ? renderer : new VerlaRenderer(this.catalog);
    }

    /**
     * Handles an incoming storefront request for a specific page or asset.
     *
     * @param ctx the Javalin context
     * @param qualitySuffix the SUT quality flavor
     * @param pagePath the requested page path relative to the flavor base
     */
    public void handle(final Context ctx, final String qualitySuffix, final String pagePath)
    {
        final TestSession session = resolveSession(ctx);
        final String cartId = resolveCartId(ctx);
        final Cart cart = session.getActiveCarts().computeIfAbsent(cartId, Cart::new);
        final User currentUser = resolveUser(ctx, session);
        final Country activeCountry = resolveCountry(ctx);
        final String locale = activeCountry.locale;
        final Map<String, String> trans = catalog.getTranslations(locale);

        final boolean isPwaRouter = "true".equalsIgnoreCase(ctx.header("X-PWA-Router"));
        final String hxTarget = ctx.header("HX-Target");
        final boolean isHtmx = "true".equalsIgnoreCase(ctx.header("HX-Request")) || isPwaRouter;

        final String pageResource;
        if (pagePath.isEmpty() || "index.html".equals(pagePath))
        {
            pageResource = "index.html";
        }
        else if (pagePath.startsWith("p/"))
        {
            pageResource = "pdp.html";
        }
        else if (pagePath.startsWith("c/"))
        {
            pageResource = "plp.html";
        }
        else
        {
            pageResource = pagePath;
        }

        if ("plp.html".equals(pageResource) && ctx.queryString() != null && ctx.queryString().contains("q="))
        {
            VerlaConfiguration.getInstance().simulateSearchQuery();
        }

        final String templateHtml = renderer.renderTemplate(qualitySuffix, pageResource, new HashMap<>(), activeCountry,
            trans, currentUser, cart, ctx.fullUrl(), isHtmx, hxTarget, isPwaRouter, session);

        if (templateHtml != null)
        {
            ctx.status(HttpStatus.OK).contentType("text/html; charset=UTF-8").result(templateHtml);
            return;
        }

        serveStaticAsset(ctx, qualitySuffix, pagePath);
    }

    private void serveStaticAsset(final Context ctx, final String qualitySuffix, final String pagePath)
    {
        final ClassLoader cl = getClass().getClassLoader();
        InputStream is = cl.getResourceAsStream("ai-test-pages/verla-" + qualitySuffix + "/" + pagePath);
        if (is == null)
        {
            is = cl.getResourceAsStream("ai-test-pages/" + pagePath);
        }

        if (is == null)
        {
            LOG.warn("Storefront static resource not found: '{}' for flavor '{}'", pagePath, qualitySuffix);
            ctx.status(HttpStatus.NOT_FOUND).contentType(ContentType.TEXT_PLAIN).result("404 Not Found");
            return;
        }

        try (final InputStream stream = is)
        {
            final byte[] bytes = stream.readAllBytes();
            final String contentType = determineContentType(pagePath);
            ctx.status(HttpStatus.OK).contentType(contentType).result(bytes);
        }
        catch (final Exception e)
        {
            LOG.error("Failed to read static resource '{}'", pagePath, e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Internal Server Error");
        }
    }

    private static String determineContentType(final String path)
    {
        if (path.endsWith(".html"))
        {
            return "text/html; charset=UTF-8";
        }
        if (path.endsWith(".css"))
        {
            return "text/css; charset=UTF-8";
        }
        if (path.endsWith(".js"))
        {
            return "application/javascript; charset=UTF-8";
        }
        if (path.endsWith(".png"))
        {
            return "image/png";
        }
        if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
        {
            return "image/jpeg";
        }
        if (path.endsWith(".gif"))
        {
            return "image/gif";
        }
        if (path.endsWith(".svg"))
        {
            return "image/svg+xml";
        }
        if (path.endsWith(".pdf"))
        {
            return "application/pdf";
        }
        if (path.endsWith(".woff2"))
        {
            return "font/woff2";
        }
        if (path.endsWith(".json"))
        {
            return "application/json; charset=UTF-8";
        }
        return "text/plain; charset=UTF-8";
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
}

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
package org.neodymium.ai.server.verla;

import java.util.List;

import org.neodymium.ai.server.session.SessionManager;
import org.neodymium.ai.server.verla.catalog.VerlaCatalog;
import org.neodymium.ai.server.verla.controller.VerlaApiController;
import org.neodymium.ai.server.verla.controller.VerlaStorefrontController;
import org.neodymium.ai.server.verla.renderer.VerlaRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

/**
 * Mounts the VÉRLA mock e-commerce storefront subsystem, catalog APIs, and 11 storefront flavors into Javalin.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class VerlaApp
{
    private static final Logger LOG = LoggerFactory.getLogger(VerlaApp.class);

    public static final List<String> FLAVORS = List.of(
        "verla-tailwind-by-claude",
        "verla-apparel",
        "verla-apperal",
        "verla-tailwind",
        "verla-perfect",
        "verla-normal",
        "verla-bad",
        "verla-modern-bad-nowcag",
        "verla-modern-bad",
        "verla-pwa-chaos",
        "verla-apocalypse",
        "verla-headless"
    );

    private final VerlaApiController apiController;
    private final VerlaStorefrontController storefrontController;

    public VerlaApp(final SessionManager sessionManager)
    {
        final VerlaCatalog catalog = VerlaCatalog.getInstance();
        final VerlaRenderer renderer = new VerlaRenderer(catalog);
        this.apiController = new VerlaApiController(sessionManager, catalog, renderer);
        this.storefrontController = new VerlaStorefrontController(sessionManager, catalog, renderer);
    }

    public VerlaApp(final VerlaApiController apiController, final VerlaStorefrontController storefrontController)
    {
        this.apiController = apiController;
        this.storefrontController = storefrontController;
    }

    /**
     * Registers all VÉRLA storefront routes across all supported flavors on the Javalin application.
     *
     * @param app the Javalin application
     */
    public void registerRoutes(final Javalin app)
    {
        for (final String flavor : FLAVORS)
        {
            final String baseNoSlash = "/" + flavor;
            final String baseWithSlash = "/" + flavor + "/";

            // Directory redirect normalization
            app.get(baseNoSlash, ctx -> ctx.redirect(baseWithSlash + "index.html", HttpStatus.FOUND));
            app.get(baseWithSlash, ctx -> ctx.redirect(baseWithSlash + "index.html", HttpStatus.FOUND));

            final Handler handler = ctx -> dispatch(ctx, flavor, baseWithSlash);

            app.get(baseNoSlash + "/*", handler);
            app.post(baseNoSlash + "/*", handler);
            app.put(baseNoSlash + "/*", handler);
            app.delete(baseNoSlash + "/*", handler);
            app.head(baseNoSlash + "/*", handler);
        }

        LOG.info("Registered VÉRLA application routes for {} flavors.", FLAVORS.size());
    }

    private void dispatch(final Context ctx, final String flavor, final String baseWithSlash)
    {
        final String qualitySuffix = resolveQualitySuffix(flavor);
        final String path = ctx.path();
        final String relative;
        if (path.startsWith(baseWithSlash))
        {
            relative = path.substring(baseWithSlash.length());
        }
        else
        {
            final int idx = path.indexOf(flavor);
            if (idx != -1)
            {
                relative = path.substring(idx + flavor.length() + 1);
            }
            else
            {
                relative = path;
            }
        }

        LOG.debug("VERLA dispatch: path={}, flavor={}, quality={}, relative={}", path, flavor, qualitySuffix, relative);
        if (relative.startsWith("api/"))
        {
            apiController.handle(ctx, qualitySuffix, relative.substring(4));
        }
        else
        {
            storefrontController.handle(ctx, qualitySuffix, relative);
        }
    }

    /**
     * Resolves the internal quality suffix from a URL flavor prefix.
     *
     * @param flavor the URL flavor string
     * @return the resolved quality suffix
     */
    public static String resolveQualitySuffix(final String flavor)
    {
        if ("verla-apperal".equals(flavor) || "verla-apparel".equals(flavor))
        {
            return "apparel";
        }
        if (flavor.startsWith("verla-"))
        {
            return flavor.substring("verla-".length());
        }
        return flavor;
    }

    public VerlaApiController getApiController()
    {
        return apiController;
    }

    public VerlaStorefrontController getStorefrontController()
    {
        return storefrontController;
    }
}

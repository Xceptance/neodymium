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
package org.neodymium.ai.server.auraglance;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.neodymium.ai.server.session.SessionManager;
import org.neodymium.ai.server.session.TestSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

/**
 * Controller serving Aura Glance test hub pages, sandbox challenges, and dynamic simulation states.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class AuraGlanceController
{
    private static final Logger LOG = LoggerFactory.getLogger(AuraGlanceController.class);

    private static final Map<String, String> DIRECTORY_INDEX_REDIRECTS = Map.ofEntries(
        Map.entry("/AuraGlanceTest", "/AuraGlanceTest/index.html"),
        Map.entry("/AuraGlanceTest/", "/AuraGlanceTest/index.html"),
        Map.entry("/AuraGlanceTest/shop", "/AuraGlanceTest/shop/index.html"),
        Map.entry("/AuraGlanceTest/shop/", "/AuraGlanceTest/shop/index.html"),
        Map.entry("/AuraGlanceTest/dashboard", "/AuraGlanceTest/dashboard/index.html"),
        Map.entry("/AuraGlanceTest/dashboard/", "/AuraGlanceTest/dashboard/index.html"),
        Map.entry("/AuraGlanceTest/a11y", "/AuraGlanceTest/a11y/index.html"),
        Map.entry("/AuraGlanceTest/a11y/", "/AuraGlanceTest/a11y/index.html"),
        Map.entry("/AuraGlanceTest/spa", "/AuraGlanceTest/spa/index.html"),
        Map.entry("/AuraGlanceTest/spa/", "/AuraGlanceTest/spa/index.html"),
        Map.entry("/AuraGlanceTest/shop/sandbox", "/AuraGlanceTest/shop/sandbox/index.html"),
        Map.entry("/AuraGlanceTest/shop/sandbox/", "/AuraGlanceTest/shop/sandbox/index.html")
    );

    private final SessionManager sessionManager;

    public AuraGlanceController(final SessionManager sessionManager)
    {
        this.sessionManager = sessionManager != null ? sessionManager : new SessionManager();
    }

    /**
     * Handles incoming GET and HEAD requests for Aura Glance test pages and sandbox assets.
     *
     * @param ctx the Javalin context
     */
    public void handleGet(final Context ctx)
    {
        final String path = ctx.path();

        // 1. Directory redirect normalization
        final String redirectTarget = DIRECTORY_INDEX_REDIRECTS.get(path);
        if (redirectTarget != null)
        {
            ctx.redirect(redirectTarget, HttpStatus.FOUND);
            return;
        }

        // 2. Resolve session state
        final String cookieToken = ctx.cookie(SessionManager.COOKIE_NAME);
        final TestSession session = sessionManager.resolveSession(cookieToken);
        final AuraGlanceState state = session.getAuraGlanceState();

        state.setLastRequestedUri(ctx.fullUrl());
        state.incrementAndGetRequestCount();

        // 3. Simulated failure injection
        if (state.isSimulateServerFailure())
        {
            LOG.warn("Simulating 500 error for Aura Glance path: {}", path);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .contentType(ContentType.APPLICATION_JSON)
               .result("{\"error\":\"Simulated Aura Glance Server Failure\"}");
            return;
        }

        // 4. Simulated network delay
        final int delayMs = state.getArtificialDelayMs();
        if (delayMs > 0)
        {
            try
            {
                Thread.sleep(delayMs);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
                LOG.warn("Aura Glance simulated delay interrupted", e);
            }
        }

        // 5. Prevent directory traversal attacks
        if (path.contains(".."))
        {
            LOG.warn("Directory traversal attempt blocked: {}", path);
            ctx.status(HttpStatus.FORBIDDEN).result("Forbidden");
            return;
        }

        // 6. Map path to classpath resource
        final String relativePath = path.startsWith("/") ? path.substring(1) : path;
        final String resourcePath = "ai-test-pages/" + relativePath;

        try (final InputStream is = AuraGlanceController.class.getClassLoader().getResourceAsStream(resourcePath))
        {
            if (is == null)
            {
                LOG.debug("Aura Glance resource not found: {}", resourcePath);
                ctx.status(HttpStatus.NOT_FOUND).result("404 Not Found");
                return;
            }

            if (path.toLowerCase().endsWith(".html"))
            {
                ctx.contentType(ContentType.HTML);
                ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
                ctx.render(relativePath, Collections.emptyMap());
                return;
            }

            final byte[] content = is.readAllBytes();
            final String contentType = resolveContentType(path);

            ctx.status(HttpStatus.OK)
               .contentType(contentType)
               .header("Cache-Control", "no-cache, no-store, must-revalidate")
               .result(content);
        }
        catch (final Exception e)
        {
            LOG.error("Failed to serve Aura Glance resource '{}'", resourcePath, e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Internal Server Error");
        }
    }

    private static String resolveContentType(final String path)
    {
        final String lower = path.toLowerCase();
        if (lower.endsWith(".html"))
        {
            return "text/html; charset=UTF-8";
        }
        if (lower.endsWith(".css"))
        {
            return "text/css; charset=UTF-8";
        }
        if (lower.endsWith(".js"))
        {
            return "application/javascript; charset=UTF-8";
        }
        if (lower.endsWith(".json"))
        {
            return "application/json; charset=UTF-8";
        }
        if (lower.endsWith(".png"))
        {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
        {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif"))
        {
            return "image/gif";
        }
        if (lower.endsWith(".svg"))
        {
            return "image/svg+xml";
        }
        if (lower.endsWith(".ico"))
        {
            return "image/x-icon";
        }
        if (lower.endsWith(".woff"))
        {
            return "font/woff";
        }
        if (lower.endsWith(".woff2"))
        {
            return "font/woff2";
        }
        if (lower.endsWith(".ttf"))
        {
            return "font/ttf";
        }
        return "application/octet-stream";
    }
}

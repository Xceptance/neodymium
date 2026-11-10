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
package org.neodymium.ai.server.actionfixtures;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

/**
 * Controller delivering isolated action test fixture pages dynamically from templates.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class ActionFixturesController
{
    private static final Logger LOG = LoggerFactory.getLogger(ActionFixturesController.class);

    private static final Set<String> RESERVED_PREFIXES = Set.of(
        "api",
        "AuraGlanceTest"
    );

    /**
     * Handles dynamic delivery of individual action test fixture pages (e.g. /{suite}/{page.html}).
     *
     * @param ctx the Javalin context
     */
    public void handleActionPage(final Context ctx)
    {
        final String suite;
        final String page;
        final String cleanPath = ctx.path().startsWith("/") ? ctx.path().substring(1) : ctx.path();
        final int slashIdx = cleanPath.indexOf('/');
        if (slashIdx != -1)
        {
            suite = cleanPath.substring(0, slashIdx);
            page = cleanPath.substring(slashIdx + 1);
        }
        else
        {
            suite = cleanPath;
            page = "";
        }

        // 1. Bypass reserved sub-systems and storefronts
        if (RESERVED_PREFIXES.contains(suite) || suite.startsWith("verla-"))
        {
            ctx.status(HttpStatus.NOT_FOUND).result("404 Not Found");
            return;
        }

        // 2. Only serve HTML templates through this handler; bypass non-HTML assets
        if (!page.endsWith(".html"))
        {
            ctx.skipRemainingHandlers();
            return;
        }

        // 3. Security: Prevent directory traversal
        final String fullPath = ctx.path();
        if (fullPath.contains(".."))
        {
            LOG.warn("Directory traversal attempt blocked: {}", fullPath);
            ctx.status(HttpStatus.FORBIDDEN).result("Forbidden");
            return;
        }

        // 4. Validate existence of template on classpath
        final String templatePath = suite + "/" + page;
        final String resourcePath = "ai-test-pages/" + templatePath;

        try (final InputStream is = ActionFixturesController.class.getClassLoader().getResourceAsStream(resourcePath))
        {
            if (is == null)
            {
                LOG.debug("Action fixture template not found: {}", resourcePath);
                ctx.status(HttpStatus.NOT_FOUND).result("404 Not Found");
                return;
            }

            ctx.contentType(ContentType.HTML);
            ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
            ctx.render(templatePath, Collections.emptyMap());
        }
        catch (final Exception e)
        {
            LOG.error("Failed to render action fixture template '{}'", templatePath, e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Internal Server Error");
        }
    }

    /**
     * Handles dynamic delivery of the action test index portal (/action-test-pages.html).
     *
     * @param ctx the Javalin context
     */
    public void handleActionIndex(final Context ctx)
    {
        final String fullPath = ctx.path();
        if (fullPath.contains(".."))
        {
            LOG.warn("Directory traversal attempt blocked: {}", fullPath);
            ctx.status(HttpStatus.FORBIDDEN).result("Forbidden");
            return;
        }

        final String templatePath = "action-test-pages.html";
        final String resourcePath = "ai-test-pages/" + templatePath;

        try (final InputStream is = ActionFixturesController.class.getClassLoader().getResourceAsStream(resourcePath))
        {
            if (is == null)
            {
                LOG.debug("Action index template not found: {}", resourcePath);
                ctx.status(HttpStatus.NOT_FOUND).result("404 Not Found");
                return;
            }

            ctx.contentType(ContentType.HTML);
            ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
            ctx.render(templatePath, Collections.emptyMap());
        }
        catch (final Exception e)
        {
            LOG.error("Failed to render action index template '{}'", templatePath, e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Internal Server Error");
        }
    }
}

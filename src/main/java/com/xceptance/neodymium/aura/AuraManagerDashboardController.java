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
package com.xceptance.neodymium.aura;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.thymeleaf.context.Context;

/**
 * Controller handling dashboard core rendering, static themes, and assets.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerDashboardController
{
    private final AuraInteractiveService interactiveService;
    private final AuraFileService fileService;
    private final NeodymiumAuraManager manager;

    public AuraManagerDashboardController(final AuraInteractiveService interactiveService, final AuraFileService fileService, final NeodymiumAuraManager manager)
    {
        this.interactiveService = interactiveService;
        this.fileService = fileService;
        this.manager = manager;
    }

    public void handleDashboard(final HttpExchange exchange) throws IOException
    {
        final Context context = new Context();
        context.setVariable("theme", interactiveService.getActiveTheme());
        context.setVariable("files", fileService.getYamlFilesList());
        context.setVariable("expandedFiles", fileService.getExpandedFiles());
        final String html = manager.getTemplateEngine().process("dashboard", context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleApiTheme(final HttpExchange exchange) throws IOException
    {
        final String query = exchange.getRequestURI().getQuery();
        String theme = "system";
        if (query != null)
        {
            for (final String param : query.split("&"))
            {
                final String[] pair = param.split("=");
                if (pair.length > 1 && "theme".equals(pair[0]))
                {
                    theme = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                }
            }
        }
        interactiveService.setActiveTheme(theme);

        final Context context = new Context();
        context.setVariable("theme", theme);
        final String fragment = manager.getTemplateEngine().process("dashboard", Set.of("themeContainer"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", fragment.getBytes(StandardCharsets.UTF_8));
    }

    public void handleDashboardAssets(final HttpExchange exchange) throws IOException
    {
        final String path = exchange.getRequestURI().getPath();
        final InputStream is = getClass().getClassLoader()
                .getResourceAsStream("com/xceptance/neodymium/aura" + path);
        if (is == null)
        {
            AuraHttpUtils.sendError(exchange, 404, "Not found");
            return;
        }
        final String contentType = path.endsWith(".css") ? "text/css" : "application/javascript";
        AuraHttpUtils.sendResponse(exchange, 200, contentType, is.readAllBytes());
    }
}

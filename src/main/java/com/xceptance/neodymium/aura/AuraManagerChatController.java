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
import com.xceptance.neodymium.aura.dto.ChatRequest;
import com.xceptance.neodymium.aura.dto.ChatResponse;
import com.xceptance.neodymium.util.Neodymium;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller handling chat requests, verifying configuration, and invoking the AI reasoning workflows.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerChatController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraManagerChatController.class);

    private final AuraChatService chatService;

    public AuraManagerChatController(final AuraChatService chatService)
    {
        this.chatService = chatService;
    }

    public void handleChat(final HttpExchange exchange) throws IOException
    {
        final String body = AuraHttpUtils.readBody(exchange);
        final ChatRequest req = AuraHttpUtils.gson.fromJson(body, ChatRequest.class);
        if (req == null || req.prompt == null || req.prompt.trim().isEmpty())
        {
            LOGGER.error("[Aura Server] Chat request failed: Missing 'prompt' in body");
            AuraHttpUtils.sendError(exchange, 400, "Missing 'prompt' in body");
            return;
        }

        try
        {
            final String apiKey = Neodymium.aiConfiguration().aiApiKey();
            if (apiKey == null || apiKey.trim().isEmpty())
            {
                AuraHttpUtils.sendError(exchange, 400,
                        "API Key is missing or invalid. Please configure 'neodymium.ai.apiKey' in properties or system environment.");
                return;
            }

            final ChatResponse response = chatService.runChatWorkflow(req);
            AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(response));
        }
        catch (final AssertionError | Exception e)
        {
            LOGGER.error("[Aura Server] Chat execution failed", e);
            AuraHttpUtils.sendError(exchange, 500, "LLM Client Error: " + e.getMessage()
                    + ". Please verify that your Gemini API key is configured and valid.");
        }
    }
}

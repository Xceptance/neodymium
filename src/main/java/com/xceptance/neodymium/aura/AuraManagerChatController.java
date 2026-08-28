/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * Loved by AI developers and functional testers alike.
 */
package com.xceptance.neodymium.aura;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.Context;

import com.sun.net.httpserver.HttpExchange;
import com.xceptance.neodymium.aura.dto.ChatMessageDto;
import com.xceptance.neodymium.aura.dto.ChatRequest;
import com.xceptance.neodymium.aura.dto.ChatResponse;
import com.xceptance.neodymium.aura.dto.ChatSessionDto;
import org.neodymium.util.Neodymium;

/**
 * Controller handling chat operations, managing active chat session states,
 * and returning Thymeleaf HTML fragments for the chat sidebar.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerChatController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraManagerChatController.class);

    private final AuraChatService chatService;
    private final AuraChatSessionService sessionService;
    private final AuraManagerQueueController queueController;
    private final NeodymiumAuraManager manager;

    public AuraManagerChatController(final AuraChatService chatService, final AuraChatSessionService sessionService,
            final NeodymiumAuraManager manager)
    {
        this(chatService, sessionService, null, manager);
    }

    public AuraManagerChatController(final AuraChatService chatService, final AuraChatSessionService sessionService,
            final AuraManagerQueueController queueController, final NeodymiumAuraManager manager)
    {
        this.chatService = chatService;
        this.sessionService = sessionService;
        this.queueController = queueController;
        this.manager = manager;
    }

    /**
     * GET /api/chat/messages?id=...
     * Renders only the chat messages list for a session.
     */
    public void handleGetMessages(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String sessionId = params.get("sessionId"); // From select name=sessionId

        final ChatSessionDto session = sessionService.getSession(sessionId);

        final Context context = new Context();
        context.setVariable("chatMessages", session.messages);
        context.setVariable("currentSessionId", session.id);

        final String html = manager.getTemplateEngine().process("fragments/chat", Set.of("chatMessagesContent"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * POST /api/chat/create
     * Creates a new session and returns the full chat container fragment.
     */
    public void handleCreateSession(final HttpExchange exchange) throws IOException
    {
        final ChatSessionDto newSession = sessionService.createSession("New Chat");

        final List<ChatSessionDto> sessions = sessionService.getSessions();
        final Context context = new Context();
        context.setVariable("chatSessions", sessions);
        context.setVariable("currentSessionId", newSession.id);
        context.setVariable("chatMessages", newSession.messages);

        final String html = manager.getTemplateEngine().process("fragments/chat", Set.of("chatContainerContent"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * POST /api/chat/delete?id=...
     * Deletes a session and returns the full chat container fragment.
     */
    public void handleDeleteSession(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String id = params.getOrDefault("id", params.get("sessionId"));

        sessionService.deleteSession(id);

        final List<ChatSessionDto> sessions = sessionService.getSessions();
        final ChatSessionDto fallbackSession = sessions.get(0);

        final Context context = new Context();
        context.setVariable("chatSessions", sessions);
        context.setVariable("currentSessionId", fallbackSession.id);
        context.setVariable("chatMessages", fallbackSession.messages);

        final String html = manager.getTemplateEngine().process("fragments/chat", Set.of("chatContainerContent"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * POST /api/chat/rename?id=...&amp;name=...
     * Renames a session and returns the full chat container fragment.
     */
    public void handleRenameSession(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String id = params.getOrDefault("id", params.get("sessionId"));
        String name = params.get("name");
        if (name == null || name.trim().isEmpty())
        {
            name = exchange.getRequestHeaders().getFirst("HX-Prompt");
        }
        if (name == null || name.trim().isEmpty())
        {
            name = "Renamed Chat";
        }

        final ChatSessionDto renamed = sessionService.renameSession(id, name);

        final List<ChatSessionDto> sessions = sessionService.getSessions();
        final Context context = new Context();
        context.setVariable("chatSessions", sessions);
        context.setVariable("currentSessionId", renamed.id);
        context.setVariable("chatMessages", renamed.messages);

        final String html = manager.getTemplateEngine().process("fragments/chat", Set.of("chatContainerContent"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * POST /api/chat
     * Handles sending a message to a session, executing the AI reasoner,
     * appending the response, and returning the updated messages list fragment.
     */
    public void handleChat(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String sessionId = params.get("sessionId");
        final String prompt = params.get("prompt");

        final ChatSessionDto session = sessionService.getSession(sessionId);

        if (prompt == null || prompt.trim().isEmpty())
        {
            LOGGER.error("[Aura Server] Chat request failed: Missing prompt");
            AuraHttpUtils.sendError(exchange, 400, "Missing 'prompt' parameter");
            return;
        }

        // 1. Save user prompt
        final ChatMessageDto userMsg = new ChatMessageDto("user", prompt);
        sessionService.addMessage(session.id, userMsg);

        // 2. Auto-rename session if it's currently generic
        if ("New Chat".equalsIgnoreCase(session.name) || "Default Session".equalsIgnoreCase(session.name)
                || session.messages.size() <= 1)
        {
            final String newName = prompt.length() > 25 ? prompt.substring(0, 25) + "..." : prompt;
            sessionService.renameSession(session.id, newName);
        }

        try
        {
            final String apiKey = Neodymium.aiConfiguration().aiApiKey();
            if (apiKey == null || apiKey.trim().isEmpty())
            {
                LOGGER.error("[Aura Server] Chat request failed: API Key is missing or invalid");
                AuraHttpUtils.sendError(exchange, 400,
                        "API Key is missing or invalid. Please configure 'neodymium.ai.apiKey' in properties or system environment.");
                return;
            }

            // Clone the history list up to but excluding the newly appended user prompt for the request body
            final int historyEnd = Math.max(0, session.messages.size() - 1);
            final List<ChatMessageDto> history = session.messages.subList(0, historyEnd);
            final ChatRequest req = new ChatRequest(prompt, null, history);

            // Run visual/logical workflow classification via Gemini
            final ChatResponse response = chatService.runChatWorkflow(req);

            // Save assistant answer and deep thinking log to session
            final ChatMessageDto assistantMsg = new ChatMessageDto("ai", response.message, response.thinking);
            sessionService.addMessage(session.id, assistantMsg);

            // Apply browser profile selection if returned by AI
            if (response.selectedBrowserProfiles != null && queueController != null)
            {
                queueController.setGlobalBrowserProfiles(response.selectedBrowserProfiles);
            }

            // Set HX-Trigger header if an action needs to execute client-side
            if (response.action != null && !response.action.trim().isEmpty())
            {
                final Map<String, Object> triggerPayload = new HashMap<>();
                triggerPayload.put("action", response.action);
                triggerPayload.put("selectedDatasets", response.selectedDatasets);
                triggerPayload.put("selectedBrowserProfiles", response.selectedBrowserProfiles);
                triggerPayload.put("files", response.files);
                triggerPayload.put("filename", response.filename);
                triggerPayload.put("content", response.content);

                final String triggerJson = AuraHttpUtils.gson.toJson(Map.of("aiAction", triggerPayload));
                exchange.getResponseHeaders().set("HX-Trigger", triggerJson);
            }
        }
        catch (final AssertionError | Exception e)
        {
            LOGGER.error("[Aura Server] Chat execution failed", e);
            AuraHttpUtils.sendError(exchange, 500, "LLM Client Error: " + e.getMessage()
                    + ". Please verify that your Gemini API key is configured and valid.");
            return;
        }

        // Return updated message logs fragment
        final Context context = new Context();
        context.setVariable("chatMessages", session.messages);
        context.setVariable("currentSessionId", session.id);

        final String html = manager.getTemplateEngine().process("fragments/chat", Set.of("chatMessagesContent"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }
}

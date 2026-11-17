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
package com.xceptance.aura.test.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.aura.chat.dto.ChatMessageDto;
import com.xceptance.aura.chat.dto.ChatRequest;
import com.xceptance.aura.chat.dto.ChatResponse;
import com.xceptance.aura.chat.dto.ChatSessionDto;
import com.xceptance.aura.chat.service.AuraChatService;
import com.xceptance.aura.chat.service.AuraChatSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for managing AI chat sessions and processing chat requests.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Controller
@RequestMapping("/api/chat")
public class AuraTestChatController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraTestChatController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AuraChatService chatService;
    private final AuraChatSessionService chatSessionService;

    public AuraTestChatController(final AuraChatService chatService, final AuraChatSessionService chatSessionService)
    {
        this.chatService = chatService;
        this.chatSessionService = chatSessionService;
    }

    @GetMapping("/messages")
    public String getMessages(@RequestParam(value = "sessionId", required = false) final String sessionId, final Model model)
    {
        final String trimmedSessionId = sessionId != null ? sessionId.trim() : null;
        final ChatSessionDto session = this.chatSessionService.getSession(trimmedSessionId);
        if (trimmedSessionId != null && !trimmedSessionId.isBlank()
                && (session == null || !trimmedSessionId.equals(session.id)))
        {
            model.addAttribute("chatSessions", this.chatSessionService.getSessions());
            model.addAttribute("oobSessionSelect", true);
        }
        return populateMessagesFragment(session, model);
    }

    @PostMapping({"/create", "/sessions"})
    public String createSession(final Model model)
    {
        final ChatSessionDto newSession = chatSessionService.createSession("New Chat");
        final List<ChatSessionDto> sessions = chatSessionService.getSessions();
        model.addAttribute("chatSessions", sessions);
        model.addAttribute("currentSessionId", newSession != null ? newSession.id : "");
        model.addAttribute("chatMessages", newSession != null ? newSession.messages : List.of());
        model.addAttribute("pendingConfirmations",
                newSession != null ? this.chatService.getPendingConfirmations(newSession.id) : List.of());
        return "fragments/chat :: chatContainerContent";
    }

    @PostMapping("/delete")
    public String deleteSession(@RequestParam(value = "id", required = false) final String idParam,
                                @RequestParam(value = "sessionId", required = false) final String sessionIdParam,
                                final Model model)
    {
        final String id = resolveTrimmedId(idParam, sessionIdParam);
        chatSessionService.deleteSession(id);
        this.chatService.clearPendingConfirmations(id);

        final List<ChatSessionDto> sessions = chatSessionService.getSessions();
        final ChatSessionDto fallback = !sessions.isEmpty() ? sessions.get(0) : null;

        model.addAttribute("chatSessions", sessions);
        model.addAttribute("currentSessionId", fallback != null ? fallback.id : "");
        model.addAttribute("chatMessages", fallback != null ? fallback.messages : List.of());
        model.addAttribute("pendingConfirmations",
                fallback != null ? this.chatService.getPendingConfirmations(fallback.id) : List.of());
        return "fragments/chat :: chatContainerContent";
    }

    @PostMapping("/rename")
    public String renameSession(@RequestParam(value = "id", required = false) final String idParam,
                                @RequestParam(value = "sessionId", required = false) final String sessionIdParam,
                                @RequestParam(value = "name", required = false) final String nameParam,
                                final HttpServletRequest request,
                                final Model model)
    {
        final String id = resolveTrimmedId(idParam, sessionIdParam);
        String name = nameParam;
        if (name == null || name.isBlank())
        {
            name = request.getHeader("HX-Prompt");
        }
        if (name == null || name.isBlank())
        {
            name = "Renamed Chat";
        }

        final ChatSessionDto renamed = chatSessionService.renameSession(id, name);
        final List<ChatSessionDto> sessions = chatSessionService.getSessions();
        final ChatSessionDto activeSession = renamed != null
                ? renamed
                : (!sessions.isEmpty() ? sessions.get(0) : null);

        model.addAttribute("chatSessions", sessions);
        model.addAttribute("currentSessionId", activeSession != null ? activeSession.id : "");
        model.addAttribute("chatMessages", activeSession != null ? activeSession.messages : List.of());
        model.addAttribute("pendingConfirmations",
                activeSession != null ? this.chatService.getPendingConfirmations(activeSession.id) : List.of());
        return "fragments/chat :: chatContainerContent";
    }

    @PostMapping
    public String handleChat(@RequestParam(value = "sessionId", required = false) final String sessionId,
                             @RequestParam(value = "prompt", required = false) final String prompt,
                             @RequestParam(value = "activeFile", required = false) final String activeFile,
                             @RequestParam(value = "viewContext", required = false) final String viewContext,
                             final HttpServletResponse response,
                             final Model model)
    {
        final boolean hasExplicitSessionId = sessionId != null && !sessionId.isBlank();
        final ChatSessionDto session;
        boolean shouldRefreshSessionSelect = false;

        if (hasExplicitSessionId)
        {
            final Optional<ChatSessionDto> found = this.chatSessionService.findSession(sessionId.trim());
            if (found.isPresent())
            {
                session = found.get();
            }
            else if (prompt != null && !prompt.isBlank())
            {
                session = this.chatSessionService.createSession("New Chat");
                shouldRefreshSessionSelect = session != null;
            }
            else
            {
                session = this.chatSessionService.getSession(null);
                shouldRefreshSessionSelect = true;
            }
        }
        else
        {
            session = this.chatSessionService.getSession(null);
        }

        if (session != null && prompt != null && !prompt.isBlank())
        {
            final List<ChatMessageDto> historyBeforeTurn = session.messages != null
                    ? List.copyOf(session.messages)
                    : List.of();

            // 1. Save user prompt
            final ChatMessageDto userMsg = new ChatMessageDto("user", prompt);
            this.chatSessionService.addMessage(session.id, userMsg);
            shouldRefreshSessionSelect = true;

            // 2. Auto-rename session only if it still has a default placeholder name
            if ("New Chat".equalsIgnoreCase(session.name) || "Default Session".equalsIgnoreCase(session.name))
            {
                final String trimmedPrompt = prompt.trim();
                final String newName = trimmedPrompt.length() > 25
                        ? trimmedPrompt.substring(0, 25) + "..."
                        : trimmedPrompt;
                this.chatSessionService.renameSession(session.id, newName);
            }

            try
            {
                final String trimmedActiveFile = (activeFile != null && !activeFile.isBlank())
                        ? activeFile.trim()
                        : null;
                final String trimmedViewContext = (viewContext != null && !viewContext.isBlank())
                        ? viewContext.trim()
                        : null;
                final ChatRequest req = new ChatRequest(prompt, trimmedActiveFile, historyBeforeTurn, session.id, trimmedViewContext);

                final ChatResponse chatResp = this.chatService.runChatWorkflow(req);
                final boolean isError = "error".equals(chatResp.action);

                final ChatMessageDto assistantMsg = new ChatMessageDto("ai", chatResp.message, chatResp.thinking, isError);
                this.chatSessionService.addMessage(session.id, assistantMsg);

                if (hasTriggerActions(chatResp, isError))
                {
                    emitAiTriggerHeader(response, chatResp, isError);
                }
            }
            catch (final Exception e)
            {
                LOGGER.error("Chat execution error", e);
                this.chatSessionService.addMessage(
                        session.id,
                        new ChatMessageDto("ai", "Error processing request: " + e.getMessage(), "", true));
            }

            final ChatSessionDto effectiveSession = this.chatSessionService.findSession(session.id).orElse(session);
            if (shouldRefreshSessionSelect)
            {
                model.addAttribute("chatSessions", this.chatSessionService.getSessions());
                model.addAttribute("oobSessionSelect", true);
            }
            return populateMessagesFragment(effectiveSession, model);
        }

        if (shouldRefreshSessionSelect)
        {
            model.addAttribute("chatSessions", this.chatSessionService.getSessions());
            model.addAttribute("oobSessionSelect", true);
        }
        return populateMessagesFragment(session, model);
    }

    /**
     * Executes an explicitly confirmed pending destructive action initiated via the UI Confirm button.
     *
     * @param sessionId active chat session identifier
     * @param actionId server-assigned confirmation action identifier
     * @param response HTTP servlet response used to emit optional {@code HX-Trigger} headers
     * @param model Spring MVC model populated for fragment rendering
     * @return rendered chat messages fragment
     */
    @PostMapping("/confirm")
    public String confirmPendingAction(@RequestParam(value = "sessionId", required = false) final String sessionId,
                                       @RequestParam(value = "actionId", required = false) final String actionId,
                                       final HttpServletResponse response,
                                       final Model model)
    {
        final Optional<ChatSessionDto> found = (sessionId != null && !sessionId.isBlank())
                ? this.chatSessionService.findSession(sessionId.trim())
                : Optional.empty();
        if (found.isEmpty())
        {
            return renderMissingSessionActionFragment(model);
        }

        final ChatSessionDto session = found.get();
        if (actionId != null && !actionId.isBlank())
        {
            final ChatResponse chatResp = this.chatService.executePendingConfirmation(session.id, actionId);
            final boolean isError = "error".equals(chatResp.action);
            final boolean hasExecutedToolTrace = chatResp.thinking != null && chatResp.thinking.contains("[Tool Call]");
            this.chatSessionService.addMessage(
                    session.id,
                    new ChatMessageDto("ai", chatResp.message, chatResp.thinking, isError && !hasExecutedToolTrace));

            if (hasTriggerActions(chatResp, isError))
            {
                emitAiTriggerHeader(response, chatResp, isError);
            }
        }
        final ChatSessionDto refreshed = this.chatSessionService.findSession(session.id).orElse(session);
        model.addAttribute("chatSessions", this.chatSessionService.getSessions());
        model.addAttribute("oobSessionSelect", true);
        return populateMessagesFragment(refreshed, model);
    }

    /**
     * Cancels a pending destructive action explicitly dismissed via the UI Cancel button.
     *
     * @param sessionId active chat session identifier
     * @param actionId server-assigned confirmation action identifier
     * @param model Spring MVC model populated for fragment rendering
     * @return rendered chat messages fragment
     */
    @PostMapping("/cancel")
    public String cancelPendingAction(@RequestParam(value = "sessionId", required = false) final String sessionId,
                                      @RequestParam(value = "actionId", required = false) final String actionId,
                                      final Model model)
    {
        final Optional<ChatSessionDto> found = (sessionId != null && !sessionId.isBlank())
                ? this.chatSessionService.findSession(sessionId.trim())
                : Optional.empty();
        if (found.isEmpty())
        {
            return renderMissingSessionActionFragment(model);
        }

        final ChatSessionDto session = found.get();
        if (actionId != null && !actionId.isBlank())
        {
            final ChatResponse chatResp = this.chatService.cancelPendingConfirmation(session.id, actionId);
            final boolean isError = "error".equals(chatResp.action);
            this.chatSessionService.addMessage(
                    session.id,
                    new ChatMessageDto("ai", chatResp.message, chatResp.thinking, isError));
        }
        final ChatSessionDto refreshed = this.chatSessionService.findSession(session.id).orElse(session);
        model.addAttribute("chatSessions", this.chatSessionService.getSessions());
        model.addAttribute("oobSessionSelect", true);
        return populateMessagesFragment(refreshed, model);
    }

    private String renderMissingSessionActionFragment(final Model model)
    {
        final ChatSessionDto fallback = this.chatSessionService.getSession(null);
        final List<ChatMessageDto> transientMessages = new ArrayList<>();
        if (fallback != null && fallback.messages != null)
        {
            transientMessages.addAll(fallback.messages);
        }
        transientMessages.add(new ChatMessageDto(
                "ai",
                "Chat session was not found or was deleted. No action was executed.",
                "",
                true));

        model.addAttribute("chatSessions", this.chatSessionService.getSessions());
        model.addAttribute("oobSessionSelect", true);
        model.addAttribute("chatMessages", List.copyOf(transientMessages));
        model.addAttribute("currentSessionId", fallback != null ? fallback.id : "");
        model.addAttribute("pendingConfirmations",
                fallback != null ? this.chatService.getPendingConfirmations(fallback.id) : List.of());
        return "fragments/chat :: chatMessagesContent";
    }

    private String populateMessagesFragment(final ChatSessionDto session, final Model model)
    {
        model.addAttribute("chatMessages", session != null && session.messages != null ? session.messages : List.of());
        model.addAttribute("currentSessionId", session != null ? session.id : "");
        model.addAttribute("pendingConfirmations",
                session != null ? this.chatService.getPendingConfirmations(session.id) : List.of());
        return "fragments/chat :: chatMessagesContent";
    }

    private static String resolveTrimmedId(final String primaryId, final String fallbackId)
    {
        final String candidate = (primaryId != null && !primaryId.isBlank()) ? primaryId : fallbackId;
        return (candidate != null && !candidate.isBlank()) ? candidate.trim() : null;
    }

    private static boolean hasTriggerActions(final ChatResponse chatResp, final boolean isError)
    {
        if (chatResp.actions != null && !chatResp.actions.isEmpty())
        {
            return true;
        }
        return !isError && chatResp.action != null && !chatResp.action.isBlank();
    }

    private static void emitAiTriggerHeader(final HttpServletResponse response, final ChatResponse chatResp,
                                            final boolean isError)
    {
        final Map<String, Object> triggerPayload = new HashMap<>();
        if (isError && chatResp.actions != null && !chatResp.actions.isEmpty())
        {
            triggerPayload.put("action", chatResp.actions.get(0));
        }
        else
        {
            triggerPayload.put("action", chatResp.action);
        }
        if (chatResp.actions != null && !chatResp.actions.isEmpty())
        {
            triggerPayload.put("actions", chatResp.actions);
        }
        try
        {
            final String triggerJson = OBJECT_MAPPER.writeValueAsString(Map.of("aiAction", triggerPayload));
            response.setHeader("HX-Trigger", triggerJson);
        }
        catch (final JsonProcessingException e)
        {
            LOGGER.warn("Failed to serialize HX-Trigger payload: {}", e.getMessage());
        }
    }

    /**
     * Rejects malformed or unsafe session identifiers with HTTP 400 Bad Request instead of 500.
     *
     * @param ex validation exception raised by the session service
     * @return HTTP 400 response containing the validation error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidSessionId(final IllegalArgumentException ex)
    {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}

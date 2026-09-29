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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.neodymium.aura.AuraChatService;
import com.xceptance.neodymium.aura.AuraChatSessionService;
import com.xceptance.neodymium.aura.dto.ChatMessageDto;
import com.xceptance.neodymium.aura.dto.ChatRequest;
import com.xceptance.neodymium.aura.dto.ChatResponse;
import com.xceptance.neodymium.aura.dto.ChatSessionDto;
import com.xceptance.neodymium.util.Neodymium;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    private final AuraChatService chatService;
    private final AuraChatSessionService chatSessionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuraTestChatController(final AuraChatService chatService, final AuraChatSessionService chatSessionService)
    {
        this.chatService = chatService;
        this.chatSessionService = chatSessionService;
    }

    @GetMapping("/messages")
    public String getMessages(@RequestParam(value = "sessionId", required = false) final String sessionId, final Model model)
    {
        final ChatSessionDto session = chatSessionService.getSession(sessionId);
        model.addAttribute("chatMessages", session != null ? session.messages : List.of());
        model.addAttribute("currentSessionId", session != null ? session.id : "");
        return "fragments/chat :: chatMessagesContent";
    }

    @PostMapping({"/create", "/sessions"})
    public String createSession(final Model model)
    {
        final ChatSessionDto newSession = chatSessionService.createSession("New Chat");
        final List<ChatSessionDto> sessions = chatSessionService.getSessions();
        model.addAttribute("chatSessions", sessions);
        model.addAttribute("currentSessionId", newSession != null ? newSession.id : "");
        model.addAttribute("chatMessages", newSession != null ? newSession.messages : List.of());
        return "fragments/chat :: chatContainerContent";
    }

    @PostMapping("/delete")
    public String deleteSession(@RequestParam(value = "id", required = false) final String idParam,
                                @RequestParam(value = "sessionId", required = false) final String sessionIdParam,
                                final Model model)
    {
        final String id = idParam != null && !idParam.isBlank() ? idParam : sessionIdParam;
        chatSessionService.deleteSession(id);

        final List<ChatSessionDto> sessions = chatSessionService.getSessions();
        final ChatSessionDto fallback = !sessions.isEmpty() ? sessions.get(0) : null;

        model.addAttribute("chatSessions", sessions);
        model.addAttribute("currentSessionId", fallback != null ? fallback.id : "");
        model.addAttribute("chatMessages", fallback != null ? fallback.messages : List.of());
        return "fragments/chat :: chatContainerContent";
    }

    @PostMapping("/rename")
    public String renameSession(@RequestParam(value = "id", required = false) final String idParam,
                                @RequestParam(value = "sessionId", required = false) final String sessionIdParam,
                                @RequestParam(value = "name", required = false) final String nameParam,
                                final HttpServletRequest request,
                                final Model model)
    {
        final String id = idParam != null && !idParam.isBlank() ? idParam : sessionIdParam;
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

        model.addAttribute("chatSessions", sessions);
        model.addAttribute("currentSessionId", renamed != null ? renamed.id : "");
        model.addAttribute("chatMessages", renamed != null ? renamed.messages : List.of());
        return "fragments/chat :: chatContainerContent";
    }

    @PostMapping
    public String handleChat(@RequestParam(value = "sessionId", required = false) final String sessionId,
                             @RequestParam(value = "prompt", required = false) final String prompt,
                             final HttpServletResponse response,
                             final Model model)
    {
        final ChatSessionDto session = chatSessionService.getSession(sessionId);

        if (session != null && prompt != null && !prompt.isBlank())
        {
            // 1. Save user prompt
            final ChatMessageDto userMsg = new ChatMessageDto("user", prompt);
            chatSessionService.addMessage(session.id, userMsg);

            // 2. Auto-rename session if it's currently generic
            if ("New Chat".equalsIgnoreCase(session.name) || "Default Session".equalsIgnoreCase(session.name)
                    || session.messages.size() <= 1)
            {
                final String newName = prompt.length() > 25 ? prompt.substring(0, 25) + "..." : prompt;
                chatSessionService.renameSession(session.id, newName);
            }

            try
            {
                final String apiKey = Neodymium.aiConfiguration().aiApiKey();
                if (apiKey != null && !apiKey.isBlank())
                {
                    final int historyEnd = Math.max(0, session.messages.size() - 1);
                    final List<ChatMessageDto> history = session.messages.subList(0, historyEnd);
                    final ChatRequest req = new ChatRequest(prompt, null, history);

                    final ChatResponse chatResp = chatService.runChatWorkflow(req);

                    final ChatMessageDto assistantMsg = new ChatMessageDto("ai", chatResp.message, chatResp.thinking);
                    chatSessionService.addMessage(session.id, assistantMsg);

                    if (chatResp.action != null && !chatResp.action.isBlank())
                    {
                        final Map<String, Object> triggerPayload = new HashMap<>();
                        triggerPayload.put("action", chatResp.action);
                        triggerPayload.put("selectedDatasets", chatResp.selectedDatasets);
                        triggerPayload.put("files", chatResp.files);
                        triggerPayload.put("filename", chatResp.filename);
                        triggerPayload.put("content", chatResp.content);

                        final String triggerJson = objectMapper.writeValueAsString(Map.of("aiAction", triggerPayload));
                        response.setHeader("HX-Trigger", triggerJson);
                    }
                }
                else
                {
                    chatSessionService.addMessage(session.id, new ChatMessageDto("ai",
                        "API Key is missing or invalid. Please configure 'neodymium.ai.apiKey' in properties or system environment."));
                }
            }
            catch (final Exception e)
            {
                LOGGER.error("Chat execution error", e);
                chatSessionService.addMessage(session.id, new ChatMessageDto("ai", "Error processing request: " + e.getMessage()));
            }

            model.addAttribute("chatMessages", session.messages);
            model.addAttribute("currentSessionId", session.id);
        }
        else
        {
            model.addAttribute("chatMessages", session != null ? session.messages : List.of());
            model.addAttribute("currentSessionId", session != null ? session.id : "");
        }

        return "fragments/chat :: chatMessagesContent";
    }
}

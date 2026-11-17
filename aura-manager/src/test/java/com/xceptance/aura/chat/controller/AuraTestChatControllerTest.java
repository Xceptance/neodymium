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
package com.xceptance.aura.chat.controller;

import com.xceptance.aura.chat.dto.ChatMessageDto;
import com.xceptance.aura.chat.dto.ChatRequest;
import com.xceptance.aura.chat.dto.ChatResponse;
import com.xceptance.aura.chat.dto.ChatSessionDto;
import com.xceptance.aura.chat.dto.PendingConfirmationDto;
import com.xceptance.aura.chat.service.AuraChatService;
import com.xceptance.aura.chat.service.AuraChatSessionService;
import com.xceptance.aura.test.controller.AuraTestChatController;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Unit tests for {@link AuraTestChatController}.
 *
 * @author AI-generated: Gemini 4 Hydrogen
 * @author Xceptance GmbH 2026
 */
public class AuraTestChatControllerTest
{
    private AuraChatService chatService;
    private AuraChatSessionService sessionService;
    private MockMvc mockMvc;

    @BeforeEach
    public void setUp()
    {
        chatService = Mockito.mock(AuraChatService.class);
        sessionService = Mockito.mock(AuraChatSessionService.class);
        final AuraTestChatController controller = new AuraTestChatController(chatService, sessionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testHandleChatEmitsHxTriggerAndOobSelectWhenAutoRenamed() throws Exception
    {
        final ChatSessionDto session = new ChatSessionDto("sess-1", "New Chat", new ArrayList<>());
        final ChatSessionDto renamed = new ChatSessionDto("sess-1", "Create a login test", List.of());
        Mockito.when(sessionService.findSession("sess-1")).thenReturn(Optional.of(session));
        Mockito.when(sessionService.renameSession("sess-1", "Create a login test")).thenReturn(renamed);
        Mockito.when(sessionService.getSessions()).thenReturn(List.of(renamed));
        Mockito.when(chatService.runChatWorkflow(Mockito.any(ChatRequest.class)))
                .thenReturn(new ChatResponse(
                        "Created login test.",
                        "[Tool Call] create_test",
                        "refresh_files",
                        List.of("refresh_files")
                ));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat")
                        .param("sessionId", "sess-1")
                        .param("prompt", "Create a login test")
                        .param("activeFile", "tests/existing.yaml"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("fragments/chat :: chatMessagesContent"))
                .andExpect(MockMvcResultMatchers.model().attribute("oobSessionSelect", true))
                .andExpect(MockMvcResultMatchers.header().string("HX-Trigger",
                        Matchers.allOf(
                                Matchers.containsString("aiAction"),
                                Matchers.containsString("refresh_files")
                        )));

        Mockito.verify(sessionService, Mockito.times(2))
                .addMessage(Mockito.eq("sess-1"), Mockito.any(ChatMessageDto.class));
        Mockito.verify(sessionService).renameSession("sess-1", "Create a login test");

        final ArgumentCaptor<ChatRequest> reqCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        Mockito.verify(chatService).runChatWorkflow(reqCaptor.capture());
        Assertions.assertEquals("sess-1", reqCaptor.getValue().sessionId);
        Assertions.assertEquals("tests/existing.yaml", reqCaptor.getValue().activeFile);
    }

    @Test
    public void testCustomNamedEmptySessionIsNotAutoRenamedOnFirstPrompt() throws Exception
    {
        final ChatSessionDto customEmpty = new ChatSessionDto("sess-custom", "Smoke Investigation", List.of());
        Mockito.when(sessionService.findSession("sess-custom")).thenReturn(Optional.of(customEmpty));
        Mockito.when(sessionService.getSessions()).thenReturn(List.of(customEmpty));
        Mockito.when(chatService.runChatWorkflow(Mockito.any(ChatRequest.class)))
                .thenReturn(new ChatResponse("Found 2 tests.", "", null, null));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat")
                        .param("sessionId", "sess-custom")
                        .param("prompt", "List all tests"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute("oobSessionSelect", true));

        Mockito.verify(sessionService, Mockito.never()).renameSession(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testHandleChatDoesNotEmitHxTriggerOnErrorAction() throws Exception
    {
        final ChatSessionDto session = new ChatSessionDto("sess-2", "Existing", new ArrayList<>(
                List.of(new ChatMessageDto("user", "old"), new ChatMessageDto("ai", "old reply"))
        ));
        Mockito.when(sessionService.findSession("sess-2")).thenReturn(Optional.of(session));
        Mockito.when(chatService.runChatWorkflow(Mockito.any(ChatRequest.class)))
                .thenReturn(new ChatResponse("API Key is missing.", "", "error", null));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat")
                        .param("sessionId", "sess-2")
                        .param("prompt", "List tests"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().doesNotExist("HX-Trigger"));

        final ArgumentCaptor<ChatMessageDto> savedMsgCaptor = ArgumentCaptor.forClass(ChatMessageDto.class);
        Mockito.verify(sessionService, Mockito.times(2)).addMessage(Mockito.eq("sess-2"), savedMsgCaptor.capture());
        Assertions.assertTrue(savedMsgCaptor.getAllValues().get(1).error);
    }

    @Test
    public void testCreateAndRenameSession() throws Exception
    {
        final ChatSessionDto created = new ChatSessionDto("sess-3", "New Chat", new ArrayList<>());
        Mockito.when(sessionService.createSession("New Chat")).thenReturn(created);
        Mockito.when(sessionService.getSessions()).thenReturn(List.of(created));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/create"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("fragments/chat :: chatContainerContent"))
                .andExpect(MockMvcResultMatchers.model().attribute("currentSessionId", "sess-3"));

        final ChatSessionDto renamed = new ChatSessionDto("sess-3", "Smoke Tests", new ArrayList<>());
        Mockito.when(sessionService.renameSession("sess-3", "Smoke Tests")).thenReturn(renamed);
        Mockito.when(sessionService.getSessions()).thenReturn(List.of(renamed));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/rename")
                        .param("id", "sess-3")
                        .header("HX-Prompt", "Smoke Tests"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("fragments/chat :: chatContainerContent"));

        Mockito.verify(sessionService).renameSession("sess-3", "Smoke Tests");
    }

    @Test
    public void testChatTemplateEscapesContentAndChatJsUsesInnerHtmlSwap() throws Exception
    {
        final Path chatTemplate = Paths.get("src/main/resources/templates/fragments/chat.html");
        final String templateContent = Files.readString(chatTemplate, StandardCharsets.UTF_8);
        Assertions.assertFalse(templateContent.contains("th:utext"),
                "fragments/chat.html must not render unsanitized message content via th:utext");
        Assertions.assertTrue(templateContent.contains("th:text=\"${msg.content}\""));
        Assertions.assertTrue(templateContent.contains("id=\"hiddenActiveFile\""));

        final Path chatJs = Paths.get("src/main/resources/static/dashboard-chat.js");
        final String jsContent = Files.readString(chatJs, StandardCharsets.UTF_8);
        Assertions.assertFalse(jsContent.contains("swap: 'outerHTML'"),
                "dashboard-chat.js must swap #chatContainer with innerHTML to preserve container wrapper");
        Assertions.assertTrue(jsContent.contains("swap: 'innerHTML'"));
    }

    @Test
    public void testInvalidSessionIdReturnsBadRequest() throws Exception
    {
        Mockito.when(sessionService.getSession("../evil"))
                .thenThrow(new IllegalArgumentException("Invalid chat session identifier."));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/chat/messages")
                        .param("sessionId", "../evil"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().string("Invalid chat session identifier."));
    }

    @Test
    public void testHandleChatCreatesFreshSessionInsteadOfMutatingDefaultWhenExplicitSessionMissing() throws Exception
    {
        final ChatSessionDto created = new ChatSessionDto("sess-fresh", "New Chat", new ArrayList<>());
        Mockito.when(sessionService.findSession("deleted-id")).thenReturn(Optional.empty());
        Mockito.when(sessionService.createSession("New Chat")).thenReturn(created);
        Mockito.when(sessionService.renameSession("sess-fresh", "List tests"))
                .thenReturn(new ChatSessionDto("sess-fresh", "List tests", List.of()));
        Mockito.when(sessionService.getSessions())
                .thenReturn(List.of(new ChatSessionDto("sess-fresh", "List tests", List.of())));
        Mockito.when(chatService.runChatWorkflow(Mockito.any(ChatRequest.class)))
                .thenReturn(new ChatResponse("Here are the tests.", ""));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat")
                        .param("sessionId", "deleted-id")
                        .param("prompt", "List tests"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute("currentSessionId", "sess-fresh"))
                .andExpect(MockMvcResultMatchers.model().attribute("oobSessionSelect", true));

        Mockito.verify(sessionService, Mockito.never()).getSession("deleted-id");
        Mockito.verify(sessionService).createSession("New Chat");
        Mockito.verify(sessionService, Mockito.times(2))
                .addMessage(Mockito.eq("sess-fresh"), Mockito.any(ChatMessageDto.class));
    }

    @Test
    public void testChatTemplateIncludesOobHiddenSessionIdAndSafeSubmitGuards() throws Exception
    {
        final Path chatTemplate = Paths.get("src/main/resources/templates/fragments/chat.html");
        final String templateContent = Files.readString(chatTemplate, StandardCharsets.UTF_8);
        Assertions.assertTrue(templateContent.contains("id=\"hiddenSessionId\"")
                        && templateContent.contains("hx-swap-oob=\"outerHTML\""),
                "chatMessagesContent must sync #hiddenSessionId via OOB swap");
        Assertions.assertTrue(templateContent.contains("pendingConfirmations"),
                "chat.html must support rendering explicit Confirm/Cancel buttons for pending confirmations");

        final Path chatJs = Paths.get("src/main/resources/static/dashboard-chat.js");
        final String jsContent = Files.readString(chatJs, StandardCharsets.UTF_8);
        Assertions.assertTrue(jsContent.contains("preventDefault"),
                "dashboard-chat.js must cancel empty chat form submissions");
    }

    @Test
    public void testConfirmAndCancelEndpointsExecuteOrClearPendingConfirmation() throws Exception
    {
        final ChatSessionDto session = new ChatSessionDto("sess-btn", "Danger Ops", new ArrayList<>());
        Mockito.when(sessionService.findSession("sess-btn")).thenReturn(Optional.of(session));
        Mockito.when(sessionService.getSessions()).thenReturn(List.of(session));
        Mockito.when(chatService.executePendingConfirmation("sess-btn", "act-1"))
                .thenReturn(new ChatResponse(
                        "Confirmed and executed `delete_test`.",
                        "[Tool Call] delete_test",
                        "refresh_files",
                        List.of("refresh_files")));
        Mockito.when(chatService.cancelPendingConfirmation("sess-btn", "act-2"))
                .thenReturn(new ChatResponse("Cancelled pending `cancel_run` on `run-1`.", ""));
        Mockito.when(chatService.getPendingConfirmations("sess-btn"))
                .thenReturn(List.of(new PendingConfirmationDto("act-2", "cancel_run", "run-1", 999999999L)))
                .thenReturn(List.of());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/confirm")
                        .param("sessionId", "sess-btn")
                        .param("actionId", "act-1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("fragments/chat :: chatMessagesContent"))
                .andExpect(MockMvcResultMatchers.model().attribute("oobSessionSelect", true))
                .andExpect(MockMvcResultMatchers.header().string("HX-Trigger",
                        Matchers.allOf(
                                Matchers.containsString("aiAction"),
                                Matchers.containsString("refresh_files")
                        )))
                .andExpect(MockMvcResultMatchers.model().attributeExists("pendingConfirmations"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/cancel")
                        .param("sessionId", "sess-btn")
                        .param("actionId", "act-2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("fragments/chat :: chatMessagesContent"))
                .andExpect(MockMvcResultMatchers.model().attribute("oobSessionSelect", true))
                .andExpect(MockMvcResultMatchers.header().doesNotExist("HX-Trigger"));

        Mockito.verify(sessionService, Mockito.times(2))
                .addMessage(Mockito.eq("sess-btn"), Mockito.any(ChatMessageDto.class));
    }

    @Test
    public void testConfirmAndCancelDoNotMutateFallbackSessionWhenExplicitSessionMissing() throws Exception
    {
        final ChatSessionDto fallback = new ChatSessionDto("sess-default", "Default Session",
                List.of(new ChatMessageDto("user", "Existing message")));
        Mockito.when(sessionService.findSession("deleted-sess")).thenReturn(Optional.empty());
        Mockito.when(sessionService.getSession(null)).thenReturn(fallback);
        Mockito.when(sessionService.getSessions()).thenReturn(List.of(fallback));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/confirm")
                        .param("sessionId", "deleted-sess")
                        .param("actionId", "act-1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("fragments/chat :: chatMessagesContent"))
                .andExpect(MockMvcResultMatchers.model().attribute("currentSessionId", "sess-default"))
                .andExpect(MockMvcResultMatchers.model().attribute("oobSessionSelect", true))
                .andExpect(MockMvcResultMatchers.model().attribute("chatMessages", Matchers.hasSize(2)))
                .andExpect(MockMvcResultMatchers.header().doesNotExist("HX-Trigger"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/cancel")
                        .param("sessionId", "deleted-sess")
                        .param("actionId", "act-1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("fragments/chat :: chatMessagesContent"))
                .andExpect(MockMvcResultMatchers.model().attribute("currentSessionId", "sess-default"))
                .andExpect(MockMvcResultMatchers.model().attribute("oobSessionSelect", true));

        Mockito.verify(chatService, Mockito.never()).executePendingConfirmation(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(chatService, Mockito.never()).cancelPendingConfirmation(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(sessionService, Mockito.never()).addMessage(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testHandleChatBlankPromptWithMissingExplicitSessionResyncsDropdown() throws Exception
    {
        final ChatSessionDto fallback = new ChatSessionDto("sess-fallback", "Default Session", List.of());
        Mockito.when(sessionService.findSession("deleted-sess")).thenReturn(Optional.empty());
        Mockito.when(sessionService.getSession(null)).thenReturn(fallback);
        Mockito.when(sessionService.getSessions()).thenReturn(List.of(fallback));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat")
                        .param("sessionId", "deleted-sess")
                        .param("prompt", "   "))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute("currentSessionId", "sess-fallback"))
                .andExpect(MockMvcResultMatchers.model().attribute("oobSessionSelect", true));

        Mockito.verify(sessionService, Mockito.never()).addMessage(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testDashboardCoreHandlesPlainTextErrorAndCleansOptimisticUserBubble() throws Exception
    {
        final Path coreJs = Paths.get("src/main/resources/static/dashboard-core.js");
        final String jsContent = Files.readString(coreJs, StandardCharsets.UTF_8);
        Assertions.assertTrue(jsContent.contains("optimistic-user-bubble"),
                "dashboard-core.js must remove orphaned optimistic user bubbles on htmx:responseError");
        Assertions.assertTrue(jsContent.contains("xhr.responseText"),
                "dashboard-core.js must surface plain-text HTTP 400 error bodies from xhr.responseText");
    }

    @Test
    public void testChatContainerDoesNotDuplicateHiddenSessionIdWhileMessagesFragmentEmitsOob()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);

        final ChatSessionDto active = new ChatSessionDto("sess-1", "Chat One", List.of());
        final Context ctx = new Context();
        ctx.setVariable("chatSessions", List.of(active));
        ctx.setVariable("currentSessionId", "sess-1");
        ctx.setVariable("chatMessages", List.of());
        ctx.setVariable("pendingConfirmations", List.of());

        final String containerHtml = engine.process("fragments/chat", Set.of("chatContainerContent"), ctx);
        final int containerMatches = countOccurrences(containerHtml, "id=\"hiddenSessionId\"");
        Assertions.assertEquals(1, containerMatches,
                "chatContainerContent must render #hiddenSessionId exactly once inside the form:\n" + containerHtml);
        final int formIdx = containerHtml.indexOf("<form");
        final int hiddenIdx = containerHtml.indexOf("id=\"hiddenSessionId\"");
        Assertions.assertTrue(formIdx >= 0 && hiddenIdx > formIdx,
                "The sole #hiddenSessionId must reside inside the chat <form>:\n" + containerHtml);

        final String messagesHtml = engine.process("fragments/chat", Set.of("chatMessagesContent"), ctx);
        Assertions.assertEquals(1, countOccurrences(messagesHtml, "id=\"hiddenSessionId\""),
                "Standalone chatMessagesContent must render one #hiddenSessionId:\n" + messagesHtml);
        Assertions.assertTrue(messagesHtml.contains("hx-swap-oob=\"outerHTML\""),
                "Standalone chatMessagesContent must emit hx-swap-oob for #hiddenSessionId");
    }

    @Test
    public void testGetMessagesResyncsDropdownWhenFallingBackFromMissingSession() throws Exception
    {
        final ChatSessionDto fallback = new ChatSessionDto("sess-default", "Default Session", List.of());
        Mockito.when(sessionService.getSession("deleted-sess")).thenReturn(fallback);
        Mockito.when(sessionService.getSessions()).thenReturn(List.of(fallback));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/chat/messages")
                        .param("sessionId", "deleted-sess"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("fragments/chat :: chatMessagesContent"))
                .andExpect(MockMvcResultMatchers.model().attribute("currentSessionId", "sess-default"))
                .andExpect(MockMvcResultMatchers.model().attribute("oobSessionSelect", true));
    }

    @Test
    public void testHandleChatEmitsHxTriggerWhenErrorFollowsSuccessfulToolAction() throws Exception
    {
        final ChatSessionDto session = new ChatSessionDto("sess-err-act", "Ops", List.of());
        Mockito.when(sessionService.findSession("sess-err-act")).thenReturn(Optional.of(session));
        Mockito.when(sessionService.getSessions()).thenReturn(List.of(session));
        Mockito.when(chatService.runChatWorkflow(Mockito.any(ChatRequest.class)))
                .thenReturn(new ChatResponse(
                        "Error processing request: Summary timeout",
                        "[Tool Call] create_test",
                        "error",
                        List.of("refresh_files")));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat")
                        .param("sessionId", "sess-err-act")
                        .param("prompt", "Create smoke.yaml"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().string("HX-Trigger",
                        Matchers.allOf(
                                Matchers.containsString("aiAction"),
                                Matchers.containsString("refresh_files"),
                                Matchers.not(Matchers.containsString("\"error\""))
                        )));
    }

    @Test
    public void testGetMessagesTrimsSessionIdDeleteEvictsPendingAndConfirmPreservesExecutedToolHistory() throws Exception
    {
        final ChatSessionDto session = new ChatSessionDto("sess-pad", "Ops", List.of());
        Mockito.when(sessionService.getSession("sess-pad")).thenReturn(session);
        Mockito.when(sessionService.findSession("sess-pad")).thenReturn(Optional.of(session));
        Mockito.when(sessionService.getSessions()).thenReturn(List.of(session));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/chat/messages")
                        .param("sessionId", " sess-pad "))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute("currentSessionId", "sess-pad"))
                .andExpect(MockMvcResultMatchers.model().attributeDoesNotExist("oobSessionSelect"));
        Mockito.verify(sessionService).getSession("sess-pad");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/delete")
                        .param("id", "sess-pad"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(chatService).clearPendingConfirmations("sess-pad");

        Mockito.when(chatService.executePendingConfirmation("sess-pad", "act-done"))
                .thenReturn(new ChatResponse(
                        "Could not execute `cancel_run` on `run-1`. Run already finished.",
                        "[Tool Call] cancel_run",
                        "error",
                        null));
        Mockito.when(chatService.executePendingConfirmation("sess-pad", "act-expired"))
                .thenReturn(new ChatResponse("Confirmation expired.", "", "error", null));
        Mockito.when(chatService.executePendingConfirmation("sess-pad", "act-exception"))
                .thenReturn(new ChatResponse(
                        "Failed to execute `cancel_run`: Transport error",
                        "[Error] Transport error",
                        "error",
                        null));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/confirm")
                        .param("sessionId", "sess-pad")
                        .param("actionId", "act-done"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/confirm")
                        .param("sessionId", "sess-pad")
                        .param("actionId", "act-expired"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/confirm")
                        .param("sessionId", "sess-pad")
                        .param("actionId", "act-exception"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        final ArgumentCaptor<ChatMessageDto> msgCaptor = ArgumentCaptor.forClass(ChatMessageDto.class);
        Mockito.verify(sessionService, Mockito.times(3)).addMessage(Mockito.eq("sess-pad"), msgCaptor.capture());
        final List<ChatMessageDto> saved = msgCaptor.getAllValues();
        Assertions.assertFalse(saved.get(0).error,
                "Executed confirmation outcome with tool trace should stay in LLM history");
        Assertions.assertTrue(saved.get(1).error,
                "Pre-execution expired confirmation failure without tool trace should remain marked error");
        Assertions.assertTrue(saved.get(2).error,
                "Pre-execution exception trace ([Error]) without [Tool Call] should remain marked error");
    }

    @Test
    public void testDeleteAndRenameSessionTrimPaddedIds() throws Exception
    {
        final ChatSessionDto fallback = new ChatSessionDto("sess-fallback", "Fallback", List.of());
        final ChatSessionDto renamed = new ChatSessionDto("sess-rename", "Updated Name", List.of());
        Mockito.when(sessionService.getSessions()).thenReturn(List.of(fallback));
        Mockito.when(sessionService.renameSession("sess-rename", "Updated Name")).thenReturn(renamed);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/delete")
                        .param("id", " sess-del "))
                .andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(sessionService).deleteSession("sess-del");
        Mockito.verify(chatService).clearPendingConfirmations("sess-del");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/rename")
                        .param("sessionId", " sess-rename ")
                        .param("name", "Updated Name"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute("currentSessionId", "sess-rename"));
        Mockito.verify(sessionService).renameSession("sess-rename", "Updated Name");
    }

    @Test
    public void testHandleChatForwardsViewContextToChatService() throws Exception
    {
        final ChatSessionDto session = new ChatSessionDto("sess-view", "Dashboard", List.of());
        Mockito.when(sessionService.findSession("sess-view")).thenReturn(Optional.of(session));
        Mockito.when(sessionService.getSessions()).thenReturn(List.of(session));
        Mockito.when(chatService.runChatWorkflow(Mockito.any(ChatRequest.class)))
                .thenReturn(new ChatResponse("Analyzed.", "", null, null));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat")
                        .param("sessionId", "sess-view")
                        .param("prompt", "Summarize this page")
                        .param("viewContext", " view=CustomDashboard; pageId=p1 "))
                .andExpect(MockMvcResultMatchers.status().isOk());

        final ArgumentCaptor<ChatRequest> reqCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        Mockito.verify(chatService).runChatWorkflow(reqCaptor.capture());
        Assertions.assertEquals("view=CustomDashboard; pageId=p1", reqCaptor.getValue().viewContext);
        Assertions.assertEquals("Summarize this page", reqCaptor.getValue().prompt);
    }

    private static int countOccurrences(final String text, final String token)
    {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(token, idx)) >= 0)
        {
            count++;
            idx += token.length();
        }
        return count;
    }
}

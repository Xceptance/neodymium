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
package com.xceptance.aura.chat.service;

import com.xceptance.aura.chat.dto.ChatMessageDto;
import com.xceptance.aura.chat.dto.ChatRequest;
import com.xceptance.aura.chat.dto.PendingConfirmationDto;
import com.xceptance.aura.mcp.tool.AuraMcpToolService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

/**
 * Unit tests for {@link AuraChatService} exercising Spring AI {@code ChatClient}
 * multi-turn tool calling with {@link AuraMcpToolCallbackProvider}.
 *
 * @author AI-generated: Gemini 4 Hydrogen
 * @author Xceptance GmbH 2026
 */
public class AuraChatServiceTest
{
    @Test
    public void testMultiTurnToolCallingAndUiActionDetection()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.createTest("tests/login.yaml", "steps: |\n  Open /login"))
                .thenReturn(Map.of("status", "SUCCESS", "filePath", "tests/login.yaml"));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);

        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            if (callCount.getAndIncrement() == 0)
            {
                final AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                        "call-1",
                        "function",
                        "create_test",
                        "{\"testPath\":\"tests/login.yaml\",\"content\":\"steps: |\\n  Open /login\"}"
                );
                final AssistantMessage toolRequestMsg = AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(toolCall))
                        .build();
                return new ChatResponse(List.of(new Generation(toolRequestMsg)));
            }

            final AssistantMessage finalMsg = new AssistantMessage("Created playbook tests/login.yaml.");
            return new ChatResponse(List.of(new Generation(finalMsg)));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final ChatRequest req = new ChatRequest(
                "Create a login test",
                "tests/existing.yaml",
                List.of(new ChatMessageDto("user", "Hi"), new ChatMessageDto("ai", "Hello!")),
                "sess-1"
        );

        final var response = chatService.runChatWorkflow(req);

        Assertions.assertEquals("Created playbook tests/login.yaml.", response.message);
        Assertions.assertTrue(response.thinking.contains("[Tool Call] create_test"));
        Assertions.assertEquals("refresh_files", response.action);
        Assertions.assertEquals(List.of("refresh_files"), response.actions);
        Mockito.verify(mcpToolService).createTest("tests/login.yaml", "steps: |\n  Open /login");

        final ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        Mockito.verify(mockChatModel, Mockito.times(2)).call(promptCaptor.capture());
        final String combinedUserText = promptCaptor.getAllValues().get(0).getInstructions().stream()
                .map(Message::getText)
                .reduce("", (a, b) -> a + "\n" + b);
        Assertions.assertTrue(combinedUserText.contains("[Active editor file: tests/existing.yaml]"));
    }

    @Test
    public void testConfirmationBarrierBlocksAutoExecutionAndRequiresUiButtonOnly()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.deleteTest("tests/danger.yaml", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-secret-1",
                        "expiresInSeconds", 60,
                        "message", "Confirm delete"))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-secret-2",
                        "expiresInSeconds", 60,
                        "message", "Confirm delete"));
        Mockito.when(mcpToolService.deleteTest("tests/danger.yaml", "tok-secret-2"))
                .thenReturn(Map.of("status", "SUCCESS", "message", "Deleted."));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);

        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            final int step = callCount.getAndIncrement();
            if (step == 0)
            {
                final AssistantMessage.ToolCall call1 = new AssistantMessage.ToolCall(
                        "c1", "function", "delete_test", "{\"testPath\":\"tests/danger.yaml\"}");
                return new ChatResponse(List.of(new Generation(
                        AssistantMessage.builder().content("").toolCalls(List.of(call1)).build())));
            }
            if (step == 1)
            {
                // LLM attempts to confirm in the same turn (must be blocked by barrier)
                final AssistantMessage.ToolCall call2 = new AssistantMessage.ToolCall(
                        "c2", "function", "delete_test",
                        "{\"testPath\":\"tests/danger.yaml\",\"confirmationToken\":\"tok-secret-1\"}");
                return new ChatResponse(List.of(new Generation(
                        AssistantMessage.builder().content("").toolCalls(List.of(call2)).build())));
            }
            if (step == 2)
            {
                return new ChatResponse(List.of(new Generation(
                        new AssistantMessage("Please click Confirm to delete tests/danger.yaml."))));
            }
            if (step == 3)
            {
                // Turn 2 after "Yes" chat message: chat text must NEVER execute destructive tools
                final AssistantMessage.ToolCall call3 = new AssistantMessage.ToolCall(
                        "c3", "function", "delete_test", "{\"testPath\":\"tests/danger.yaml\"}");
                return new ChatResponse(List.of(new Generation(
                        AssistantMessage.builder().content("").toolCalls(List.of(call3)).build())));
            }
            return new ChatResponse(List.of(new Generation(
                    new AssistantMessage("Click the Confirm button below to delete tests/danger.yaml."))));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);

        final var turn1 = chatService.runChatWorkflow(
                new ChatRequest("Delete tests/danger.yaml", null, List.of(), "sess-confirm"));
        Assertions.assertNull(turn1.action);
        Assertions.assertFalse(turn1.thinking.contains("tok-secret-1"));
        Assertions.assertTrue(turn1.thinking.contains("Blocked same-turn confirmation retry"));
        Mockito.verify(mcpToolService, Mockito.times(1)).deleteTest("tests/danger.yaml", null);
        Mockito.verify(mcpToolService, Mockito.never()).deleteTest("tests/danger.yaml", "tok-secret-1");

        final var turn2 = chatService.runChatWorkflow(
                new ChatRequest("Yes, delete it", null, List.of(), "sess-confirm"));
        Assertions.assertNull(turn2.action);
        Assertions.assertFalse(turn2.thinking.contains("tok-secret-2"));
        Assertions.assertNotNull(turn2.pendingConfirmations);
        Assertions.assertEquals(1, turn2.pendingConfirmations.size());
        Mockito.verify(mcpToolService, Mockito.never()).deleteTest("tests/danger.yaml", "tok-secret-1");
        Mockito.verify(mcpToolService, Mockito.never()).deleteTest("tests/danger.yaml", "tok-secret-2");

        final var confirmed = chatService.executePendingConfirmation(
                "sess-confirm", turn2.pendingConfirmations.get(0).actionId);
        Assertions.assertEquals("refresh_files", confirmed.action);
        Mockito.verify(mcpToolService, Mockito.times(1)).deleteTest("tests/danger.yaml", "tok-secret-2");
    }

    @Test
    public void testDetectUiActionParsesJsonStatusWithoutFalsePositives()
    {
        final Set<String> actions = new LinkedHashSet<>();
        AuraChatService.detectUiAction("cancel_run", "{\"status\":\"ALREADY_COMPLETED\"}", actions);
        AuraChatService.detectUiAction("cancel_run", "{\"status\":\"NOT_FOUND\"}", actions);
        AuraChatService.detectUiAction("cancel_run", "{\"status\":\"CONFIRMATION_PENDING_USER\"}", actions);
        AuraChatService.detectUiAction("create_test", "{\"status\":\"ERROR\",\"message\":\"Contains \\\"SUCCESS\\\" in YAML\"}", actions);
        Assertions.assertTrue(actions.isEmpty());

        AuraChatService.detectUiAction("cancel_run", "{\"status\":\"CANCELLED\"}", actions);
        AuraChatService.detectUiAction("delete_test", "{\"status\":\"SUCCESS\"}", actions);
        Assertions.assertEquals(Set.of("refresh_runs", "refresh_files"), actions);
    }

    @Test
    public void testBuildHistoryMessagesBoundsWindowToLastTwentyMessages()
    {
        final List<ChatMessageDto> history = new ArrayList<>();
        for (int i = 0; i < 30; i++)
        {
            history.add(new ChatMessageDto(i % 2 == 0 ? "user" : "ai", "msg-" + i));
        }

        final List<Message> converted = AuraChatService.buildHistoryMessages(history);
        Assertions.assertEquals(20, converted.size());
        Assertions.assertEquals("msg-10", converted.get(0).getText());
        Assertions.assertEquals("msg-29", converted.get(19).getText());
    }

    @Test
    public void testMissingApiKeyReturnsGracefulErrorResponse()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        Mockito.when(modelFactory.createChatModel())
                .thenThrow(new IllegalStateException("API Key is missing."));

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var response = chatService.runChatWorkflow(new ChatRequest("List tests", null, List.of()));

        Assertions.assertEquals("API Key is missing.", response.message);
        Assertions.assertEquals("error", response.action);
    }

    @Test
    public void testBlankPromptReturnsHelpfulMessage()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var response = chatService.runChatWorkflow(new ChatRequest("   ", null, List.of()));

        Assertions.assertEquals("Please enter a prompt.", response.message);
        Mockito.verifyNoInteractions(modelFactory);
    }

    @Test
    public void testFailedTurnDoesNotPersistPendingConfirmation()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.deleteTest("tests/danger.yaml", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-err-1",
                        "expiresInSeconds", 60,
                        "message", "Destructive operation: confirm"));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);

        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            final int step = callCount.getAndIncrement();
            if (step == 0)
            {
                final AssistantMessage.ToolCall call1 = new AssistantMessage.ToolCall(
                        "c1", "function", "delete_test", "{\"testPath\":\"tests/danger.yaml\"}");
                return new ChatResponse(List.of(new Generation(
                        AssistantMessage.builder().content("").toolCalls(List.of(call1)).build())));
            }
            if (step == 1)
            {
                throw new IllegalStateException("Upstream rate limit");
            }
            if (step == 2)
            {
                final AssistantMessage.ToolCall call2 = new AssistantMessage.ToolCall(
                        "c2", "function", "delete_test", "{\"testPath\":\"tests/danger.yaml\"}");
                return new ChatResponse(List.of(new Generation(
                        AssistantMessage.builder().content("").toolCalls(List.of(call2)).build())));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Please confirm."))));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var failedTurn = chatService.runChatWorkflow(
                new ChatRequest("Delete tests/danger.yaml", null, List.of(), "sess-fail"));
        Assertions.assertEquals("error", failedTurn.action);
        Assertions.assertFalse(failedTurn.message.contains("Upstream rate limit"),
                "Raw upstream SDK error details should stay in Thinking Process rather than main message");
        Assertions.assertTrue(failedTurn.thinking.contains("Upstream rate limit"));

        chatService.runChatWorkflow(new ChatRequest("Delete tests/danger.yaml", null, List.of(), "sess-fail"));
        Mockito.verify(mcpToolService, Mockito.times(2)).deleteTest("tests/danger.yaml", null);
        Mockito.verify(mcpToolService, Mockito.never()).deleteTest("tests/danger.yaml", "tok-err-1");
    }

    @Test
    public void testThinkingTraceLogsPostBarrierPrettyJsonAndRedactsInjectedToken()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.deleteTest("tests/danger.yaml", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-secret-2",
                        "expiresInSeconds", 60,
                        "message", "RAW_PRE_BARRIER_TOKEN_HINT"));
        Mockito.when(mcpToolService.deleteTest("tests/danger.yaml", "tok-secret-2"))
                .thenReturn(Map.of("status", "SUCCESS", "message", "Deleted cleanly."));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);

        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            final int step = callCount.getAndIncrement();
            if (step == 0)
            {
                final AssistantMessage.ToolCall call1 = new AssistantMessage.ToolCall(
                        "c1", "function", "delete_test", "{\"testPath\":\"tests/danger.yaml\"}");
                return new ChatResponse(List.of(new Generation(
                        AssistantMessage.builder().content("").toolCalls(List.of(call1)).build())));
            }
            if (step == 1)
            {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("Confirm deletion?"))));
            }
            if (step == 2)
            {
                final AssistantMessage.ToolCall call2 = new AssistantMessage.ToolCall(
                        "c2", "function", "delete_test", "{\"testPath\":\"tests/danger.yaml\"}");
                return new ChatResponse(List.of(new Generation(
                        AssistantMessage.builder().content("").toolCalls(List.of(call2)).build())));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Deleted."))));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var turn1 = chatService.runChatWorkflow(
                new ChatRequest("Delete tests/danger.yaml", null, List.of(), "sess-trace"));

        Assertions.assertFalse(turn1.thinking.contains("RAW_PRE_BARRIER_TOKEN_HINT"),
                "Thinking trace should reflect post-barrier output seen by LLM");
        Assertions.assertTrue(turn1.thinking.contains("click the Confirm or Cancel button"));
        Assertions.assertTrue(turn1.thinking.contains("\n    \"status\" :"),
                "Thinking trace should format JSON outputs with pretty printing for readability");
        Assertions.assertNotNull(turn1.pendingConfirmations);

        final var confirmed = chatService.executePendingConfirmation(
                "sess-trace", turn1.pendingConfirmations.get(0).actionId);
        Assertions.assertFalse(confirmed.thinking.contains("tok-secret-2"),
                "Thinking trace must never expose server-injected confirmation tokens");
        Assertions.assertTrue(confirmed.thinking.contains("Deleted cleanly."));
    }

    @Test
    public void testParallelToolCallsThreadSafeTraceAndActions()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.createTest(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Map.of("status", "SUCCESS", "filePath", "tests/a.yaml"));
        Mockito.when(mcpToolService.runTest(Mockito.anyString(), Mockito.isNull(), Mockito.isNull()))
                .thenReturn(Map.of("status", "RUNNING", "runId", "run-1"));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);

        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            if (callCount.getAndIncrement() == 0)
            {
                final AssistantMessage.ToolCall c1 = new AssistantMessage.ToolCall(
                        "c1", "function", "create_test",
                        "{\"testPath\":\"tests/a.yaml\",\"content\":\"steps: |\\n  Open /\"}");
                final AssistantMessage.ToolCall c2 = new AssistantMessage.ToolCall(
                        "c2", "function", "run_test",
                        "{\"testPath\":\"tests/a.yaml\"}");
                return new ChatResponse(List.of(new Generation(
                        AssistantMessage.builder().content("").toolCalls(List.of(c1, c2)).build())));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Created and started."))));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var response = chatService.runChatWorkflow(
                new ChatRequest("Create and run a.yaml", null, List.of(), "sess-parallel"));

        Assertions.assertEquals("Created and started.", response.message);
        Assertions.assertNotNull(response.actions);
        Assertions.assertTrue(response.actions.contains("refresh_files"));
        Assertions.assertTrue(response.actions.contains("refresh_runs"));
        Assertions.assertTrue(response.thinking.contains("[Tool Call] create_test"));
        Assertions.assertTrue(response.thinking.contains("[Tool Call] run_test"));
    }

    @Test
    public void testExplicitUiConfirmAndCancelPendingAction()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.deleteTest("tests/a.yaml", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-btn-1",
                        "expiresInSeconds", 60,
                        "message", "Confirm delete a"));
        Mockito.when(mcpToolService.deleteTest("tests/a.yaml", "tok-btn-1"))
                .thenReturn(Map.of("status", "SUCCESS", "message", "Deleted tests/a.yaml."));
        Mockito.when(mcpToolService.cancelRun("run-9", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-btn-2",
                        "expiresInSeconds", 60,
                        "message", "Confirm cancel run-9"));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);

        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            final int step = callCount.getAndIncrement();
            if (step == 0)
            {
                final AssistantMessage.ToolCall c1 = new AssistantMessage.ToolCall(
                        "c1", "function", "delete_test", "{\"testPath\":\"tests/a.yaml\"}");
                final AssistantMessage.ToolCall c2 = new AssistantMessage.ToolCall(
                        "c2", "function", "cancel_run", "{\"runId\":\"run-9\"}");
                return new ChatResponse(List.of(new Generation(
                        AssistantMessage.builder().content("").toolCalls(List.of(c1, c2)).build())));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Please confirm."))));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var turn1 = chatService.runChatWorkflow(
                new ChatRequest("Delete tests/a.yaml and cancel run-9", null, List.of(), "sess-btn"));

        final List<PendingConfirmationDto> pending = chatService.getPendingConfirmations("sess-btn");
        Assertions.assertEquals(2, pending.size());
        Assertions.assertNotNull(turn1.pendingConfirmations);
        Assertions.assertEquals(2, turn1.pendingConfirmations.size());

        final PendingConfirmationDto deletePending = pending.stream()
                .filter(p -> "delete_test".equals(p.toolName))
                .findFirst()
                .orElseThrow();
        final PendingConfirmationDto cancelPending = pending.stream()
                .filter(p -> "cancel_run".equals(p.toolName))
                .findFirst()
                .orElseThrow();

        // Cancel one without affecting the other
        final var cancelResp = chatService.cancelPendingConfirmation("sess-btn", cancelPending.actionId);
        Assertions.assertTrue(cancelResp.message.contains("cancel_run"));
        Assertions.assertEquals(1, chatService.getPendingConfirmations("sess-btn").size());
        Mockito.verify(mcpToolService, Mockito.never()).cancelRun(Mockito.eq("run-9"), Mockito.eq("tok-btn-2"));

        // Confirm the remaining action via UI endpoint helper
        final var confirmResp = chatService.executePendingConfirmation("sess-btn", deletePending.actionId);
        Assertions.assertEquals("refresh_files", confirmResp.action);
        Assertions.assertFalse(confirmResp.thinking.contains("tok-btn-1"));
        Assertions.assertTrue(chatService.getPendingConfirmations("sess-btn").isEmpty());
        Mockito.verify(mcpToolService, Mockito.times(1)).deleteTest("tests/a.yaml", "tok-btn-1");
    }

    @Test
    public void testBuildHistoryMessagesSkipsErrorMessages()
    {
        final List<ChatMessageDto> history = List.of(
                new ChatMessageDto("user", "List tests"),
                new ChatMessageDto("ai", "API Key missing", "", true),
                new ChatMessageDto("ai", "Error processing request: timeout", "", true),
                new ChatMessageDto("ai", "Found 3 tests.")
        );

        final List<Message> converted = AuraChatService.buildHistoryMessages(history);
        Assertions.assertEquals(2, converted.size());
        Assertions.assertEquals("List tests", converted.get(0).getText());
        Assertions.assertEquals("Found 3 tests.", converted.get(1).getText());
    }

    @Test
    public void testClarifyingTextTurnAndLlmErrorPreservePriorPendingConfirmations()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.deleteTest("tests/a.yaml", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-keep-1",
                        "expiresInSeconds", 60,
                        "message", "Confirm delete a"));
        Mockito.when(mcpToolService.deleteTest("tests/a.yaml", "tok-keep-1"))
                .thenReturn(Map.of("status", "SUCCESS", "message", "Deleted a."));
        Mockito.when(mcpToolService.cancelRun("run-9", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-discard-2",
                        "expiresInSeconds", 60,
                        "message", "Confirm cancel run-9"));
        Mockito.when(mcpToolService.listTests(Mockito.nullable(String.class)))
                .thenReturn(Map.of("total", 1, "tests", List.of(Map.of("file", "tests/a.yaml"))));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);

        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            final int step = callCount.getAndIncrement();
            if (step == 0)
            {
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "c1", "function", "delete_test", "{\"testPath\":\"tests/a.yaml\"}")))
                        .build())));
            }
            if (step == 1)
            {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("Please confirm."))));
            }
            if (step == 2)
            {
                // Turn 2: clarifying read-only question without touching delete_test
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "c2", "function", "list_tests", "{}")))
                        .build())));
            }
            if (step == 3)
            {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("Found tests/a.yaml."))));
            }
            if (step == 4)
            {
                // Turn 3: issues a new confirmation, then fails with upstream error
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "c3", "function", "cancel_run", "{\"runId\":\"run-9\"}")))
                        .build())));
            }
            throw new IllegalStateException("Transient upstream error");
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);

        // Turn 1: pending delete_test issued
        final var turn1 = chatService.runChatWorkflow(
                new ChatRequest("Delete tests/a.yaml", null, List.of(), "sess-preserve"));
        Assertions.assertNotNull(turn1.pendingConfirmations);
        Assertions.assertEquals(1, turn1.pendingConfirmations.size());

        // Turn 2: clarifying text turn must keep prior pending confirmation alive
        final var turn2 = chatService.runChatWorkflow(
                new ChatRequest("Which tests exist?", null, List.of(), "sess-preserve"));
        Assertions.assertNotNull(turn2.pendingConfirmations);
        Assertions.assertEquals(1, turn2.pendingConfirmations.size());
        Assertions.assertEquals("delete_test", turn2.pendingConfirmations.get(0).toolName);

        // Turn 3: LLM error must restore prior delete_test confirmation while discarding failed cancel_run
        final var turn3 = chatService.runChatWorkflow(
                new ChatRequest("Also cancel run-9", null, List.of(), "sess-preserve"));
        Assertions.assertEquals("error", turn3.action);
        final List<PendingConfirmationDto> afterError = chatService.getPendingConfirmations("sess-preserve");
        Assertions.assertEquals(1, afterError.size());
        Assertions.assertEquals("delete_test", afterError.get(0).toolName);

        // Prior delete_test still executes cleanly via UI confirm
        final var confirmResp = chatService.executePendingConfirmation("sess-preserve", afterError.get(0).actionId);
        Assertions.assertEquals("refresh_files", confirmResp.action);
        Mockito.verify(mcpToolService, Mockito.times(1)).deleteTest("tests/a.yaml", "tok-keep-1");
        Mockito.verify(mcpToolService, Mockito.never()).cancelRun(Mockito.eq("run-9"), Mockito.eq("tok-discard-2"));
    }

    @Test
    public void testPartialButtonConfirmationPreservesSiblingPendingConfirmation()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.deleteTest("tests/a.yaml", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-a",
                        "expiresInSeconds", 60));
        Mockito.when(mcpToolService.deleteTest("tests/a.yaml", "tok-a"))
                .thenReturn(Map.of("status", "SUCCESS", "message", "Deleted a."));
        Mockito.when(mcpToolService.cancelRun("run-9", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-run",
                        "expiresInSeconds", 60));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);

        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            if (callCount.getAndIncrement() == 0)
            {
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(
                                new AssistantMessage.ToolCall("c1", "function", "delete_test", "{\"testPath\":\"tests/a.yaml\"}"),
                                new AssistantMessage.ToolCall("c2", "function", "cancel_run", "{\"runId\":\"run-9\"}")))
                        .build())));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Confirm via buttons."))));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var turn1 = chatService.runChatWorkflow(
                new ChatRequest("Delete and cancel", null, List.of(), "sess-partial"));
        final String deleteActionId = turn1.pendingConfirmations.stream()
                .filter(p -> "delete_test".equals(p.toolName))
                .findFirst()
                .orElseThrow()
                .actionId;

        final var confirmResp = chatService.executePendingConfirmation("sess-partial", deleteActionId);
        Assertions.assertEquals("refresh_files", confirmResp.action);
        Assertions.assertNotNull(confirmResp.pendingConfirmations);
        Assertions.assertEquals(1, confirmResp.pendingConfirmations.size());
        Assertions.assertEquals("cancel_run", confirmResp.pendingConfirmations.get(0).toolName);
        Assertions.assertEquals("run-9", confirmResp.pendingConfirmations.get(0).target);
    }

    @Test
    public void testPendingConfirmationRemainsActionableWhileUnrelatedTurnIsInFlight() throws Exception
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.deleteTest("tests/a.yaml", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-inflight-1",
                        "expiresInSeconds", 60));
        Mockito.when(mcpToolService.deleteTest("tests/a.yaml", "tok-inflight-1"))
                .thenReturn(Map.of("status", "SUCCESS", "message", "Deleted tests/a.yaml."));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);

        final CountDownLatch inFlightStarted = new CountDownLatch(1);
        final CountDownLatch releaseInFlight = new CountDownLatch(1);
        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            final int step = callCount.getAndIncrement();
            if (step == 0)
            {
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "c1", "function", "delete_test", "{\"testPath\":\"tests/a.yaml\"}")))
                        .build())));
            }
            if (step == 1)
            {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("Please confirm."))));
            }
            inFlightStarted.countDown();
            if (!releaseInFlight.await(10, TimeUnit.SECONDS))
            {
                throw new IllegalStateException("Timed out waiting to release in-flight turn");
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Answered while waiting."))));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var turn1 = chatService.runChatWorkflow(
                new ChatRequest("Delete tests/a.yaml", null, List.of(), "sess-inflight"));
        Assertions.assertNotNull(turn1.pendingConfirmations);
        Assertions.assertEquals(1, turn1.pendingConfirmations.size());
        final String actionId = turn1.pendingConfirmations.get(0).actionId;

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try
        {
            final var turn2Future = executor.submit(() ->
                    chatService.runChatWorkflow(new ChatRequest("Say hello", null, List.of(), "sess-inflight")));
            Assertions.assertTrue(inFlightStarted.await(5, TimeUnit.SECONDS));

            // While Turn 2 is blocked in ChatModel.call, UI Confirm must still see and execute the pending confirmation
            Assertions.assertEquals(1, chatService.getPendingConfirmations("sess-inflight").size());
            final var confirmResp = chatService.executePendingConfirmation("sess-inflight", actionId);
            Assertions.assertEquals("refresh_files", confirmResp.action);

            releaseInFlight.countDown();
            final var turn2 = turn2Future.get(5, TimeUnit.SECONDS);
            Assertions.assertEquals("Answered while waiting.", turn2.message);
            Assertions.assertNull(turn2.pendingConfirmations);
            Assertions.assertTrue(chatService.getPendingConfirmations("sess-inflight").isEmpty());
            Mockito.verify(mcpToolService, Mockito.times(1)).deleteTest("tests/a.yaml", "tok-inflight-1");
        }
        finally
        {
            releaseInFlight.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void testInFlightTurnReinvokingDestructiveToolDoesNotResurrectConcurrentlyHandledAction() throws Exception
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.deleteTest("tests/a.yaml", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-race-del-1",
                        "expiresInSeconds", 60))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-race-del-2",
                        "expiresInSeconds", 60));
        Mockito.when(mcpToolService.deleteTest("tests/a.yaml", "tok-race-del-1"))
                .thenReturn(Map.of("status", "SUCCESS", "message", "Deleted tests/a.yaml."));
        Mockito.when(mcpToolService.cancelRun("run-race", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-race-run-1",
                        "expiresInSeconds", 60));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);

        final CountDownLatch inFlightStarted = new CountDownLatch(1);
        final CountDownLatch releaseInFlight = new CountDownLatch(1);
        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            final int step = callCount.getAndIncrement();
            if (step == 0)
            {
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(
                                new AssistantMessage.ToolCall("c1", "function", "delete_test", "{\"testPath\":\"tests/a.yaml\"}"),
                                new AssistantMessage.ToolCall("c2", "function", "cancel_run", "{\"runId\":\"run-race\"}")))
                        .build())));
            }
            if (step == 1)
            {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("Please confirm or cancel."))));
            }
            if (step == 2)
            {
                inFlightStarted.countDown();
                if (!releaseInFlight.await(10, TimeUnit.SECONDS))
                {
                    throw new IllegalStateException("Timed out waiting to release in-flight turn");
                }
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(
                                new AssistantMessage.ToolCall("c3", "function", "delete_test", "{\"testPath\":\"tests/a.yaml\"}"),
                                new AssistantMessage.ToolCall("c4", "function", "cancel_run", "{\"runId\":\"run-race\"}")))
                        .build())));
            }
            if (step == 3)
            {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("Handled while in flight."))));
            }
            if (step == 4)
            {
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "c5", "function", "delete_test", "{\"testPath\":\"tests/a.yaml\"}")))
                        .build())));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("New sequential turn issued."))));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var turn1 = chatService.runChatWorkflow(
                new ChatRequest("Delete and cancel", null, List.of(), "sess-race"));
        Assertions.assertNotNull(turn1.pendingConfirmations);
        Assertions.assertEquals(2, turn1.pendingConfirmations.size());
        final String deleteId = turn1.pendingConfirmations.stream()
                .filter(p -> "delete_test".equals(p.toolName))
                .findFirst().orElseThrow().actionId;
        final String cancelId = turn1.pendingConfirmations.stream()
                .filter(p -> "cancel_run".equals(p.toolName))
                .findFirst().orElseThrow().actionId;

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try
        {
            final var turn2Future = executor.submit(() ->
                    chatService.runChatWorkflow(new ChatRequest("Yes please", null, List.of(), "sess-race")));
            Assertions.assertTrue(inFlightStarted.await(5, TimeUnit.SECONDS));

            // Confirm delete_test and cancel cancel_run via UI while Turn 2 is blocked inside ChatModel.call
            final var confirmResp = chatService.executePendingConfirmation("sess-race", deleteId);
            Assertions.assertEquals("refresh_files", confirmResp.action);
            final var cancelResp = chatService.cancelPendingConfirmation("sess-race", cancelId);
            Assertions.assertNull(cancelResp.pendingConfirmations);

            releaseInFlight.countDown();
            final var turn2 = turn2Future.get(5, TimeUnit.SECONDS);
            Assertions.assertNull(turn2.pendingConfirmations,
                    "In-flight turn must not resurrect concurrently confirmed or cancelled cards");
            Assertions.assertTrue(chatService.getPendingConfirmations("sess-race").isEmpty());
            Mockito.verify(mcpToolService, Mockito.times(1)).deleteTest("tests/a.yaml", null);
            Mockito.verify(mcpToolService, Mockito.times(1)).cancelRun("run-race", null);
            Mockito.verify(mcpToolService, Mockito.times(1)).deleteTest("tests/a.yaml", "tok-race-del-1");

            // A subsequent sequential turn after concurrent resolution must still be able to issue a fresh confirmation
            final var turn3 = chatService.runChatWorkflow(
                    new ChatRequest("Delete tests/a.yaml again", null, List.of(), "sess-race"));
            Assertions.assertNotNull(turn3.pendingConfirmations);
            Assertions.assertEquals(1, turn3.pendingConfirmations.size());
            Mockito.verify(mcpToolService, Mockito.times(2)).deleteTest("tests/a.yaml", null);
        }
        finally
        {
            releaseInFlight.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void testExecutePendingConfirmationPropagatesToolErrorsAndPreservesSiblings()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.deleteTest("tests/missing.yaml", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-missing",
                        "expiresInSeconds", 60));
        Mockito.when(mcpToolService.deleteTest("tests/missing.yaml", "tok-missing"))
                .thenReturn(Map.of("status", "NOT_FOUND", "message", "Playbook not found."));
        Mockito.when(mcpToolService.cancelRun("run-boom", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-boom",
                        "expiresInSeconds", 60));
        Mockito.when(mcpToolService.cancelRun("run-boom", "tok-boom"))
                .thenThrow(new IllegalStateException("Queue service unavailable"));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);
        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            if (callCount.getAndIncrement() == 0)
            {
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(
                                new AssistantMessage.ToolCall("c1", "function", "delete_test", "{\"testPath\":\"tests/missing.yaml\"}"),
                                new AssistantMessage.ToolCall("c2", "function", "cancel_run", "{\"runId\":\"run-boom\"}")))
                        .build())));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Confirm?"))));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var turn1 = chatService.runChatWorkflow(
                new ChatRequest("Delete missing and cancel boom", null, List.of(), "sess-confirm-err"));
        final String deleteActionId = turn1.pendingConfirmations.stream()
                .filter(p -> "delete_test".equals(p.toolName))
                .findFirst()
                .orElseThrow()
                .actionId;
        final String cancelActionId = turn1.pendingConfirmations.stream()
                .filter(p -> "cancel_run".equals(p.toolName))
                .findFirst()
                .orElseThrow()
                .actionId;

        final var boomResp = chatService.executePendingConfirmation("sess-confirm-err", cancelActionId);
        Assertions.assertEquals("error", boomResp.action);
        Assertions.assertNotNull(boomResp.pendingConfirmations);
        Assertions.assertEquals(1, boomResp.pendingConfirmations.size());

        final var notFoundResp = chatService.executePendingConfirmation("sess-confirm-err", deleteActionId);
        Assertions.assertEquals("error", notFoundResp.action);
        Assertions.assertNull(notFoundResp.actions);
        Assertions.assertFalse(notFoundResp.message.startsWith("Confirmed"));
        Assertions.assertTrue(notFoundResp.message.contains("Could not execute"));
        Assertions.assertTrue(notFoundResp.message.contains("Playbook not found."));
    }

    @Test
    public void testHallucinatedTokenInNextTurnIsStrippedAndNeverExecutesWithoutButtonClick()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.deleteTest("tests/a.yaml", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-real-99",
                        "expiresInSeconds", 60));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);
        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            final int step = callCount.getAndIncrement();
            if (step == 0)
            {
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "c1", "function", "delete_test", "{\"testPath\":\"tests/a.yaml\"}")))
                        .build())));
            }
            if (step == 1)
            {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("Please click Confirm."))));
            }
            if (step == 2)
            {
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "c2", "function", "delete_test",
                                "{\"testPath\":\"tests/a.yaml\",\"confirmationToken\":\"confirmed\"}")))
                        .build())));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Still requires UI confirmation."))));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        chatService.runChatWorkflow(new ChatRequest("Delete tests/a.yaml", null, List.of(), "sess-hallucinated"));
        final var turn2 = chatService.runChatWorkflow(
                new ChatRequest("Yes confirm", null, List.of(), "sess-hallucinated"));

        Assertions.assertNull(turn2.action);
        Mockito.verify(mcpToolService, Mockito.times(2)).deleteTest("tests/a.yaml", null);
        Mockito.verify(mcpToolService, Mockito.never()).deleteTest("tests/a.yaml", "tok-real-99");
        Mockito.verify(mcpToolService, Mockito.never()).deleteTest("tests/a.yaml", "confirmed");
    }

    @Test
    public void testPaddedTargetPreservedForUiConfirmAndClearPendingConfirmationsEvictsSession()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.deleteTest(" tests/pad.yaml ", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-pad-1",
                        "expiresInSeconds", 60));
        Mockito.when(mcpToolService.deleteTest(" tests/pad.yaml ", "tok-pad-1"))
                .thenReturn(Map.of("status", "SUCCESS", "message", "Deleted."));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);
        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            if (callCount.getAndIncrement() == 0)
            {
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "c1", "function", "delete_test", "{\"testPath\":\" tests/pad.yaml \"}")))
                        .build())));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Confirm?"))));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var turn1 = chatService.runChatWorkflow(
                new ChatRequest("Delete pad", null, List.of(), "sess-pad"));
        Assertions.assertNotNull(turn1.pendingConfirmations);
        Assertions.assertEquals(" tests/pad.yaml ", turn1.pendingConfirmations.get(0).target);

        final var confirmed = chatService.executePendingConfirmation(
                "sess-pad", turn1.pendingConfirmations.get(0).actionId);
        Assertions.assertEquals("refresh_files", confirmed.action);
        Mockito.verify(mcpToolService).deleteTest(" tests/pad.yaml ", "tok-pad-1");

        // Re-issue and verify clearPendingConfirmations evicts session state
        callCount.set(0);
        chatService.runChatWorkflow(new ChatRequest("Delete pad again", null, List.of(), "sess-pad"));
        Assertions.assertEquals(1, chatService.getPendingConfirmations("sess-pad").size());
        chatService.clearPendingConfirmations("sess-pad");
        Assertions.assertTrue(chatService.getPendingConfirmations("sess-pad").isEmpty());
    }

    @Test
    public void testBuildHistoryMessagesCoalescesAdjacentAssistantMessagesAndDropsLeadingAssistant()
    {
        final List<ChatMessageDto> history = List.of(
                new ChatMessageDto("ai", "Orphaned leading reply"),
                new ChatMessageDto("user", "Delete tests/a.yaml"),
                new ChatMessageDto("ai", "Please confirm."),
                new ChatMessageDto("ai", "Confirmed `delete_test` for `tests/a.yaml`.")
        );

        final List<Message> converted = AuraChatService.buildHistoryMessages(history);
        Assertions.assertEquals(2, converted.size());
        Assertions.assertEquals("Delete tests/a.yaml", converted.get(0).getText());
        Assertions.assertTrue(converted.get(1) instanceof AssistantMessage);
        Assertions.assertEquals(
                "Please confirm.\n\nConfirmed `delete_test` for `tests/a.yaml`.",
                converted.get(1).getText());
    }

    @Test
    public void testBuildHistoryMessagesCoalescesAdjacentUsersAndRunWorkflowMergesTrailingUserAfterError()
    {
        final List<ChatMessageDto> history = List.of(
                new ChatMessageDto("user", "Delete old.yaml"),
                new ChatMessageDto("ai", "Error processing request: timeout", "", true),
                new ChatMessageDto("user", "List tests"),
                new ChatMessageDto("ai", "Error processing request: 503", "", true)
        );

        final List<Message> converted = AuraChatService.buildHistoryMessages(history);
        Assertions.assertEquals(1, converted.size());
        Assertions.assertEquals("Delete old.yaml\n\nList tests", converted.get(0).getText());

        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Retried OK.")))));

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var resp = chatService.runChatWorkflow(
                new ChatRequest("Try again now", null, history, "sess-user-alt"));
        Assertions.assertEquals("Retried OK.", resp.message);

        final ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        Mockito.verify(mockChatModel).call(promptCaptor.capture());
        final List<Message> sent = promptCaptor.getValue().getInstructions();
        // System + single coalesced User message (never two adjacent UserMessages)
        final long userCount = sent.stream().filter(m -> m instanceof UserMessage).count();
        Assertions.assertEquals(1, userCount);
        final String sentUserText = sent.get(sent.size() - 1).getText();
        Assertions.assertTrue(sentUserText.contains("Delete old.yaml"));
        Assertions.assertTrue(sentUserText.contains("Try again now"));
    }

    @Test
    public void testExecutePendingConfirmationHandlesAlreadyCompletedAndPreservesSiblingsOnMissingOrExpired()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.cancelRun("run-done", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-done",
                        "expiresInSeconds", 60));
        Mockito.when(mcpToolService.cancelRun("run-done", "tok-done"))
                .thenReturn(Map.of("status", "ALREADY_COMPLETED", "message", "Run already finished."));
        Mockito.when(mcpToolService.deleteTest("tests/keep.yaml", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-keep-sibling",
                        "expiresInSeconds", 60));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);
        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            if (callCount.getAndIncrement() == 0)
            {
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(
                                new AssistantMessage.ToolCall("c1", "function", "cancel_run", "{\"runId\":\"run-done\"}"),
                                new AssistantMessage.ToolCall("c2", "function", "delete_test", "{\"testPath\":\"tests/keep.yaml\"}")))
                        .build())));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Confirm?"))));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var turn1 = chatService.runChatWorkflow(
                new ChatRequest("Cancel run-done and delete keep.yaml", null, List.of(), "sess-siblings"));
        final String cancelId = turn1.pendingConfirmations.stream()
                .filter(p -> "cancel_run".equals(p.toolName))
                .findFirst()
                .orElseThrow()
                .actionId;

        final var missingIdResp = chatService.executePendingConfirmation("sess-siblings", "unknown-id");
        Assertions.assertEquals("error", missingIdResp.action);
        Assertions.assertNotNull(missingIdResp.pendingConfirmations);
        Assertions.assertEquals(2, missingIdResp.pendingConfirmations.size());

        final var alreadyCompletedResp = chatService.executePendingConfirmation("sess-siblings", cancelId);
        Assertions.assertEquals("error", alreadyCompletedResp.action);
        Assertions.assertTrue(alreadyCompletedResp.message.contains("Could not execute"));
        Assertions.assertTrue(alreadyCompletedResp.message.contains("Run already finished."));
        Assertions.assertNotNull(alreadyCompletedResp.pendingConfirmations);
        Assertions.assertEquals(1, alreadyCompletedResp.pendingConfirmations.size());
    }

    @Test
    public void testPostToolLlmFailurePreservesEmittedUiActions()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.createTest("tests/created.yaml", "steps: |\n  Open /"))
                .thenReturn(Map.of("status", "SUCCESS", "filePath", "tests/created.yaml"));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);
        final AtomicInteger callCount = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            if (callCount.getAndIncrement() == 0)
            {
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "c1", "function", "create_test",
                                "{\"testPath\":\"tests/created.yaml\",\"content\":\"steps: |\\n  Open /\"}")))
                        .build())));
            }
            throw new IllegalStateException("Summary timeout");
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final var resp = chatService.runChatWorkflow(
                new ChatRequest("Create created.yaml", null, List.of(), "sess-post-tool-err"));

        Assertions.assertEquals("error", resp.action);
        Assertions.assertEquals(List.of("refresh_files"), resp.actions);
        Mockito.verify(mcpToolService).createTest("tests/created.yaml", "steps: |\n  Open /");
    }

    @Test
    public void testResolutionMarkersPrunedWhenIdleAndExpiredClickDoesNotBlockInFlightTurn() throws Exception
    {
        final AtomicReference<Instant> nowRef = new AtomicReference<>(Instant.parse("2026-11-16T12:00:00Z"));
        final Clock mutableClock = new Clock()
        {
            @Override
            public ZoneId getZone()
            {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(final ZoneId zone)
            {
                return this;
            }

            @Override
            public Instant instant()
            {
                return nowRef.get();
            }
        };

        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.deleteTest("tests/exp.yaml", null))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-exp-1",
                        "expiresInSeconds", 60))
                .thenReturn(Map.of(
                        "status", "CONFIRMATION_REQUIRED",
                        "confirmationToken", "tok-exp-2",
                        "expiresInSeconds", 60));

        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);

        final CountDownLatch inFlightStarted = new CountDownLatch(1);
        final CountDownLatch releaseInFlight = new CountDownLatch(1);
        final AtomicInteger step = new AtomicInteger(0);
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class))).thenAnswer(invocation -> {
            final int current = step.getAndIncrement();
            if (current == 0 || current == 2)
            {
                if (current == 2)
                {
                    inFlightStarted.countDown();
                    Assertions.assertTrue(releaseInFlight.await(5, TimeUnit.SECONDS));
                }
                return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "c" + current, "function", "delete_test", "{\"testPath\":\"tests/exp.yaml\"}")))
                        .build())));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Please confirm."))));
        });

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider, mutableClock);
        final var turn1 = chatService.runChatWorkflow(
                new ChatRequest("Delete tests/exp.yaml", null, List.of(), "sess-prune"));
        final String expiredId = turn1.pendingConfirmations.get(0).actionId;
        Assertions.assertEquals(0, chatService.getResolutionMarkerCount());

        // Missing cancel should report error action so it is excluded from LLM replay history
        final var missingCancel = chatService.cancelPendingConfirmation("sess-prune", "missing-id");
        Assertions.assertEquals("error", missingCancel.action);
        Assertions.assertEquals(0, chatService.getResolutionMarkerCount());

        // Advance clock past 55s confirmation TTL while an in-flight turn is running
        nowRef.set(nowRef.get().plusSeconds(61));

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try
        {
            final var inFlightFuture = executor.submit(() ->
                    chatService.runChatWorkflow(new ChatRequest("Delete again", null, List.of(), "sess-prune")));
            Assertions.assertTrue(inFlightStarted.await(5, TimeUnit.SECONDS));

            final var expiredClick = chatService.executePendingConfirmation("sess-prune", expiredId);
            Assertions.assertEquals("error", expiredClick.action);
            Assertions.assertTrue(expiredClick.message.contains("expired"));

            releaseInFlight.countDown();
            final var turn2 = inFlightFuture.get(5, TimeUnit.SECONDS);
            Assertions.assertNotNull(turn2.pendingConfirmations,
                    "Clicking an already-expired confirmation must not block a concurrent turn from issuing a fresh one");
            Assertions.assertEquals(1, turn2.pendingConfirmations.size());
            Assertions.assertEquals(0, chatService.getResolutionMarkerCount(),
                    "Resolution markers must be pruned once in-flight turns complete");
        }
        finally
        {
            releaseInFlight.countDown();
            executor.shutdownNow();
        }

        // Clearing session when idle must also leave zero leaked markers
        chatService.clearPendingConfirmations("sess-prune");
        Assertions.assertEquals(0, chatService.getResolutionMarkerCount());
    }

    @Test
    public void testRunChatWorkflowPrependsActiveViewContextAndActiveFile()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        final AuraMcpToolCallbackProvider callbackProvider = new AuraMcpToolCallbackProvider(mcpToolService);
        final AuraChatModelFactory modelFactory = Mockito.mock(AuraChatModelFactory.class);
        final ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        Mockito.when(mockChatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        Mockito.when(modelFactory.createChatModel()).thenReturn(mockChatModel);

        final ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        Mockito.when(mockChatModel.call(promptCaptor.capture()))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Dashboard looks healthy.")))));

        final AuraChatService chatService = new AuraChatService(modelFactory, callbackProvider);
        final ChatRequest req = new ChatRequest(
                "Analyze this dashboard",
                "tests/Login.yaml",
                List.of(),
                "sess-ctx",
                "view=CustomDashboard; pageId=page-general; pageName=General Overview");
        final var resp = chatService.runChatWorkflow(req);

        Assertions.assertEquals("Dashboard looks healthy.", resp.message);
        final Prompt captured = promptCaptor.getValue();
        final String rendered = captured.getInstructions().stream()
                .map(Message::getText)
                .reduce("", (a, b) -> a + "\n" + b);
        Assertions.assertTrue(rendered.contains("[Active view context: view=CustomDashboard; pageId=page-general; pageName=General Overview]"));
        Assertions.assertTrue(rendered.contains("[Active editor file: tests/Login.yaml]"));
        Assertions.assertTrue(rendered.contains("Analyze this dashboard"));
    }
}

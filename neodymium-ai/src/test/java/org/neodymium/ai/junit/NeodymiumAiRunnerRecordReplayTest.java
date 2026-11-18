/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.junit;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.io.TempDir;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.report.PreliminaryReportListener;
import org.neodymium.ai.report.TestExecutionReport;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.util.Neodymium;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * End-to-end recording tests for {@link NeodymiumAiRunner} lifecycle handling, driven through the real JUnit
 * extension callbacks ({@code @BeforeEach}, intercepted test method, {@code @AfterEach}) and the real
 * {@link org.neodymium.ai.recorder.PlaybookRecorder}.
 * <p>
 * Each scenario runs in {@link ExecutionMode#FORCE_RECORDING} against a temporary recording directory and verifies
 * the recording that was written: every step exactly once, in execution order, composites kept with their sub-steps.
 * The composite scenario additionally replays its recording in {@link ExecutionMode#REPLAY_STRICT}.
 * <p>
 * Known limit: the LLM is a mock and no browser is running. Mock steps never produce recordable tool calls, and strict
 * replay rejects steps without recorded tool calls. Replay of programmatic and hybrid recordings can therefore not be
 * verified here; that needs a run against the test-server sandbox with a real browser.
 *
 * @author AI-generated: Claude Sonnet 5.5
 * @author Xceptance GmbH 2026
 */
public class NeodymiumAiRunnerRecordReplayTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Programmatic test (no YAML) that records into {@code e2e_programmatic.json}. */
    public static class ProgrammaticRecordClass
    {
        @Test
        @AiMode(ExecutionMode.FORCE_RECORDING)
        @AiPlaybook(value = AiPlaybook.PROGRAMMATIC, recordingFileName = "e2e_programmatic")
        public void testRun()
        {
        }
    }

    /** Hybrid test (YAML test method, programmatic setup) that records into {@code e2e_hybrid.json}. */
    public static class HybridRecordClass
    {
        @Test
        @AiMode(ExecutionMode.FORCE_RECORDING)
        @AiInlinePlaybook("name: hybrid\nsteps:\n  - step: YAML main step\n")
        @AiPlaybook(recordingFileName = "e2e_hybrid")
        public void testRun()
        {
        }
    }

    /** YAML test containing a folded {@code >-} composite step that records into {@code e2e_composite.json}. */
    public static class CompositeRecordClass
    {
        @Test
        @AiMode(ExecutionMode.FORCE_RECORDING)
        @AiInlinePlaybook("name: composite\nsteps:\n  - Click the button\n  - >-\n    Verify header is displayed.\n    Verify items are displayed.\n")
        @AiPlaybook(recordingFileName = "e2e_composite")
        public void testRun()
        {
        }
    }

    /** YAML test containing a folded {@code >-} composite step that replays {@code e2e_composite.json}. */
    public static class CompositeReplayClass
    {
        @Test
        @AiMode(ExecutionMode.REPLAY_STRICT)
        @AiInlinePlaybook("name: composite\nsteps:\n  - Click the button\n  - >-\n    Verify header is displayed.\n    Verify items are displayed.\n")
        @AiPlaybook(recordingFileName = "e2e_composite")
        public void testRun()
        {
        }
    }

    /**
     * Outcome of one simulated JUnit lifecycle run.
     *
     * @param report   the preliminary report built from the session events
     * @param llmCalls number of live LLM requests issued during the run
     */
    private record RunResult(TestExecutionReport report, int llmCalls)
    {
        List<String> instructions()
        {
            final List<String> result = new ArrayList<>();
            report.getSteps().forEach(step -> result.add(step.getInstruction()));
            return result;
        }
    }

    /**
     * Goal: A fully programmatic test whose {@code @BeforeEach} and test method both call
     * {@code session.execute(...)} must record all steps once, in execution order.
     */
    @Test
    public void testProgrammaticBeforeEachAndTestMethodAreRecordedInOrder(@TempDir final Path tempDir) throws Throwable
    {
        withRecordingDirectory(tempDir, () -> {
            final List<String> setup = List.of("Setup step");
            final List<String> body = List.of("Main step 1", "Main step 2");
            final List<String> expected = List.of("Setup step", "Main step 1", "Main step 2");

            final RunResult recorded = runLifecycle(ProgrammaticRecordClass.class, setup, body);
            Assertions.assertEquals(expected, recorded.instructions(), "Recording run must report all steps in order");
            assertRecordedTopLevelInstructions(tempDir.resolve("e2e_programmatic.json"), expected);
        });
    }

    /**
     * Goal: A hybrid test (programmatic {@code @BeforeEach} plus a YAML test method) must record each
     * step once, in execution order, without duplicating the setup step.
     */
    @Test
    public void testHybridProgrammaticSetupAndYamlTestAreRecordedInOrder(@TempDir final Path tempDir) throws Throwable
    {
        withRecordingDirectory(tempDir, () -> {
            final List<String> setup = List.of("Setup step");
            final List<String> expected = List.of("Setup step", "YAML main step");

            final RunResult recorded = runLifecycle(HybridRecordClass.class, setup, Collections.emptyList());
            Assertions.assertEquals(expected, recorded.instructions(), "Recording run must report setup and YAML steps in order");
            assertRecordedTopLevelInstructions(tempDir.resolve("e2e_hybrid.json"), expected);
        });
    }

    /**
     * Goal: A YAML test containing a folded {@code >-} composite step must record the composite with
     * its sub-steps (not drop it), and a strict replay of that recording must succeed.
     */
    @Test
    public void testCompositeStepRecordThenReplay(@TempDir final Path tempDir) throws Throwable
    {
        withRecordingDirectory(tempDir, () -> {
            runLifecycle(CompositeRecordClass.class, Collections.emptyList(), Collections.emptyList());

            final JsonNode recording = MAPPER.readTree(Files.readString(tempDir.resolve("e2e_composite.json")));
            Assertions.assertTrue(recording.isArray(), "Recording must be a JSON array of top-level steps");
            Assertions.assertEquals(2, recording.size(),
                "Recording must hold the plain step and the composite step as two top-level entries: " + recording);
            final JsonNode composite = recording.get(1);
            Assertions.assertNotNull(composite.get("subSteps"), "Composite step must be recorded with its sub-steps: " + recording);
            Assertions.assertEquals(2, composite.get("subSteps").size(), "Composite must keep both sub-steps: " + recording);

            final RunResult replayed = runLifecycle(CompositeReplayClass.class, Collections.emptyList(), Collections.emptyList());
            Assertions.assertTrue(replayed.report().isSuccess(), "Replay of a recording with a composite step must succeed");
            Assertions.assertEquals(0, replayed.llmCalls(), "Strict replay must not fall back to live LLM calls");
        });
    }

    /**
     * Runs the given body with the recording directory pointed to a temporary folder and restores
     * all global state afterwards.
     *
     * @param tempDir the directory that receives and provides recordings
     * @param body    the scenario to run
     * @throws Throwable if the scenario fails
     */
    private static void withRecordingDirectory(final Path tempDir, final ThrowingRunnable body) throws Throwable
    {
        System.setProperty("neodymium.ai.playbook.recordingDirectory", tempDir.toString());
        AiConfiguration.resetInstance();
        try
        {
            body.run();
        }
        finally
        {
            System.clearProperty("neodymium.ai.playbook.recordingDirectory");
            AiConfiguration.resetInstance();
            Neodymium.clearThreadContext();
        }
    }

    /** A runnable that may throw checked exceptions and errors. */
    @FunctionalInterface
    private interface ThrowingRunnable
    {
        void run() throws Throwable;
    }

    /**
     * Simulates one JUnit lifecycle for the given sample class: {@code @BeforeEach} callback, programmatic
     * setup calls, the intercepted test method with programmatic body calls, and {@code @AfterEach}.
     *
     * @param testClass    the sample test class (single method {@code testRun})
     * @param setupSteps   instructions executed programmatically as if from a {@code @BeforeEach} method
     * @param bodySteps    instructions executed programmatically from within the test method
     * @return the report and LLM call count of the run
     * @throws Throwable if any lifecycle phase fails
     */
    private static RunResult runLifecycle(final Class<?> testClass, final List<String> setupSteps, final List<String> bodySteps) throws Throwable
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = testClass.getMethod("testRun");
        final ExtensionContext extensionContext = createMockExtensionContext(testClass, method);

        final List<TestTemplateInvocationContext> invocations = runner.provideTestTemplateInvocationContexts(extensionContext).toList();
        Assertions.assertFalse(invocations.isEmpty());

        final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
        final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream().filter(e -> e instanceof BeforeEachCallback).findFirst().orElseThrow();
        final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream().filter(e -> e instanceof AfterEachCallback).findFirst().orElseThrow();
        final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream().filter(e -> e instanceof InvocationInterceptor).findFirst().orElseThrow();

        beforeEach.beforeEach(extensionContext);
        final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
        Assertions.assertNotNull(session);

        final AtomicInteger llmCalls = new AtomicInteger();
        final LlmProvider countingLlm = countingMockLlm(llmCalls);
        session.getLlmRegistry().setDefaultProvider(countingLlm);
        for (final LlmCapability capability : LlmCapability.values())
        {
            session.getLlmRegistry().registerProvider(capability, countingLlm);
        }

        for (final String instruction : setupSteps)
        {
            session.execute(instruction);
        }

        interceptor.interceptTestMethod(
            () -> {
                for (final String instruction : bodySteps)
                {
                    session.execute(instruction);
                }
                return null;
            },
            mockReflectiveInvocationContext(method),
            extensionContext
        );

        afterEach.afterEach(extensionContext);

        final PreliminaryReportListener reportListener = (PreliminaryReportListener) session.getEventBus().getListeners().stream()
            .filter(l -> l instanceof PreliminaryReportListener)
            .findFirst()
            .orElseThrow();
        return new RunResult(reportListener.getReport(), llmCalls.get());
    }

    /**
     * Asserts that a recording file is a JSON array whose top-level entries carry exactly the given instructions in order.
     *
     * @param recordingFile        the recording written by the recorder
     * @param expectedInstructions the expected top-level instructions
     * @throws Exception if the file cannot be read or parsed
     */
    private static void assertRecordedTopLevelInstructions(final Path recordingFile, final List<String> expectedInstructions) throws Exception
    {
        Assertions.assertTrue(Files.exists(recordingFile), "Recording file must have been written: " + recordingFile);
        final JsonNode recording = MAPPER.readTree(Files.readString(recordingFile));
        Assertions.assertTrue(recording.isArray(), "Recording must be a JSON array of top-level steps");
        final List<String> actual = new ArrayList<>();
        recording.forEach(node -> actual.add(node.get("instruction").asText()));
        Assertions.assertEquals(expectedInstructions, actual, "Recording must contain every step exactly once, in execution order: " + recording);
    }

    /**
     * Creates a mock LLM that makes every live step record a real, browser-free tool call and counts the requests
     * it receives. Strict replay only accepts steps that carry recorded tool calls, so a mock that merely completes
     * steps would make every replay fail for reasons unrelated to what is under test.
     * <p>
     * The mock alternates between a one-millisecond {@code wait} tool call and {@code complete_step}, so every live step
     * performs exactly one {@code wait} before it completes (an unchanging answer would trip the thrashing breaker).
     *
     * @param calls counter incremented for each request
     * @return the counting provider
     */
    private static LlmProvider countingMockLlm(final AtomicInteger calls)
    {
        return new LlmProvider()
        {
            @Override
            public LlmResponse chat(final LlmRequest request) throws IOException
            {
                final int callNumber = calls.incrementAndGet();
                final boolean completeNow = callNumber % 2 == 0;
                final ObjectNode arguments = JsonNodeFactory.instance.objectNode();
                final String toolName;
                if (completeNow)
                {
                    toolName = "complete_step";
                }
                else
                {
                    toolName = "wait";
                    arguments.put("durationMs", 1);
                }
                return new LlmResponse("[]", new TokenUsage(10, 0, 10), "mock-model", List.of(new ToolCall("mock_call_" + callNumber, toolName, arguments)), null);
            }

            @Override
            public Set<LlmCapability> getCapabilities()
            {
                return Set.of(LlmCapability.values());
            }
        };
    }

    private static ExtensionContext createMockExtensionContext(final Class<?> testClass, final Method method)
    {
        final Map<Object, Object> storeMap = new HashMap<>();
        final ExtensionContext.Store mockStore = (ExtensionContext.Store) Proxy.newProxyInstance(
            ExtensionContext.Store.class.getClassLoader(),
            new Class<?>[]{ExtensionContext.Store.class},
            (sp, sm, sargs) -> {
                if ("put".equals(sm.getName()))
                {
                    storeMap.put(sargs[0], sargs[1]);
                    return null;
                }
                if ("get".equals(sm.getName()))
                {
                    return storeMap.get(sargs[0]);
                }
                return null;
            }
        );

        return (ExtensionContext) Proxy.newProxyInstance(
            ExtensionContext.class.getClassLoader(),
            new Class<?>[]{ExtensionContext.class},
            (proxy, m, args) -> {
                if ("getTestClass".equals(m.getName()))
                {
                    return Optional.of(testClass);
                }
                if ("getRequiredTestClass".equals(m.getName()))
                {
                    return testClass;
                }
                if ("getTestMethod".equals(m.getName()))
                {
                    return Optional.of(method);
                }
                if ("getRequiredTestMethod".equals(m.getName()))
                {
                    return method;
                }
                if ("getRequiredTestInstance".equals(m.getName()))
                {
                    return testClass.getDeclaredConstructor().newInstance();
                }
                if ("getStore".equals(m.getName()))
                {
                    return mockStore;
                }
                if (m.getReturnType().equals(Optional.class))
                {
                    return Optional.empty();
                }
                return null;
            }
        );
    }

    @SuppressWarnings("unchecked")
    private static ReflectiveInvocationContext<Method> mockReflectiveInvocationContext(final Method method)
    {
        return (ReflectiveInvocationContext<Method>) Proxy.newProxyInstance(
            ReflectiveInvocationContext.class.getClassLoader(),
            new Class<?>[]{ReflectiveInvocationContext.class},
            (proxy, m, args) -> {
                if ("getExecutable".equals(m.getName()))
                {
                    return method;
                }
                if ("getTarget".equals(m.getName()))
                {
                    return Optional.empty();
                }
                if ("getArguments".equals(m.getName()))
                {
                    return Collections.emptyList();
                }
                return null;
            }
        );
    }
}

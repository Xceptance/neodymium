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
package com.xceptance.neodymium.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codeborne.selenide.Configuration;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.config.AiConfiguration;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.ai.core.AiStats;
import com.xceptance.neodymium.ai.core.LlmClient;

/**
 * Tests for the explicit language tags features in {@link AiAgent}:
 * - Tag parsing & clean tag stripping architecture.
 * - Soft/Optional step execution failure bypassing.
 * - Timeout isolation under all outcomes.
 */
public final class AiExplicitTagsTest
{
    private AiConfiguration config;
    private PageAnalyzer dummyAnalyzer;
    private LlmClient dummyLlmClient;

    @BeforeEach
    public void setUp()
    {
        config = Neodymium.aiConfiguration();
        dummyAnalyzer = new PageAnalyzer()
        {
            @Override
            public String getPageContext(final ContextLevel level)
            {
                return "";
            }

            @Override
            public String captureScreenshot(final String title)
            {
                return "";
            }
        };
        dummyLlmClient = new LlmClient(config, new AiStats())
        {
            @Override
            public String chat(final String systemPrompt, final String userPrompt)
            {
                return "{\"r\": \"simulated reasoning\", \"a\": [{\"t\": \"CLICK\", \"tg\": \"#dummy-btn\", \"desc\": \"Click dummy button\"}], \"d\": true}";
            }

            @Override
            public String chatWithScreenshot(final String systemPrompt, final String userPrompt, final String screenshot)
            {
                return "{\"r\": \"simulated reasoning\", \"a\": [{\"t\": \"CLICK\", \"tg\": \"#dummy-btn\", \"desc\": \"Click dummy button\"}], \"d\": true}";
            }
        };

        // Prevent dirty state leaking between tests
        Neodymium.setAiPlaybook(null);
        Neodymium.initializePlaybook();
        config.setProperty("neodymium.ai.pesap.classify.enabled", "true");
        config.setProperty("neodymium.ai.pesap.linter.enabled", "true");
    }

    /**
     * Verifies that the tag patterns match correctly and extract expected values.
     */
    @Test
    public void testRegexPatterns() throws Exception
    {
        final Field optionalField = AiAgent.class.getDeclaredField("OPTIONAL_TAG_PATTERN");
        optionalField.setAccessible(true);
        final Pattern optionalPattern = (Pattern) optionalField.get(null);
        assertNotNull(optionalPattern, "Expected OPTIONAL_TAG_PATTERN to not be null");

        final Matcher matcher1 = optionalPattern.matcher("Click element (optional)");
        assertTrue(matcher1.find(), "Expected 'Click element (optional)' to match optional pattern");
        assertEquals("optional", matcher1.group(1), "Expected matcher group 1 to be 'optional', but got: " + matcher1.group(1));

        final Matcher matcher2 = optionalPattern.matcher("Verify header (soft)");
        assertTrue(matcher2.find(), "Expected 'Verify header (soft)' to match optional pattern");
        assertEquals("soft", matcher2.group(1), "Expected matcher group 1 to be 'soft', but got: " + matcher2.group(1));

        final Matcher matcher3 = optionalPattern.matcher("Click element (OPTIONAL)");
        assertTrue(matcher3.find(), "Expected 'Click element (OPTIONAL)' to match optional pattern");
        assertEquals("OPTIONAL", matcher3.group(1), "Expected matcher group 1 to be 'OPTIONAL', but got: " + matcher3.group(1));

        final Field timeoutField = AiAgent.class.getDeclaredField("TIMEOUT_TAG_PATTERN");
        timeoutField.setAccessible(true);
        final Pattern timeoutPattern = (Pattern) timeoutField.get(null);
        assertNotNull(timeoutPattern, "Expected TIMEOUT_TAG_PATTERN to not be null");

        final Matcher matcher4 = timeoutPattern.matcher("Click with timeout (timeout: 500ms)");
        assertTrue(matcher4.find(), "Expected 'Click with timeout (timeout: 500ms)' to match timeout pattern");
        assertEquals("500", matcher4.group(1), "Expected matcher group 1 to be '500', but got: " + matcher4.group(1));
        assertEquals("ms", matcher4.group(2), "Expected matcher group 2 to be 'ms', but got: " + matcher4.group(2));

        final Matcher matcher5 = timeoutPattern.matcher("Verify content (timeout: 2s)");
        assertTrue(matcher5.find(), "Expected 'Verify content (timeout: 2s)' to match timeout pattern");
        assertEquals("2", matcher5.group(1), "Expected matcher group 1 to be '2', but got: " + matcher5.group(1));
        assertEquals("s", matcher5.group(2), "Expected matcher group 2 to be 's', but got: " + matcher5.group(2));

        final Matcher matcher6 = timeoutPattern.matcher("Action with pure digits (timeout: 100)");
        assertTrue(matcher6.find(), "Expected 'Action with pure digits (timeout: 100)' to match timeout pattern");
        assertEquals("100", matcher6.group(1), "Expected matcher group 1 to be '100', but got: " + matcher6.group(1));
        assertNull(matcher6.group(2), "Expected matcher group 2 to be null, but got: " + matcher6.group(2));
    }

    /**
     * Verifies tag stripping normalized representation.
     */
    @Test
    public void testStripAllTags()
    {
        assertEquals("Click standard button", AiAgent.stripAllTags("Click standard button (optional)"));
        assertEquals("Verify login button", AiAgent.stripAllTags("Verify login button (soft)"));
        assertEquals("Wait for element to load", AiAgent.stripAllTags("Wait for element to load (timeout: 500ms)"));
        assertEquals("Check dashboard visibility", AiAgent.stripAllTags("Check dashboard visibility (timeout: 2s) (optional)"));
        assertEquals("Verify text in field", AiAgent.stripAllTags("Verify text in field (OPTIONAL) (TIMEOUT: 100)"));
    }

    /**
     * Verifies timeout isolation under successful action execution.
     */
    @Test
    public void testTimeoutIsolation_Success() throws Exception
    {
        final long originalTimeout = 3000;
        Configuration.timeout = originalTimeout;

        final List<Long> recordedTimeouts = new ArrayList<>();
        final ActionExecutor recordingExecutor = new ActionExecutor(null)
        {
            @Override
            public void executeAll(final List<Action> actions)
            {
                recordedTimeouts.add(Configuration.timeout);
            }
        };

        final AiAgent agent = new AiAgent(dummyLlmClient, dummyAnalyzer, recordingExecutor, config);
        setExecutionLog(agent);

        final String prompt = "Click element (timeout: 500ms)";
        final Playbook playbook = Neodymium.getAiPlaybook();
        playbook.setRecording(false);

        final PlaybookStep pbStep = playbook.getCurrentStep();
        pbStep.setPromptLine("Click element");
        pbStep.setActions(List.of(new Action("CLICK", "btn", "Click")));

        final Method executeStepMethod = AiAgent.class.getDeclaredMethod(
            "executeStep",
            int.class, String.class, boolean.class, String.class,
            boolean.class, Long.class,
            List.class, String.class, List.class, Integer.class, String.class,
            StepDetails.class, AiExecutionResult.class
        );
        executeStepMethod.setAccessible(true);

        executeStepMethod.invoke(
            agent,
            0, "Click element", false, null,
            false, 500L,
            new ArrayList<String>(), prompt, new ArrayList<String>(), null, null,
            new StepDetails("Click element"), new AiExecutionResult(new HashMap<>())
        );

        assertEquals(1, recordedTimeouts.size());
        assertEquals(500L, recordedTimeouts.get(0));
        assertEquals(originalTimeout, Configuration.timeout);
    }

    /**
     * Verifies timeout isolation under action execution failure.
     */
    @Test
    public void testTimeoutIsolation_Failure() throws Exception
    {
        final long originalTimeout = 3000;
        Configuration.timeout = originalTimeout;

        final List<Long> recordedTimeouts = new ArrayList<>();
        final ActionExecutor failingExecutor = new ActionExecutor(null)
        {
            @Override
            public void executeAll(final List<Action> actions)
            {
                recordedTimeouts.add(Configuration.timeout);
                throw new ActionExecutionException("Action execution simulated failure");
            }
        };

        final AiAgent agent = new AiAgent(dummyLlmClient, dummyAnalyzer, failingExecutor, config);
        setExecutionLog(agent);

        final String prompt = "Click element (timeout: 2s)";
        final Playbook playbook = Neodymium.getAiPlaybook();
        playbook.setRecording(false);

        final PlaybookStep pbStep = playbook.getCurrentStep();
        pbStep.setPromptLine("Click element");
        pbStep.setActions(List.of(new Action("CLICK", "btn", "Click")));

        final Method executeStepMethod = AiAgent.class.getDeclaredMethod(
            "executeStep",
            int.class, String.class, boolean.class, String.class,
            boolean.class, Long.class,
            List.class, String.class, List.class, Integer.class, String.class,
            StepDetails.class, AiExecutionResult.class
        );
        executeStepMethod.setAccessible(true);

        try
        {
            executeStepMethod.invoke(
                agent,
                0, "Click element", false, null,
                false, 2000L,
                new ArrayList<String>(), prompt, new ArrayList<String>(), null, null,
                new StepDetails("Click element"), new AiExecutionResult(new HashMap<>())
            );
        }
        catch (final Exception e)
        {
            // Expected exception to be thrown
        }

        assertFalse(recordedTimeouts.isEmpty());
        for (final long timeout : recordedTimeouts)
        {
            assertEquals(2000L, timeout);
        }
        assertEquals(originalTimeout, Configuration.timeout);
    }

    /**
     * Verifies soft execution bypassing on ActionExecutionException.
     */
    @Test
    public void testOptionalStepBypassesActionExecutionException() throws Exception
    {
        final ActionExecutor failingExecutor = new ActionExecutor(null)
        {
            @Override
            public void executeAll(final List<Action> actions)
            {
                throw new ActionExecutionException("Action execution simulated failure");
            }
        };

        final AiAgent agent = new AiAgent(dummyLlmClient, dummyAnalyzer, failingExecutor, config);
        setExecutionLog(agent);

        final String prompt = "Click element (optional)";
        final Playbook playbook = Neodymium.getAiPlaybook();
        playbook.setRecording(true); // Recording mode saves the prompt and logs correctly

        final PlaybookStep pbStep = playbook.getCurrentStep();

        final Method executeStepMethod = AiAgent.class.getDeclaredMethod(
            "executeStep",
            int.class, String.class, boolean.class, String.class,
            boolean.class, Long.class,
            List.class, String.class, List.class, Integer.class, String.class,
            StepDetails.class, AiExecutionResult.class
        );
        executeStepMethod.setAccessible(true);

        // This invocation should complete cleanly without throwing an exception!
        executeStepMethod.invoke(
            agent,
            0, "Click element", false, null,
            true, null,
            new ArrayList<String>(), prompt, new ArrayList<String>(), null, null,
            new StepDetails("Click element"), new AiExecutionResult(new HashMap<>())
        );

        // Verify the step was handled as optional and saved to playbook step
        assertEquals("Click element", pbStep.getPromptLine(), "Expected prompt line to be 'Click element', but got: '" + pbStep.getPromptLine() + "'");
        assertTrue(pbStep.getReasoning().contains("Optional step execution failed"), "Expected reasoning to contain 'Optional step execution failed', but got: '" + pbStep.getReasoning() + "'");
    }

    /**
     * Verifies soft execution bypassing on AssertionError.
     */
    @Test
    public void testOptionalStepBypassesAssertionError() throws Exception
    {
        final ActionExecutor failingExecutor = new ActionExecutor(null)
        {
            @Override
            public void executeAll(final List<Action> actions)
            {
                throw new AssertionError("Simulated assertion failure");
            }
        };

        final AiAgent agent = new AiAgent(dummyLlmClient, dummyAnalyzer, failingExecutor, config);
        setExecutionLog(agent);

        final String prompt = "Verify text (soft)";
        final Playbook playbook = Neodymium.getAiPlaybook();
        playbook.setRecording(true);

        final PlaybookStep pbStep = playbook.getCurrentStep();

        final Method executeStepMethod = AiAgent.class.getDeclaredMethod(
            "executeStep",
            int.class, String.class, boolean.class, String.class,
            boolean.class, Long.class,
            List.class, String.class, List.class, Integer.class, String.class,
            StepDetails.class, AiExecutionResult.class
        );
        executeStepMethod.setAccessible(true);

        // This invocation should complete cleanly without throwing an exception!
        executeStepMethod.invoke(
            agent,
            0, "Verify text", false, null,
            true, null,
            new ArrayList<String>(), prompt, new ArrayList<String>(), null, null,
            new StepDetails("Verify text"), new AiExecutionResult(new HashMap<>())
        );

        assertEquals("Verify text", pbStep.getPromptLine(), "Expected prompt line to be 'Verify text', but got: '" + pbStep.getPromptLine() + "'");
        assertTrue(pbStep.getReasoning().contains("Optional step assertion failed"), "Expected reasoning to contain 'Optional step assertion failed', but got: '" + pbStep.getReasoning() + "'");
    }

    /**
     * Verifies soft execution bypassing on generic Exception.
     */
    @Test
    public void testOptionalStepBypassesGenericException() throws Exception
    {
        final ActionExecutor failingExecutor = new ActionExecutor(null)
        {
            @Override
            public void executeAll(final List<Action> actions)
            {
                throw new RuntimeException("Simulated unexpected exception");
            }
        };

        final AiAgent agent = new AiAgent(dummyLlmClient, dummyAnalyzer, failingExecutor, config);
        setExecutionLog(agent);

        final String prompt = "Click element (optional)";
        final Playbook playbook = Neodymium.getAiPlaybook();
        playbook.setRecording(true);

        final PlaybookStep pbStep = playbook.getCurrentStep();

        final Method executeStepMethod = AiAgent.class.getDeclaredMethod(
            "executeStep",
            int.class, String.class, boolean.class, String.class,
            boolean.class, Long.class,
            List.class, String.class, List.class, Integer.class, String.class,
            StepDetails.class, AiExecutionResult.class
        );
        executeStepMethod.setAccessible(true);

        // This invocation should complete cleanly without throwing an exception!
        executeStepMethod.invoke(
            agent,
            0, "Click element", false, null,
            true, null,
            new ArrayList<String>(), prompt, new ArrayList<String>(), null, null,
            new StepDetails("Click element"), new AiExecutionResult(new HashMap<>())
        );

        assertEquals("Click element", pbStep.getPromptLine(), "Expected prompt line to be 'Click element', but got: '" + pbStep.getPromptLine() + "'");
        assertTrue(pbStep.getReasoning().contains("Optional step unexpected error"), "Expected reasoning to contain 'Optional step unexpected error', but got: '" + pbStep.getReasoning() + "'");
    }

    /**
     * Verifies that PESAP successfully parses a JSON response with LEAN predicted level.
     *
     * @throws Exception if reflection fails
     */
    @Test
    public void testPesapPredictionWithLean() throws Exception
    {
        final LlmClient mockLlm = new LlmClient(config, new AiStats())
        {
            @Override
            public String chat(final String systemPrompt, final String userPrompt)
            {
                if (systemPrompt.equals(AiAgentPrompts.PESAP_CLASSIFY_PROMPT))
                {
                    return "{\"predictions\": {\"0\": \"LEAN\"}}";
                }
                if (systemPrompt.equals(AiAgentPrompts.PESAP_LINTER_PROMPT))
                {
                    return "{\"warnings\": {\"0\": [\"Lacks element targeting\"]}}";
                }
                return "{}";
            }
        };

        final AiAgent agent = new AiAgent(mockLlm, dummyAnalyzer, null, config);
        setExecutionLog(agent);

        final Method runPesapMethod = AiAgent.class.getDeclaredMethod(
            "runPesap",
            List.class, List.class, String.class, AiExecutionResult.class
        );
        runPesapMethod.setAccessible(true);

        final List<String> steps = List.of("Click button");
        final List<Integer> lines = List.of(10);

        // This should run successfully and set predictions.get(0) to ContextLevel.LEAN
        runPesapMethod.invoke(agent, steps, lines, "test.yaml", new AiExecutionResult(new HashMap<>()));

        final Field pesapPredictionsField = AiAgent.class.getDeclaredField("pesapPredictions");
        pesapPredictionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final List<ContextLevel> predictions = (List<ContextLevel>) pesapPredictionsField.get(agent);

        assertNotNull(predictions);
        assertEquals(1, predictions.size());
        assertEquals(ContextLevel.LEAN, predictions.get(0));
    }

    /**
     * Verifies that PESAP handles and logs a malformed JSON response without crashing the test run.
     *
     * @throws Exception if reflection fails
     */
    @Test
    public void testPesapPredictionMalformedJson() throws Exception
    {
        final LlmClient mockLlm = new LlmClient(config, new AiStats())
        {
            @Override
            public String chat(final String systemPrompt, final String userPrompt)
            {
                return "{\"predictions\": \"malformed_garbage_value\"";
            }
        };

        final AiAgent agent = new AiAgent(mockLlm, dummyAnalyzer, null, config);
        setExecutionLog(agent);

        final Method runPesapMethod = AiAgent.class.getDeclaredMethod(
            "runPesap",
            List.class, List.class, String.class, AiExecutionResult.class
        );
        runPesapMethod.setAccessible(true);

        final List<String> steps = List.of("Click button");
        final List<Integer> lines = List.of(10);

        // This should not crash, it should just log the error and proceed (bypassing PESAP predictions)
        runPesapMethod.invoke(agent, steps, lines, "test.yaml", new AiExecutionResult(new HashMap<>()));

        final Field pesapPredictionsField = AiAgent.class.getDeclaredField("pesapPredictions");
        pesapPredictionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final List<ContextLevel> predictions = (List<ContextLevel>) pesapPredictionsField.get(agent);

        assertNotNull(predictions);
        assertEquals(1, predictions.size());
        assertNull(predictions.get(0)); // Should remain null due to parsing failure
    }

    /**
     * Verifies that PESAP successfully recovers partial predictions from a truncated JSON response using the regex scanner fallback.
     *
     * @throws Exception if reflection fails
     */
    @Test
    public void testPesapPredictionRegexRecovery() throws Exception
    {
        final LlmClient mockLlm = new LlmClient(config, new AiStats())
        {
            @Override
            public String chat(final String systemPrompt, final String userPrompt)
            {
                // Truncated JSON response
                return "{\"predictions\": {\"0\": \"LEAN\", \"1\": \"STANDARD\", \"2\": \"AXT";
            }
        };

        final AiAgent agent = new AiAgent(mockLlm, dummyAnalyzer, null, config);
        setExecutionLog(agent);

        final Method runPesapMethod = AiAgent.class.getDeclaredMethod(
            "runPesap",
            List.class, List.class, String.class, AiExecutionResult.class
        );
        runPesapMethod.setAccessible(true);

        final List<String> steps = List.of("Click button", "Verify text", "Hover element");
        final List<Integer> lines = List.of(10, 11, 12);

        // This should recover the first two predictions using regex fallback
        runPesapMethod.invoke(agent, steps, lines, "test.yaml", new AiExecutionResult(new HashMap<>()));

        final Field pesapPredictionsField = AiAgent.class.getDeclaredField("pesapPredictions");
        pesapPredictionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final List<ContextLevel> predictions = (List<ContextLevel>) pesapPredictionsField.get(agent);

        assertNotNull(predictions);
        assertEquals(3, predictions.size());
        assertEquals(ContextLevel.LEAN, predictions.get(0));
        assertEquals(ContextLevel.STANDARD, predictions.get(1));
        assertNull(predictions.get(2)); // The truncated one should remain null
    }

    /**
     * Verifies that when only classification is enabled, only classification is called.
     *
     * @throws Exception if reflection fails
     */
    @Test
    public void testPesapWithOnlyClassificationEnabled() throws Exception
    {
        config.setProperty("neodymium.ai.pesap.classify.enabled", "true");
        config.setProperty("neodymium.ai.pesap.linter.enabled", "false");

        final AtomicInteger classifyCallCount = new AtomicInteger(0);
        final AtomicInteger linterCallCount = new AtomicInteger(0);

        try
        {
            final LlmClient mockLlm = new LlmClient(config, new AiStats())
            {
                @Override
                public String chat(final String systemPrompt, final String userPrompt)
                {
                    if (systemPrompt.equals(AiAgentPrompts.PESAP_CLASSIFY_PROMPT))
                    {
                        classifyCallCount.incrementAndGet();
                        return "{\"predictions\": {\"0\": \"LEAN\"}}";
                    }
                    if (systemPrompt.equals(AiAgentPrompts.PESAP_LINTER_PROMPT))
                    {
                        linterCallCount.incrementAndGet();
                        return "{\"warnings\": {\"0\": [\"Lacks element targeting\"]}}";
                    }
                    return "{}";
                }
            };

            final AiAgent agent = new AiAgent(mockLlm, dummyAnalyzer, null, config);
            setExecutionLog(agent);

            final Method runPesapMethod = AiAgent.class.getDeclaredMethod(
                "runPesap",
                List.class, List.class, String.class, AiExecutionResult.class
            );
            runPesapMethod.setAccessible(true);

            final List<String> steps = List.of("Click button");
            final List<Integer> lines = List.of(10);

            runPesapMethod.invoke(agent, steps, lines, "test.yaml", new AiExecutionResult(new HashMap<>()));

            final Field pesapPredictionsField = AiAgent.class.getDeclaredField("pesapPredictions");
            pesapPredictionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            final List<ContextLevel> predictions = (List<ContextLevel>) pesapPredictionsField.get(agent);

            assertNotNull(predictions);
            assertEquals(1, predictions.size());
            assertEquals(ContextLevel.LEAN, predictions.get(0));

            assertEquals(1, classifyCallCount.get());
            assertEquals(0, linterCallCount.get());
        }
        finally
        {
            config.removeProperty("neodymium.ai.pesap.classify.enabled");
            config.removeProperty("neodymium.ai.pesap.linter.enabled");
        }
    }

    /**
     * Verifies that when only linter is enabled, only linter is called.
     *
     * @throws Exception if reflection fails
     */
    @Test
    public void testPesapWithOnlyLinterEnabled() throws Exception
    {
        config.setProperty("neodymium.ai.pesap.classify.enabled", "false");
        config.setProperty("neodymium.ai.pesap.linter.enabled", "true");

        final AtomicInteger classifyCallCount = new AtomicInteger(0);
        final AtomicInteger linterCallCount = new AtomicInteger(0);

        try
        {
            final LlmClient mockLlm = new LlmClient(config, new AiStats())
            {
                @Override
                public String chat(final String systemPrompt, final String userPrompt)
                {
                    if (systemPrompt.equals(AiAgentPrompts.PESAP_CLASSIFY_PROMPT))
                    {
                        classifyCallCount.incrementAndGet();
                        return "{\"predictions\": {\"0\": \"LEAN\"}}";
                    }
                    if (systemPrompt.equals(AiAgentPrompts.PESAP_LINTER_PROMPT))
                    {
                        linterCallCount.incrementAndGet();
                        return "{\"warnings\": {\"0\": [\"Lacks element targeting\"]}}";
                    }
                    return "{}";
                }
            };

            final AiAgent agent = new AiAgent(mockLlm, dummyAnalyzer, null, config);
            setExecutionLog(agent);

            final Method runPesapMethod = AiAgent.class.getDeclaredMethod(
                "runPesap",
                List.class, List.class, String.class, AiExecutionResult.class
            );
            runPesapMethod.setAccessible(true);

            final List<String> steps = List.of("Click button");
            final List<Integer> lines = List.of(10);

            runPesapMethod.invoke(agent, steps, lines, "test.yaml", new AiExecutionResult(new HashMap<>()));

            final Field pesapPredictionsField = AiAgent.class.getDeclaredField("pesapPredictions");
            pesapPredictionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            final List<ContextLevel> predictions = (List<ContextLevel>) pesapPredictionsField.get(agent);

            // Classification was disabled, so predictions should remain null
            assertNotNull(predictions);
            assertEquals(1, predictions.size());
            assertNull(predictions.get(0));

            assertEquals(0, classifyCallCount.get());
            assertEquals(1, linterCallCount.get());
        }
        finally
        {
            config.removeProperty("neodymium.ai.pesap.classify.enabled");
            config.removeProperty("neodymium.ai.pesap.linter.enabled");
        }
    }

    /**
     * Verifies that when both are disabled, no chat calls are made.
     *
     * @throws Exception if reflection fails
     */
    @Test
    public void testPesapWithBothDisabled() throws Exception
    {
        config.setProperty("neodymium.ai.pesap.classify.enabled", "false");
        config.setProperty("neodymium.ai.pesap.linter.enabled", "false");

        final AtomicInteger classifyCallCount = new AtomicInteger(0);
        final AtomicInteger linterCallCount = new AtomicInteger(0);

        try
        {
            final LlmClient mockLlm = new LlmClient(config, new AiStats())
            {
                @Override
                public String chat(final String systemPrompt, final String userPrompt)
                {
                    if (systemPrompt.equals(AiAgentPrompts.PESAP_CLASSIFY_PROMPT))
                    {
                        classifyCallCount.incrementAndGet();
                    }
                    if (systemPrompt.equals(AiAgentPrompts.PESAP_LINTER_PROMPT))
                    {
                        linterCallCount.incrementAndGet();
                    }
                    return "{}";
                }
            };

            final AiAgent agent = new AiAgent(mockLlm, dummyAnalyzer, null, config);
            setExecutionLog(agent);

            final Method runPesapMethod = AiAgent.class.getDeclaredMethod(
                "runPesap",
                List.class, List.class, String.class, AiExecutionResult.class
            );
            runPesapMethod.setAccessible(true);

            final List<String> steps = List.of("Click button");
            final List<Integer> lines = List.of(10);

            runPesapMethod.invoke(agent, steps, lines, "test.yaml", new AiExecutionResult(new HashMap<>()));

            assertEquals(0, classifyCallCount.get());
            assertEquals(0, linterCallCount.get());
        }
        finally
        {
            config.removeProperty("neodymium.ai.pesap.classify.enabled");
            config.removeProperty("neodymium.ai.pesap.linter.enabled");
        }
    }

    /**
     * Verifies that the PESAP user prompt is formatted as a clean 0-based indexed plain text list.
     *
     * @throws Exception if reflection fails
     */
    @Test
    public void testPesapUserPromptFormatting() throws Exception
    {
        final AtomicReference<String> capturedUserPrompt = new AtomicReference<>();

        final LlmClient mockLlm = new LlmClient(config, new AiStats())
        {
            @Override
            public String chat(final String systemPrompt, final String userPrompt)
            {
                capturedUserPrompt.set(userPrompt);
                return "{}";
            }
        };

        final AiAgent agent = new AiAgent(mockLlm, dummyAnalyzer, null, config);
        setExecutionLog(agent);

        final Method runPesapMethod = AiAgent.class.getDeclaredMethod(
            "runPesap",
            List.class, List.class, String.class, AiExecutionResult.class
        );
        runPesapMethod.setAccessible(true);

        final List<String> steps = List.of("Click button", "Verify text");
        final List<Integer> lines = List.of(10, 11);

        runPesapMethod.invoke(agent, steps, lines, "test.yaml", new AiExecutionResult(new HashMap<>()));

        final String expectedPrompt = "## Test Steps\n0: Click button\n1: Verify text\n";
        assertEquals(expectedPrompt, capturedUserPrompt.get());
    }

    private void setExecutionLog(final AiAgent agent) throws Exception
    {
        final Field logField = AiAgent.class.getDeclaredField("executionLog");
        logField.setAccessible(true);
        logField.set(agent, new AiDiscussionLogger("dummy instructions"));
    }
}

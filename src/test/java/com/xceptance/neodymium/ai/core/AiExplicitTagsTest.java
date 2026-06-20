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

/**
 * Tests for the explicit language tags features in {@link AiAgent}:
 * - Tag parsing & clean tag stripping architecture.
 * - Soft/Optional step execution failure bypassing.
 * - Timeout isolation under all outcomes.
 * 
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
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
        assertEquals("Observe visual consistency", AiAgent.stripAllTags("Observe visual consistency (no-replay)"));
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
            List.class, String.class, List.class, String.class, String.class,
            StepDetails.class, AiExecutionResult.class,
            List.class, List.class
        );
        executeStepMethod.setAccessible(true);

        final String stepText = "Click element";
        executeStepMethod.invoke(
            agent,
            0, stepText, false, null,
            false, 500L,
            new ArrayList<String>(), prompt, new ArrayList<String>(), null, null,
            new StepDetails(stepText), new AiExecutionResult(new HashMap<>()),
            new ArrayList<String>(), new ArrayList<Integer>()
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
            List.class, String.class, List.class, String.class, String.class,
            StepDetails.class, AiExecutionResult.class,
            List.class, List.class
        );
        executeStepMethod.setAccessible(true);

        final String stepText = "Click element";
        try
        {
            executeStepMethod.invoke(
                agent,
                0, stepText, false, null,
                false, 2000L,
                new ArrayList<String>(), prompt, new ArrayList<String>(), null, null,
                new StepDetails(stepText), new AiExecutionResult(new HashMap<>()),
                new ArrayList<String>(), new ArrayList<Integer>()
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
            List.class, String.class, List.class, String.class, String.class,
            StepDetails.class, AiExecutionResult.class,
            List.class, List.class
        );
        executeStepMethod.setAccessible(true);

        final String stepText = "Click element";
        // This invocation should complete cleanly without throwing an exception!
        executeStepMethod.invoke(
            agent,
            0, stepText, false, null,
            true, null,
            new ArrayList<String>(), prompt, new ArrayList<String>(), null, null,
            new StepDetails(stepText), new AiExecutionResult(new HashMap<>()),
            new ArrayList<String>(), new ArrayList<Integer>()
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
            List.class, String.class, List.class, String.class, String.class,
            StepDetails.class, AiExecutionResult.class,
            List.class, List.class
        );
        executeStepMethod.setAccessible(true);

        final String stepText = "Verify text";
        // This invocation should complete cleanly without throwing an exception!
        executeStepMethod.invoke(
            agent,
            0, stepText, false, null,
            true, null,
            new ArrayList<String>(), prompt, new ArrayList<String>(), null, null,
            new StepDetails(stepText), new AiExecutionResult(new HashMap<>()),
            new ArrayList<String>(), new ArrayList<Integer>()
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
            List.class, String.class, List.class, String.class, String.class,
            StepDetails.class, AiExecutionResult.class,
            List.class, List.class
        );
        executeStepMethod.setAccessible(true);

        final String stepText = "Click element";
        // This invocation should complete cleanly without throwing an exception!
        executeStepMethod.invoke(
            agent,
            0, stepText, false, null,
            true, null,
            new ArrayList<String>(), prompt, new ArrayList<String>(), null, null,
            new StepDetails(stepText), new AiExecutionResult(new HashMap<>()),
            new ArrayList<String>(), new ArrayList<Integer>()
        );

        assertEquals("Click element", pbStep.getPromptLine(), "Expected prompt line to be 'Click element', but got: '" + pbStep.getPromptLine() + "'");
        assertTrue(pbStep.getReasoning().contains("Optional step unexpected error"), "Expected reasoning to contain 'Optional step unexpected error', but got: '" + pbStep.getReasoning() + "'");
    }

    /**
     * Verifies that JIT pre-step PESAP successfully parses a JSON response
     * and populates the cache and StepDetails.
     *
     * @throws Exception if reflection fails
     */
    @Test
    public void testJitPreStepPesapPrediction() throws Exception
    {
        final LlmClient mockLlm = new LlmClient(config, new AiStats())
        {
            @Override
            public String chat(final LlmMode mode, final String systemPrompt, final String userPrompt)
            {
                return "{\"c\": \"STANDARD\", \"jm\": false, \"sp\": []}";
            }

            @Override
            public String chat(final String systemPrompt, final String userPrompt)
            {
                return chat(LlmMode.PESAP, systemPrompt, userPrompt);
            }
        };

        final ActionExecutor dummyExecutor = new ActionExecutor(null);
        final AiAgent agent = new AiAgent(mockLlm, dummyAnalyzer, dummyExecutor, config);
        setExecutionLog(agent);

        // Set the currentStepsList field via reflection
        final Field stepsField = AiAgent.class.getDeclaredField("currentStepsList");
        stepsField.setAccessible(true);
        final StepDetails stepDetails = new StepDetails("Verify the price is greater than zero");
        stepsField.set(agent, List.of("Open the homepage", stepDetails.getRawInstruction(), "Click checkout"));

        // Invoke runPreStepPesap via reflection
        final Method runPreStepPesapMethod = AiAgent.class.getDeclaredMethod(
            "runPreStepPesap",
            int.class, Class.class, StepDetails.class
        );
        runPreStepPesapMethod.setAccessible(true);

        final Object result = runPreStepPesapMethod.invoke(agent, 1, null, stepDetails);

        assertNotNull(result, "Expected JIT PESAP to return a non-null result");
        assertEquals(ContextLevel.STANDARD, stepDetails.getPesapPredictedContextLevel());

        // Verify the cache is populated
        assertTrue(stepDetails.isPesapCalled(), "Expected JIT PESAP call flag to be true");
    }

    /**
     * Verifies that JIT pre-step PESAP gracefully handles malformed JSON
     * and returns null without crashing.
     *
     * @throws Exception if reflection fails
     */
    @Test
    public void testJitPreStepPesapMalformedJson() throws Exception
    {
        final LlmClient mockLlm = new LlmClient(config, new AiStats())
        {
            @Override
            public String chat(final LlmMode mode, final String systemPrompt, final String userPrompt)
            {
                return "{\"c\": \"malformed_garbage_value\"";
            }

            @Override
            public String chat(final String systemPrompt, final String userPrompt)
            {
                return chat(LlmMode.PESAP, systemPrompt, userPrompt);
            }
        };

        final ActionExecutor dummyExecutor = new ActionExecutor(null);
        final AiAgent agent = new AiAgent(mockLlm, dummyAnalyzer, dummyExecutor, config);
        setExecutionLog(agent);

        final Field stepsField = AiAgent.class.getDeclaredField("currentStepsList");
        stepsField.setAccessible(true);
        final StepDetails stepDetails = new StepDetails("Click button");
        stepsField.set(agent, List.of(stepDetails.getRawInstruction()));

        final Method runPreStepPesapMethod = AiAgent.class.getDeclaredMethod(
            "runPreStepPesap",
            int.class, Class.class, StepDetails.class
        );
        runPreStepPesapMethod.setAccessible(true);

        final Object result = runPreStepPesapMethod.invoke(agent, 0, null, stepDetails);

        // Should return null on failure (graceful fallback)
        assertNull(result, "Expected JIT PESAP to return null on malformed JSON");
    }

    /**
     * Verifies that the JIT pre-step PESAP builds the correct flow context window
     * format: 1 previous / current / 2 next.
     *
     * @throws Exception if reflection fails
     */
    @Test
    public void testJitPreStepPesapFlowContextFormat() throws Exception
    {
        final AtomicReference<String> capturedUserPrompt = new AtomicReference<>();

        final LlmClient mockLlm = new LlmClient(config, new AiStats())
        {
            @Override
            public String chat(final LlmMode mode, final String systemPrompt, final String userPrompt)
            {
                capturedUserPrompt.set(userPrompt);
                return "{\"c\": \"AXTREE\", \"jm\": false, \"sp\": []}";
            }

            @Override
            public String chat(final String systemPrompt, final String userPrompt)
            {
                return chat(LlmMode.PESAP, systemPrompt, userPrompt);
            }
        };

        final ActionExecutor dummyExecutor = new ActionExecutor(null);
        final AiAgent agent = new AiAgent(mockLlm, dummyAnalyzer, dummyExecutor, config);
        setExecutionLog(agent);

        final Field stepsField = AiAgent.class.getDeclaredField("currentStepsList");
        stepsField.setAccessible(true);
        final StepDetails stepDetails = new StepDetails("Click the first product");
        stepsField.set(agent, List.of(
            "Search for Screwdriver",
            stepDetails.getRawInstruction(),
            "Add to cart",
            "Go to checkout",
            "Enter payment details"
        ));

        final Method runPreStepPesapMethod = AiAgent.class.getDeclaredMethod(
            "runPreStepPesap",
            int.class, Class.class, StepDetails.class
        );
        runPreStepPesapMethod.setAccessible(true);

        runPreStepPesapMethod.invoke(agent, 1, null, stepDetails);

        final String prompt = capturedUserPrompt.get();
        assertNotNull(prompt, "Expected user prompt to be captured");

        // Verify flow context format: 1 previous + current + 2 next
        assertTrue(prompt.contains("[PREVIOUS]"), "Expected [PREVIOUS] tag in flow context");
        assertTrue(prompt.contains("[CURRENT]"), "Expected [CURRENT] tag in flow context");
        assertTrue(prompt.contains("[NEXT]"), "Expected [NEXT] tag in flow context");
        assertTrue(prompt.contains("Search for Screwdriver"), "Expected previous step content");
        assertTrue(prompt.contains("Click the first product"), "Expected current step content");
        assertTrue(prompt.contains("Add to cart"), "Expected next step 1 content");
        assertTrue(prompt.contains("Go to checkout"), "Expected next step 2 content");
        assertFalse(prompt.contains("Enter payment details"), "Expected step beyond window to be excluded");
    }

    private void setExecutionLog(final AiAgent agent) throws Exception
    {
        final Field logField = AiAgent.class.getDeclaredField("executionLog");
        logField.setAccessible(true);
        logField.set(agent, new AiDiscussionLogger("dummy instructions"));
    }

    /**
     * Verifies that the (no-replay) tag successfully bypasses replay loading
     * and forces live execution (which triggers identifyActions or the LLM).
     */
    @Test
    public void testNoReplayBypassesPlaybookReplay() throws Exception
    {
        final List<Action> executedActions = new ArrayList<>();
        final ActionExecutor recordingExecutor = new ActionExecutor(null)
        {
            @Override
            public void executeAll(final List<Action> actions)
            {
                executedActions.addAll(actions);
            }
        };

        final AiAgent agent = new AiAgent(dummyLlmClient, dummyAnalyzer, recordingExecutor, config);
        setExecutionLog(agent);

        final String prompt = "Click element (no-replay)";
        final Playbook playbook = Neodymium.getAiPlaybook();
        playbook.setRecording(false); // Replay mode

        // Cache actions in the playbook step
        final PlaybookStep pbStep = playbook.getCurrentStep();
        pbStep.setPromptLine("Click element");
        pbStep.setActions(List.of(new Action("CLICK", "#cached-btn", "Cached click")));

        final Method getStepActionsMethod = AiAgent.class.getDeclaredMethod(
            "getStepActions",
            int.class, String.class, String.class, Playbook.class,
            boolean.class, String.class, List.class,
            StepDetails.class, AiExecutionResult.class
        );
        getStepActionsMethod.setAccessible(true);

        final String stepText = "Click element";
        @SuppressWarnings("unchecked")
        final List<Action> actions = (List<Action>) getStepActionsMethod.invoke(
            agent,
            0, stepText, prompt, playbook,
            false, null, new ArrayList<Action>(),
            new StepDetails(stepText), new AiExecutionResult(new HashMap<>())
        );

        // Verify that the cached action "#cached-btn" was bypassed,
        // and instead the live LLM actions (from dummyLlmClient: "#dummy-btn") were returned!
        assertNotNull(actions, "Expected actions to be resolved");
        assertFalse(actions.isEmpty(), "Expected actions list to not be empty");
        assertEquals("#dummy-btn", actions.get(0).getTarget(), "Expected LLM resolved action target");
    }
}

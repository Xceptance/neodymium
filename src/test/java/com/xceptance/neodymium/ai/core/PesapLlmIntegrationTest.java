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
package com.xceptance.neodymium.ai.core;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.BaseLlmTest;
import com.xceptance.neodymium.ai.config.AiConfiguration;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Live LLM integration tests for JIT Pre-Step PESAP predictions.
 * Verifies that prompt optimizations do not degrade prediction accuracy.
 * 
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PesapLlmIntegrationTest extends BaseLlmTest
{
    private final AiConfiguration config;
    private final LlmClient liveLlmClient;
    private final PageAnalyzer dummyAnalyzer;

    /**
     * Constructs a new PesapLlmIntegrationTest and initializes fields.
     */
    public PesapLlmIntegrationTest()
    {
        this.config = Neodymium.aiConfiguration();
        this.liveLlmClient = new LlmClient(this.config, new AiStats());
        this.dummyAnalyzer = new PageAnalyzer()
        {
            @Override
            public final String getPageContext(final ContextLevel level)
            {
                return "";
            }

            @Override
            public final String captureScreenshot(final String title)
            {
                return "";
            }
        };
    }

    /**
     * Tests a standard interaction step with a button target.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testInteractionButton() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Click the 'Add to Cart' button";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertEquals(ContextLevel.AXTREE, result.contextLevel());
        Assertions.assertFalse(result.requiresJavaMethods());
        Assertions.assertTrue(result.splitSteps().isEmpty());

        // Validate stepDetails properties
        Assertions.assertEquals(ContextLevel.AXTREE, stepDetails.getPesapPredictedContextLevel());
        Assertions.assertFalse(stepDetails.isPesapRequiresJavaMethods());
    }

    /**
     * Tests a standard interaction step with an input target.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testInteractionInput() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Enter 'hello' into search input";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertEquals(ContextLevel.AXTREE, result.contextLevel());
        Assertions.assertFalse(result.requiresJavaMethods());
        Assertions.assertTrue(result.splitSteps().isEmpty());

        // Validate stepDetails properties
        Assertions.assertEquals(ContextLevel.AXTREE, stepDetails.getPesapPredictedContextLevel());
        Assertions.assertFalse(stepDetails.isPesapRequiresJavaMethods());
    }

    /**
     * Tests an assertion step checking visible text.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testAssertionText() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Verify that the error message 'Invalid email' is visible";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertEquals(ContextLevel.STANDARD, result.contextLevel());
        Assertions.assertFalse(result.requiresJavaMethods());
        Assertions.assertTrue(result.splitSteps().isEmpty());

        // Validate stepDetails properties
        Assertions.assertEquals(ContextLevel.STANDARD, stepDetails.getPesapPredictedContextLevel());
        Assertions.assertFalse(stepDetails.isPesapRequiresJavaMethods());
    }

    /**
     * Tests an assertion step checking visual appearance and layout.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testAssertionLayout() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Verify the layout of the product grid looks correct";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        final ContextLevel level = result.contextLevel();
        Assertions.assertEquals(ContextLevel.VISUAL_LEAN, level);
        Assertions.assertTrue(result.splitSteps().isEmpty());
        Assertions.assertEquals(result.requiresJavaMethods(), stepDetails.isPesapRequiresJavaMethods());

        // Validate stepDetails properties
        Assertions.assertEquals(level, stepDetails.getPesapPredictedContextLevel());
    }


    /**
     * We hint visual and that should be VISUAL_LEAN
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testVisualHintLayout() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Check that you see a black background and white dots (visual)";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertEquals(ContextLevel.VISUAL_LEAN, result.contextLevel());
        Assertions.assertTrue(result.splitSteps().isEmpty());
        Assertions.assertEquals(result.requiresJavaMethods(), stepDetails.isPesapRequiresJavaMethods());

        // Validate stepDetails properties
        Assertions.assertEquals(result.contextLevel(), stepDetails.getPesapPredictedContextLevel());
    }

    /**
     * We hint layout and that should be VISUAL.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testLayoutHint() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Check that the table columns align with headers (layout)";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertEquals(ContextLevel.VISUAL, result.contextLevel());
        Assertions.assertTrue(result.splitSteps().isEmpty());
        Assertions.assertEquals(result.requiresJavaMethods(), stepDetails.isPesapRequiresJavaMethods());

        // Validate stepDetails properties
        Assertions.assertEquals(result.contextLevel(), stepDetails.getPesapPredictedContextLevel());
    }

    /**
     * Tests that a compound step representing related form entries is NOT split.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testFormEntryNoSplit() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Type 'admin' in email, 'secret' in password and click login";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertNotNull(result.contextLevel());
        Assertions.assertEquals(result.requiresJavaMethods(), stepDetails.isPesapRequiresJavaMethods());
        Assertions.assertTrue(result.splitSteps().isEmpty());

        // Validate stepDetails properties
        Assertions.assertEquals(result.contextLevel(), stepDetails.getPesapPredictedContextLevel());
    }

    /**
     * Tests that a compound step representing sequential page state transitions (e.g. dropdown menu) IS split.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testMenuDropdownSplit() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Click the user profile menu and then click 'Sign out'";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertNotNull(result.contextLevel());
        Assertions.assertEquals(result.requiresJavaMethods(), stepDetails.isPesapRequiresJavaMethods());
        Assertions.assertNotNull(result.splitSteps(), "Expected steps to be split");
        Assertions.assertTrue(result.splitSteps().size() >= 2,
            "Expected at least 2 split steps, but got: " + result.splitSteps());

        // Validate stepDetails properties
        Assertions.assertEquals(result.contextLevel(), stepDetails.getPesapPredictedContextLevel());
    }

    /**
     * Tests that a compound step containing an assertion sequence IS split.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testSequenceWithVerifySplit() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Click on 'Add to cart', verify that cart count is 1, and then proceed to checkout";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertNotNull(result.contextLevel());
        Assertions.assertEquals(result.requiresJavaMethods(), stepDetails.isPesapRequiresJavaMethods());
        Assertions.assertNotNull(result.splitSteps(), "Expected steps to be split");
        Assertions.assertTrue(result.splitSteps().size() >= 3,
            "Expected at least 3 split steps, but got: " + result.splitSteps());

        // Validate stepDetails properties
        Assertions.assertEquals(result.contextLevel(), stepDetails.getPesapPredictedContextLevel());
    }

    /**
     * Tests that a compound step containing an assertion sequence IS split and the hints are recognized.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testSequenceWithSpliAndHint() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Click on 'Add to cart' (hint: #addtocart), verify that cart count is 1 (hint: .mini-cart), and then proceed to checkout (hint: #checkout)";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertNotNull(result.contextLevel());
        Assertions.assertEquals(result.requiresJavaMethods(), stepDetails.isPesapRequiresJavaMethods());
        Assertions.assertEquals(3, result.splitSteps().size());

        // Validate stepDetails properties
        Assertions.assertEquals(result.contextLevel(), stepDetails.getPesapPredictedContextLevel());
    }

    /**
     * Tests a step that requires a custom Java assertion method.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testRequiresJavaMethodsTrue() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Verify the calculations using the custom Java method assertPriceGreaterThanZero";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertEquals(ContextLevel.STANDARD, result.contextLevel());
        Assertions.assertTrue(result.requiresJavaMethods());
        Assertions.assertTrue(result.splitSteps().isEmpty());

        // Validate stepDetails properties
        Assertions.assertEquals(ContextLevel.STANDARD, stepDetails.getPesapPredictedContextLevel());
        Assertions.assertTrue(stepDetails.isPesapRequiresJavaMethods());
    }

    /**
     * Tests a rather ambiguous step for Java methods.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testJavaMethodAmbiguousStep() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Verify the calculations using method assertPriceGreaterThanZero";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertEquals(ContextLevel.STANDARD, result.contextLevel());
        Assertions.assertTrue(result.requiresJavaMethods());
        Assertions.assertTrue(result.splitSteps().isEmpty());

        // Validate stepDetails properties
        Assertions.assertEquals(ContextLevel.STANDARD, stepDetails.getPesapPredictedContextLevel());
        Assertions.assertTrue(stepDetails.isPesapRequiresJavaMethods());
    }

    /**
     * Tests a rather ambiguous step for Java methods.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testJavaMethodAmbiguousStep2() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Verify the calculations using `assertPriceGreaterThanZero`";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertTrue(result.contextLevel() == ContextLevel.AXTREE || result.contextLevel() == ContextLevel.STANDARD);
        Assertions.assertTrue(result.requiresJavaMethods());
        Assertions.assertTrue(result.splitSteps().isEmpty());

        // Validate stepDetails properties
        Assertions.assertTrue(stepDetails.getPesapPredictedContextLevel() == ContextLevel.AXTREE || stepDetails.getPesapPredictedContextLevel() == ContextLevel.STANDARD);
        Assertions.assertTrue(stepDetails.isPesapRequiresJavaMethods());
    }

    /**
     * Tests that a compound step containing an assertion sequence IS split.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testSequenceWithSpliAndMethods() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Click on 'Add to cart' (hint: #addtocart), verify that cart count is 1 (hint: .mini-cart), and then proceed to checkout (hint: #checkout)";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertNotNull(result.contextLevel());
        Assertions.assertEquals(result.requiresJavaMethods(), stepDetails.isPesapRequiresJavaMethods());
        Assertions.assertNotNull(result.splitSteps(), "Expected steps to be split");
        Assertions.assertTrue(result.splitSteps().size() >= 3,
            "Expected at least 3 split steps, but got: " + result.splitSteps());

        // Validate stepDetails properties
        Assertions.assertEquals(result.contextLevel(), stepDetails.getPesapPredictedContextLevel());
    }

    /**
     * Tests a step that does NOT require custom Java methods.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testRequiresJavaMethodsFalse() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Click the 'Login' button";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertEquals(ContextLevel.AXTREE, result.contextLevel());
        Assertions.assertFalse(result.requiresJavaMethods());
        Assertions.assertTrue(result.splitSteps().isEmpty());

        // Validate stepDetails properties
        Assertions.assertEquals(ContextLevel.AXTREE, stepDetails.getPesapPredictedContextLevel());
        Assertions.assertFalse(stepDetails.isPesapRequiresJavaMethods());
    }

    /**
     * Tests a navigation step.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testNavigationStep() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Open http://localhost/home";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertEquals(ContextLevel.AXTREE, result.contextLevel());
        Assertions.assertFalse(result.requiresJavaMethods());
        Assertions.assertTrue(result.splitSteps().isEmpty());

        // Validate stepDetails properties
        Assertions.assertEquals(ContextLevel.AXTREE, stepDetails.getPesapPredictedContextLevel());
        Assertions.assertFalse(stepDetails.isPesapRequiresJavaMethods());
        Assertions.assertNotNull(stepDetails.getPesapCall());
        Assertions.assertEquals(LlmMode.PESAP, stepDetails.getPesapCall().getCallMode());
    }

    /**
     * Tests that a visual observation / layout assertion step does NOT require custom Java methods.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testVisualObserveRequiresJavaMethodsFalse() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Observe page visual consistency (layout). Assert that the description paragraph inside the 'Clipped Content & Text Boundaries' card is fully visible, and that NO words or lines of text are sliced horizontally or truncated at the bottom of the container.";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertEquals(ContextLevel.VISUAL, result.contextLevel());
        Assertions.assertFalse(result.requiresJavaMethods());
        Assertions.assertTrue(result.splitSteps().isEmpty());

        // Validate stepDetails properties
        Assertions.assertEquals(ContextLevel.VISUAL, stepDetails.getPesapPredictedContextLevel());
        Assertions.assertFalse(stepDetails.isPesapRequiresJavaMethods());
    }

    /**
     * Tests a step that validates calculations but does NOT name a specific custom Java method.
     * Under Rule 3, requiresJavaMethods must be false.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testCalculationsWithoutJavaMethodName() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Verify that the total price displayed on the page is $100.00.";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertEquals(ContextLevel.STANDARD, result.contextLevel());
        Assertions.assertFalse(result.requiresJavaMethods());

        // Validate stepDetails properties
        Assertions.assertEquals(ContextLevel.STANDARD, stepDetails.getPesapPredictedContextLevel());
        Assertions.assertFalse(stepDetails.isPesapRequiresJavaMethods());
    }

    /**
     * Tests that a step with (visual) tag that also asserts specific text content resolves to VISUAL.
     * Under Rule 6, the presence of specific text to assert overrides the default VISUAL_LEAN context level.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testVisualTagWithTextAssertion() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Verify that the header text 'Order Confirmed' is visible (visual)";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertEquals(ContextLevel.VISUAL, result.contextLevel());
        Assertions.assertFalse(result.requiresJavaMethods());

        // Validate stepDetails properties
        Assertions.assertEquals(ContextLevel.VISUAL, stepDetails.getPesapPredictedContextLevel());
        Assertions.assertFalse(stepDetails.isPesapRequiresJavaMethods());
    }

    /**
     * Tests that a step containing hints is predicted as HINT context level by PESAP.
     * 
     * @throws Exception if JIT pre-step PESAP fails
     */
    @Test
    public final void testHintStepContextLevel() throws Exception
    {
        final AiAgent agent = new AiAgent(this.liveLlmClient, this.dummyAnalyzer, null, this.config);
        final String stepText = "Click the 'Login' button (hint: #login-btn)";
        final List<String> steps = List.of(stepText);
        final StepDetails stepDetails = new StepDetails(stepText);

        final AiAgent.PreStepPesapResult result = agent.runPreStepPesapForTest(steps, 0, stepDetails);

        // Validate result record properties
        Assertions.assertEquals(ContextLevel.HINT, result.contextLevel());
        Assertions.assertFalse(result.requiresJavaMethods());
        Assertions.assertTrue(result.splitSteps().isEmpty());

        // Validate stepDetails properties
        Assertions.assertEquals(ContextLevel.HINT, stepDetails.getPesapPredictedContextLevel());
        Assertions.assertFalse(stepDetails.isPesapRequiresJavaMethods());
    }
}

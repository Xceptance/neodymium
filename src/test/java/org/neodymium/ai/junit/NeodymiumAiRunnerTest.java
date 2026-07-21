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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;

/**
 * Unit and integration tests for {@link NeodymiumAiRunner} validating JUnit 5 integration.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class NeodymiumAiRunnerTest
{
    /**
     * Constructs a default NeodymiumAiRunnerTest.
     */
    public NeodymiumAiRunnerTest()
    {
    }

    @Test
    public void testConventionBasedPlaybookAndParameterInjection()
    {
        ConventionTest.executionCount = 0;
        ConventionTest.resolvedParamValues.clear();

        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(ConventionTest.class))
            .build();
        final Launcher launcher = LauncherFactory.create();
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        for (final org.junit.platform.launcher.listeners.TestExecutionSummary.Failure failure : listener.getSummary().getFailures())
        {
            System.err.println("LAUNCHER FAILURE in Convention: " + failure.getException().getMessage());
            failure.getException().printStackTrace();
        }

        // Verify JUnit ran successfully
        assertEquals(0, listener.getSummary().getTestsFailedCount(), "No tests should fail");
        assertEquals(2, listener.getSummary().getTestsSucceededCount(), "Two dataset invocations should succeed");

        // Verify playbook and datasets execution
        assertEquals(2, ConventionTest.executionCount);
        assertTrue(ConventionTest.resolvedParamValues.contains("value1"));
        assertTrue(ConventionTest.resolvedParamValues.contains("value2"));
    }

    @Test
    public void testExplicitPlaybookAndDataSetFiltering()
    {
        ExplicitTest.executionCount = 0;
        ExplicitTest.resolvedParamValues.clear();

        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(ExplicitTest.class))
            .build();
        final Launcher launcher = LauncherFactory.create();
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        for (final org.junit.platform.launcher.listeners.TestExecutionSummary.Failure failure : listener.getSummary().getFailures())
        {
            System.err.println("LAUNCHER FAILURE in Explicit: " + failure.getException().getMessage());
            failure.getException().printStackTrace();
        }

        // Verify JUnit ran successfully
        assertEquals(0, listener.getSummary().getTestsFailedCount(), "No tests should fail");
        assertEquals(1, listener.getSummary().getTestsSucceededCount(), "One filtered dataset invocation should succeed");

        // Verify playbook and datasets execution
        assertEquals(1, ExplicitTest.executionCount);
        assertEquals("foo", ExplicitTest.resolvedParamValues.get(0));
    }

    @NeodymiumAiTest
    public static class ConventionTest
    {
        public static int executionCount = 0;
        public static final List<String> resolvedParamValues = new ArrayList<>();

        @BeforeEach
        public void setup(final AiSession session)
        {
            final MockLlmProvider mock = new MockLlmProvider();
            for (final LlmCapability cap : LlmCapability.values())
            {
                session.getLlmRegistry().registerProvider(cap, mock);
            }
            // Step 1: Action extraction + Verification
            mock.addResponse(new LlmResponse("{\"actions\":[]}", null, "mock"));
            mock.addResponse(new LlmResponse("{\"rubrics\":{\"intentMatch\":{\"analysis\":\"step 1 verified\",\"score\":\"PASS\"},\"visualDelta\":{\"analysis\":\"step 1 verified\",\"score\":\"PASS\"},\"absenceOfErrors\":{\"analysis\":\"step 1 verified\",\"score\":\"PASS\"}},\"overallVerdict\":{\"passed\":true,\"summary\":\"step 1 verified\"}}", null, "mock"));
            // Step 2: Action extraction + Verification
            mock.addResponse(new LlmResponse("{\"actions\":[]}", null, "mock"));
            mock.addResponse(new LlmResponse("{\"rubrics\":{\"intentMatch\":{\"analysis\":\"step 2 verified\",\"score\":\"PASS\"},\"visualDelta\":{\"analysis\":\"step 2 verified\",\"score\":\"PASS\"},\"absenceOfErrors\":{\"analysis\":\"step 2 verified\",\"score\":\"PASS\"}},\"overallVerdict\":{\"passed\":true,\"summary\":\"step 2 verified\"}}", null, "mock"));
        }

        @AiPlaybook
        public void myTestMethod(final AiSession session, final ExecutionContext context)
        {
            executionCount++;
            final Object paramVal = session.getExecutionContext().getSessionData().get("param");
            resolvedParamValues.add(String.valueOf(paramVal));
        }
    }

    @NeodymiumAiTest
    public static class ExplicitTest
    {
        public static int executionCount = 0;
        public static final List<String> resolvedParamValues = new ArrayList<>();

        @BeforeEach
        public void setup(final AiSession session)
        {
            final MockLlmProvider mock = new MockLlmProvider();
            for (final LlmCapability cap : LlmCapability.values())
            {
                session.getLlmRegistry().registerProvider(cap, mock);
            }
            // Step A: Action extraction + Verification
            mock.addResponse(new LlmResponse("{\"actions\":[]}", null, "mock"));
            mock.addResponse(new LlmResponse("{\"rubrics\":{\"intentMatch\":{\"analysis\":\"step A verified\",\"score\":\"PASS\"},\"visualDelta\":{\"analysis\":\"step A verified\",\"score\":\"PASS\"},\"absenceOfErrors\":{\"analysis\":\"step A verified\",\"score\":\"PASS\"}},\"overallVerdict\":{\"passed\":true,\"summary\":\"step A verified\"}}", null, "mock"));
            // Step B: Action extraction + Verification
            mock.addResponse(new LlmResponse("{\"actions\":[]}", null, "mock"));
            mock.addResponse(new LlmResponse("{\"rubrics\":{\"intentMatch\":{\"analysis\":\"step B verified\",\"score\":\"PASS\"},\"visualDelta\":{\"analysis\":\"step B verified\",\"score\":\"PASS\"},\"absenceOfErrors\":{\"analysis\":\"step B verified\",\"score\":\"PASS\"}},\"overallVerdict\":{\"passed\":true,\"summary\":\"step B verified\"}}", null, "mock"));
        }

        @AiPlaybook("/org/neodymium/ai/junit/ExplicitTest.yaml")
        @AiDataSet("explicit1")
        public void myTestMethod(final AiSession session)
        {
            executionCount++;
            final Object paramVal = session.getExecutionContext().getSessionData().get("param");
            resolvedParamValues.add(String.valueOf(paramVal));
        }
    }

    @Test
    public void testRelativeAndAbsoluteClasspathResolution()
    {
        RelativeAndAbsoluteTest.executionCount = 0;

        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(RelativeAndAbsoluteTest.class))
            .build();
        final Launcher launcher = LauncherFactory.create();
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        for (final org.junit.platform.launcher.listeners.TestExecutionSummary.Failure failure : listener.getSummary().getFailures())
        {
            System.err.println("LAUNCHER FAILURE in RelativeAndAbsoluteTest: " + failure.getException().getMessage());
            failure.getException().printStackTrace();
        }

        assertEquals(0, listener.getSummary().getTestsFailedCount(), "No tests should fail");
        assertEquals(1, listener.getSummary().getTestsSucceededCount(), "Relative and absolute resolution test should succeed");
        assertEquals(1, RelativeAndAbsoluteTest.executionCount);
    }

    @NeodymiumAiTest
    public static class RelativeAndAbsoluteTest
    {
        public static int executionCount = 0;

        @BeforeEach
        public void setup(final AiSession session)
        {
            final MockLlmProvider mock = new MockLlmProvider();
            for (final LlmCapability cap : LlmCapability.values())
            {
                session.getLlmRegistry().registerProvider(cap, mock);
            }
            mock.addResponse(new LlmResponse("{\"actions\":[]}", null, "mock"));
            mock.addResponse(new LlmResponse("{\"rubrics\":{\"intentMatch\":{\"analysis\":\"ok\",\"score\":\"PASS\"},\"visualDelta\":{\"analysis\":\"ok\",\"score\":\"PASS\"},\"absenceOfErrors\":{\"analysis\":\"ok\",\"score\":\"PASS\"}},\"overallVerdict\":{\"passed\":true,\"summary\":\"ok\"}}", null, "mock"));
            mock.addResponse(new LlmResponse("{\"actions\":[]}", null, "mock"));
            mock.addResponse(new LlmResponse("{\"rubrics\":{\"intentMatch\":{\"analysis\":\"ok\",\"score\":\"PASS\"},\"visualDelta\":{\"analysis\":\"ok\",\"score\":\"PASS\"},\"absenceOfErrors\":{\"analysis\":\"ok\",\"score\":\"PASS\"}},\"overallVerdict\":{\"passed\":true,\"summary\":\"ok\"}}", null, "mock"));
        }

        @AiPlaybook("ExplicitTest.yaml")
        @AiDataSet("explicit1")
        public void testRelativePath(final AiSession session)
        {
            executionCount++;
        }
    }
}

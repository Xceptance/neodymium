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
package org.neodymium.ai.integration.live;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiJudge;
import org.neodymium.ai.junit.AiLinter;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live browser integration tests for visual and layout snapshotting directives:
 * {@code (visual)}, {@code (visual-full)}, {@code (layout)}, and {@code (layout: full)}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@NeodymiumAiTest
@AiLinter(false)
@AiJudge({false, true})
@Tag("LiveAPI")
public class VisualAndLayoutIntegrationTest extends BaseAiTest
{
    /**
     * Sets up the test page URL before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/verla-tailwind/index.html", server.getPort());
        session.data().putDynamic("test.page.url", pageUrl, false);
    }

    /**
     * Verifies standard viewport-level visual snapshotting via {@code (visual)}.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/VisualAndLayoutIntegrationTest_testVisual.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testVisual(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${test.page.url} in the browser
              Verify header navigation and hero section (visual)
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());
    }

    /**
     * Verifies full-page visual snapshotting via {@code (visual-full)}.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/VisualAndLayoutIntegrationTest_testVisualFull.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testVisualFull(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${test.page.url} in the browser
              Verify full page content from header to footer (visual-full)
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());
    }

    /**
     * Verifies fuzzy color wireframe layout snapshotting via {@code (layout)}.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/VisualAndLayoutIntegrationTest_testLayout.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testLayout(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${test.page.url} in the browser
              Verify homepage layout and styling (layout)
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());
    }

    /**
     * Verifies full-page color wireframe layout snapshotting via {@code (layout: full)}.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/VisualAndLayoutIntegrationTest_testLayoutFull.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testLayoutFull(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${test.page.url} in the browser
              Verify entire homepage structure from top to bottom (layout: full)
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());
    }
}

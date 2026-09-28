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

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiLinter;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live integration test for the STORE action plugin verifying DOM text capture into session
 * variables, literal value storage, and dynamic variable substitution into downstream actions.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
@AiLinter(false)
public class StoreIntegrationTest extends BaseAiTest
{
    /**
     * Sets up test page URL before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/StoreActionTest/testStoreHappyPath.html", server.getPort());
        session.data().putDynamic("store.test.url", pageUrl, false);
    }

    /**
     * Verifies storing element text into a variable and using it in subsequent typing and validation.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/StoreIntegrationTest_testStoreElementTextAndVerify.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testStoreElementTextAndVerify(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${store.test.url} in the browser
              Store the text of #order-id into variable storedOrderId
              Type ${storedOrderId} into #input-target
              Click #btn-verify
            """)
            .verifyMetrics()
            .hasStepCount(4)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#input-target").shouldHave(value("ORD-987654"));
        $("#result").shouldHave(exactText("Verified successfully!"));
    }

    /**
     * Verifies storing a literal string value into a variable and using it in downstream actions.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/StoreIntegrationTest_testStoreLiteralValueAndVerify.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testStoreLiteralValueAndVerify(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${store.test.url} in the browser
              Store "ORD-987654" into variable customOrder
              Type ${customOrder} into #input-target
              Click #btn-verify
            """)
            .verifyMetrics()
            .hasStepCount(4)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#result").shouldHave(exactText("Verified successfully!"));
    }

    /**
     * Verifies storing multiple DOM elements into distinct variables and inspecting session state.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/StoreIntegrationTest_testStoreMultipleVariables.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testStoreMultipleVariables(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${store.test.url} in the browser
              Store the text of #order-id into variable orderId
              Store the text of #price-amount into variable orderPrice
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        assertEquals("ORD-987654", session.data().getDynamic("orderId"));
        assertEquals("14,96 €", session.data().getDynamic("orderPrice"));
    }
}

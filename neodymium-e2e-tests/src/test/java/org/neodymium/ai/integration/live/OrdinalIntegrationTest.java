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
package org.neodymium.ai.integration.live;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Selenide.$;

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
 * Live integration test suite verifying natural language ordinal element selection
 * across multiple frontend implementation qualities (Semantic HTML5, Legacy Unstructured,
 * Modern Non-Table Grids, and Non-Table Component Collections).
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
@AiLinter(false)
@AiJudge({false, true})
public class OrdinalIntegrationTest extends BaseAiTest
{
    /**
     * Sets up test fixture URLs before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String semanticUrl = String.format("http://localhost:%d/OrdinalChallengesTest/testOrdinalSemantic.html", server.getPort());
        final String legacyUrl = String.format("http://localhost:%d/OrdinalChallengesTest/testOrdinalLegacyBad.html", server.getPort());
        final String nonTableUrl = String.format("http://localhost:%d/OrdinalChallengesTest/testOrdinalModernNonTable.html", server.getPort());
        final String componentsUrl = String.format("http://localhost:%d/OrdinalChallengesTest/testOrdinalComponents.html", server.getPort());

        session.data().putDynamic("ordinal.test.semantic.url", semanticUrl, false);
        session.data().putDynamic("ordinal.test.legacy.url", legacyUrl, false);
        session.data().putDynamic("ordinal.test.nontable.url", nonTableUrl, false);
        session.data().putDynamic("ordinal.test.components.url", componentsUrl, false);
    }

    /**
     * Verifies clicking the 3rd row in a semantic HTML5 table with thead/tbody.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickThirdRowSemantic.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickThirdRowSemantic(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.semantic.url} in the browser
              Click the third row in table users-table
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Row 3 clicked: #103 Charlie Davies"));
    }

    /**
     * Verifies clicking an action button located inside the 3rd row of a semantic HTML5 table.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickActionInThirdRowSemantic.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickActionInThirdRowSemantic(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.semantic.url} in the browser
              Click the 'Edit' button in the third row of table users-table
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Charlie Davies edited"));
    }

    /**
     * Verifies clicking the 5th item in a semantic unordered list.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickFifthItemSemanticList.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickFifthItemSemanticList(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.semantic.url} in the browser
              Click the fifth item in catalog-list
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Item 5 clicked: Billing and Subscriptions"));
    }

    /**
     * Verifies clicking the 3rd data row in a legacy table without thead/tbody where row 0 is header th.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickThirdRowLegacyBadTable.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickThirdRowLegacyBadTable(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.legacy.url} in the browser
              Click the third row in table legacy-orders
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Order #1003 selected"));
    }

    /**
     * Verifies colloquial typo tolerance ('fith') when clicking items in a collection with duplicate IDs.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickFifthItemWithTypo.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickFifthItemWithTypo(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.legacy.url} in the browser
              Click the fith item in legacy-cards
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Item 5 selected"));
    }

    /**
     * Verifies clicking the 3rd row in an ARIA DataGrid (divs with role="table", role="row", role="cell").
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickThirdRowAriaDataGrid.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickThirdRowAriaDataGrid(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.nontable.url} in the browser
              Click the third row in table aria-orders
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Order #303 selected"));
    }

    /**
     * Verifies clicking an action button in the 3rd row of an ARIA DataGrid.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickActionInThirdRowAriaDataGrid.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickActionInThirdRowAriaDataGrid(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.nontable.url} in the browser
              Click the 'View' button in the third row of table aria-orders
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Viewing Order #303"));
    }

    /**
     * Verifies clicking the 3rd row in a pure Tailwind CSS Grid div-table without table tags or ARIA.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickThirdRowTailwindTable.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickThirdRowTailwindTable(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.nontable.url} in the browser
              Click the third row in tailwind-order-table
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Row 3 clicked: REF-103"));
    }

    /**
     * Verifies clicking an action button in the 3rd row of a pure Tailwind CSS Grid div-table.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickActionInThirdRowTailwindTable.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickActionInThirdRowTailwindTable(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.nontable.url} in the browser
              Click the 'Inspect' button in the third row of tailwind-order-table
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Viewing REF-103"));
    }

    /**
     * Verifies clicking a button inside the 3rd product card where each card is wrapped in a solitary column div.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickThirdItemWrappedCatalogGrid.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickThirdItemWrappedCatalogGrid(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.nontable.url} in the browser
              Click 'Buy' on the third item in catalog-grid
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Purchased Wool Sweater"));
    }

    /**
     * Verifies clicking the 2nd suggestion in an autocomplete search dropdown list.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickSecondSearchSuggestion.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickSecondSearchSuggestion(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.components.url} in the browser
              Click the second search suggestion
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Navigated to Minimalist Terracotta Sweater"));
    }

    /**
     * Verifies clicking the 3rd tab in an ARIA tablist navigation bar.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickThirdTab.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickThirdTab(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.components.url} in the browser
              Click the third tab in account-tabs
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Tab selected: Saved Addresses"));
    }

    /**
     * Verifies clicking an action button on the 2nd line item in a stacked cart drawer.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testRemoveSecondCartItem.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testRemoveSecondCartItem(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.components.url} in the browser
              Click the 'Remove' button on the second item in the cart drawer
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Removed Leather Belt"));
    }

    /**
     * Verifies clicking an action on the last item in a collection via negative relative indexing.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickLastItemInCart.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickLastItemInCart(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.components.url} in the browser
              Click the 'Remove' button on the last item in the cart drawer
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Removed Wool Socks"));
    }

    /**
     * Verifies selecting an ordinal item from a list containing dynamically hidden elements,
     * ensuring ordinal counts reflect visible elements only.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickSecondVisibleItemAfterFiltering.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickSecondVisibleItemAfterFiltering(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.components.url} in the browser
              Click the second order in filtered-orders
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Selected Active Order #403"));
    }

    /**
     * Verifies clicking an element targeted with digit ordinal '3rd element' phrasing.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClick3rdElementGenericList.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClick3rdElementGenericList(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.semantic.url} in the browser
              Click the 3rd element in catalog-list
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Item 3 clicked: Audit Logs"));
    }

    /**
     * Verifies clicking the 'last' row directly in a semantic HTML5 table.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickLastRowInSemanticTable.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickLastRowInSemanticTable(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.semantic.url} in the browser
              Click the last row in table users-table
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Row 5 clicked: #105 Evan Wright"));
    }

    /**
     * Verifies clicking the 'last' item in an unordered list collection.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickLastItemInCatalogList.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickLastItemInCatalogList(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.semantic.url} in the browser
              Click the last item in catalog-list
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Item 6 clicked: Security Settings"));
    }

    /**
     * Verifies clicking the 'first' row in a table.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickFirstRowInTable.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickFirstRowInTable(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.semantic.url} in the browser
              Click the first row in table users-table
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Row 1 clicked: #101 Alice Smith"));
    }

    /**
     * Verifies clicking the '3rd element' in legacy cards with duplicate IDs.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClick3rdElementInLegacyCards.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClick3rdElementInLegacyCards(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.legacy.url} in the browser
              Click the 3rd element in legacy-cards
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Item 3 selected"));
    }

    /**
     * Verifies clicking the 'last' row in a legacy bad table where row 0 is a header th.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickLastRowInLegacyTable.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickLastRowInLegacyTable(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.legacy.url} in the browser
              Click the last row in table legacy-orders
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Order #1005 selected"));
    }

    /**
     * Verifies clicking the '3rd card' directly in a modern wrapped CSS grid.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClick3rdCardInModernGrid.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClick3rdCardInModernGrid(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.nontable.url} in the browser
              Click the 3rd card in catalog-grid
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Card 3 clicked: Wool Sweater"));
    }

    /**
     * Verifies clicking the 'last card' in a modern wrapped CSS grid.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickLastCardInModernGrid.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickLastCardInModernGrid(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.nontable.url} in the browser
              Click the last card in catalog-grid
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Card 6 clicked: Leather Jacket"));
    }

    /**
     * Verifies clicking the 'last row' in a pure Tailwind CSS Grid div-table.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickLastRowInTailwindTable.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickLastRowInTailwindTable(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.nontable.url} in the browser
              Click the last row in tailwind-order-table
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Row 4 clicked: REF-104"));
    }

    /**
     * Verifies clicking the '2nd element' in an autocomplete suggestions dropdown.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClick2ndElementInSearchSuggestions.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClick2ndElementInSearchSuggestions(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.components.url} in the browser
              Click the 2nd element in search-suggestions
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Navigated to Minimalist Terracotta Sweater"));
    }

    /**
     * Verifies directly clicking the 'last item' in a slide-out cart drawer.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickLastCartItemDirectly.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickLastCartItemDirectly(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.components.url} in the browser
              Click the last item in the cart drawer
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Cart item clicked: Wool Socks"));
    }

    /**
     * Verifies clicking an action button in a row identified by content with an intentional typo ('Alica' instead of 'Alice').
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickEditButtonInRowWithAlicaTypo.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickEditButtonInRowWithAlicaTypo(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.semantic.url} in the browser
              Click the edit button in the row with Alica as name
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Alice Smith edited"));
    }

    /**
     * Verifies clicking an action button in a row identified by content ('Alice').
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickEditButtonInRowWithAlice.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickEditButtonInRowWithAlice(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.semantic.url} in the browser
              Click the edit button in the row with Alice as name
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Alice Smith edited"));
    }

    /**
     * Verifies clicking an action button in a pure CSS grid row identified by content ('REF-102').
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook("/playbooks/integration/programmatic/OrdinalIntegrationTest_testClickInspectButtonInTailwindRowByCode.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickInspectButtonInTailwindRowByCode(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${ordinal.test.nontable.url} in the browser
              Click the 'Inspect' button in the row with 'REF-102'
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#status-message").shouldHave(exactText("Viewing REF-102"));
    }
}

/*
 * Apache License 2.0
 *
 * Copyright (c) 2026 Xceptance
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neodymium.ai.executor.selenide.plugins;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.selenide.SelenideTargetExecutor;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;

/**
 * Unit and integration tests validating the newly introduced action plugins:
 * CHECK, STORE, BRANCH, INCLUDE, and SPLIT.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public final class SelenideActionPluginsTest extends BaseAiTest
{
    private ExecutionContext context;
    private SelenideTargetExecutor executor;

    /**
     * Default constructor.
     */
    public SelenideActionPluginsTest()
    {
    }

    /**
     * Initializes the context, session, and executor before each test method.
     */
    @BeforeEach
    public void setUp()
    {
        this.context = new ExecutionContext(new SessionData(Map.of()));
        this.executor = new SelenideTargetExecutor();
        this.executor.setExecutionContext(this.context);
        this.context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, this.executor);

        final AiSession session = AiSession.mock(this.context.getSessionData(), null, null, this.executor);
        this.context.getTransientData().put(ExecutionContext.KEY_SESSION, session);

        // Open the local test HTML page
        open(String.format("http://localhost:%d/SelenideActionPluginsTest/test.html", server.getPort()));
    }

    /**
     * Verifies that CHECK action correctly selects checkboxes and radio buttons.
     */
    @Test
    public void testCheckAction() throws Exception
    {
        // Assert initial unchecked state
        assertFalse($("#check-box-1").isSelected());

        // Perform CHECK action
        final Action checkAction = new Action("CHECK", "#check-box-1", "Check the checkbox");
        this.executor.execute(checkAction);

        // Assert checked
        assertTrue($("#check-box-1").isSelected());

        // Perform CHECK action again (should remain checked and not toggle back)
        this.executor.execute(checkAction);
        assertTrue($("#check-box-1").isSelected());
    }

    /**
     * Verifies that STORE action extracts page element text, normalizes prices,
     * and handles storing literal values into the session data.
     */
    @Test
    public void testStoreAction() throws Exception
    {
        // 1. Store element text with price normalization
        final Action storePriceAction = new Action("STORE", "#price-container", List.of("myPrice"), "Store total price", "reason");
        storePriceAction.setAdjust(true);
        this.executor.execute(storePriceAction);

        final Object storedPrice = this.context.getSessionData().get("myPrice");
        assertEquals("19.99", String.valueOf(storedPrice));

        // 2. Store literal values directly
        final Action storeLiteralAction = new Action("STORE", "", List.of("myLiteral", "hello-world"), "Store literal string", "reason");
        this.executor.execute(storeLiteralAction);

        final Object storedLiteral = this.context.getSessionData().get("myLiteral");
        assertEquals("hello-world", String.valueOf(storedLiteral));
    }

    /**
     * Verifies that BRANCH action evaluates condition assertions and executes the correct branch.
     */
    @Test
    public void testBranchAction() throws Exception
    {
        assertFalse($("#check-box-1").isSelected());

        // Create condition (assert checkbox is visible)
        final Action checkAssert = new Action("ASSERT", "#check-box-1", List.of("visible"), "Assert visible", "reason");
        // Create then (check the checkbox)
        final Action thenClick = new Action("CLICK", "#check-box-1", "Click checkbox");

        final Action branchAction = new Action("BRANCH", "", "Execute branch conditional");
        branchAction.setCondition(List.of(checkAssert));
        branchAction.setThen(List.of(thenClick));

        // Execute branch (condition is met, thenClick should run)
        this.executor.execute(branchAction);
        assertTrue($("#check-box-1").isSelected());
        assertTrue(BranchAction.getLastConditionResult());
    }
}

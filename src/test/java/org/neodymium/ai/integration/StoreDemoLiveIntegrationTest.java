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
package org.neodymium.ai.integration;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;

/**
 * Live integration test using JUnit 5 template-driven AI execution.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
public class StoreDemoLiveIntegrationTest extends BaseAiTest
{
    /**
     * Constructs a default StoreDemoLiveIntegrationTest.
     */
    public StoreDemoLiveIntegrationTest()
    {
    }

    @BeforeEach
    public void setupUrl()
    {
        final String pageUrl = String.format("http://localhost:%d/ClickActionTest/testClickStandardButton.html", server.getPort());
        System.setProperty("demo.url", pageUrl);
    }

    @AiPlaybook("playbooks/integration/store-click-demo.yaml")
    public void testLiveStoreClick()
    {
        // Assert the button click listener was triggered successfully by checking the result label
        $("#result").shouldHave(text("Order Submitted!"));
    }
}

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

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.util.Neodymium;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;

/**
 * Complex Storefront Integration Test for Verla storefront across all three quality levels (perfect, normal, bad).
 * Tests country selection, search, product selection, checkout failures (bug negation), and coupon calculations.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VerlaComplexStorefrontIntegrationTest extends BaseAiTest
{
    /**
     * Constructs a default VerlaComplexStorefrontIntegrationTest.
     */
    public VerlaComplexStorefrontIntegrationTest()
    {
    }

    /**
     * Set up dynamic test parameters before each run.
     */
    @BeforeEach
    public void setup()
    {
        Neodymium.getData().put("verla.url", String.format("http://localhost:%d", server.getPort()));
        com.codeborne.selenide.Configuration.browserSize = "1500x1000";
        if (com.codeborne.selenide.WebDriverRunner.hasWebDriverStarted())
        {
            com.codeborne.selenide.Selenide.closeWebDriver();
        }
    }

    /**
     * Executes the complex checkout flow in live mode (recording playbooks).
     */
    @Order(1)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook("playbooks/integration/complex-storefront-verla.yaml")
    public void testComplexCheckoutLive()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Replays the recorded complex checkout flow in strict mode to ensure stability.
     */
    @Order(2)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiPlaybook("playbooks/integration/complex-storefront-verla.yaml")
    public void testComplexCheckoutReplay()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }
}

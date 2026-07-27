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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;

/**
 * Guest Checkout Integration Test for Verla storefront.
 * Tests LLM action generation, recording, and strict replay modes sequentially.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VerlaGuestCheckoutIntegrationTest extends BaseAiTest
{
    /**
     * Constructs a default VerlaGuestCheckoutIntegrationTest.
     */
    public VerlaGuestCheckoutIntegrationTest()
    {
    }

    /**
     * Set up dynamic test parameters and customize target playbook path writers.
     *
     * @param session the thread-isolated AiSession
     * @param context the thread-isolated execution context
     */
    @BeforeEach
    public void setup()
    {
        // Resolve dynamic server port for the test execution
        org.neodymium.util.Neodymium.getData().put("verla.url.host", String.format("localhost:%d", server.getPort()));
    }

    /**
     * Live mode execution with dynamic LLM query and recorded playbook generation.
     */
    @Order(1)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("perfect")
    @AiPlaybook("/playbooks/integration/guest-checkout-verla.yaml")
    public void testCheckoutLive()
    {
        // Assert order success screen checkmark is displayed
        // we do that independent of the AI part to ensure we are really ok
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Strict replay mode execution using the recorded playbook generated in live mode.
     */
    @Order(2)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("perfect")
    @AiPlaybook("/playbooks/integration/guest-checkout-verla.yaml")
    public void testCheckoutReplay()
    {
        // Assert order success screen checkmark is displayed
        // we do that independent of the AI part to ensure we are really ok
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Live mode execution running with all datasets (perfect, normal, bad, modern-bad, modern-bad-nowcag) defined in the playbook.
     */
    @Order(3)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook("/playbooks/integration/guest-checkout-verla.yaml")
    public void testCheckoutLiveAllDataSets()
    {
        // Assert order success screen checkmark is displayed
        // we do that independent of the AI part to ensure we are really ok
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Strict replay mode execution running with all datasets (perfect, normal, bad, modern-bad, modern-bad-nowcag) defined in the playbook.
     */
    @Order(4)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiPlaybook("/playbooks/integration/guest-checkout-verla.yaml")
    public void testCheckoutReplayAllDataSets()
    {
        // Assert order success screen checkmark is displayed
        // we do that independent of the AI part to ensure we are really ok
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }
}

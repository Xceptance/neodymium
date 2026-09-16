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
package org.neodymium.ai.integration.verla.checkout.normal.judge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiJudge;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiOutcomeVerification;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Quality Judge Guest Checkout Integration Test targeting the Normal US storefront (/verla-normal/) in live recording and strict replay modes with Quality Judge enabled.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("integration")
@Tag("verla")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@Tag("flow:checkout")
@Tag("store:normal")
@Tag("mode:judge")
@NeodymiumAiTest
@AiJudge(true)
@AiOutcomeVerification(value = false, failOnError = false)
@AiPlaybook(value = "playbooks/integration/VerlaGuestCheckout_Us_English_Normal.yaml", recordingDirectory = "target/playbooks/integration/checkout/normal")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VerlaNormalJudgeCheckoutTest extends BaseAiTest
{
    /**
     * Constructs a default VerlaNormalJudgeCheckoutTest instance.
     */
    public VerlaNormalJudgeCheckoutTest()
    {
    }

    /**
     * Set up dynamic test parameters before each run.
     */
    @BeforeEach
    public void setup()
    {
        if (server != null)
        {
            server.resetInventory();
        }
        Neodymium.getData().put("verla.url", String.format("https://localhost:%d", server.getHttpsPort()));
    }

    /**
     * Live recording mode execution for dataset 'normal'.
     */
    @Order(1)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("normal")
    @AiPlaybook
    public void testCheckoutLiveWithJudge()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'normal'.
     */
    @Order(2)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("normal")
    @AiPlaybook(recordingMethod = "testCheckoutLiveWithJudge")
    public void testCheckoutReplayWithJudge()
    {
    }
}

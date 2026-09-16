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
package org.neodymium.ai.integration.verla;

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
 * English Language Guest Checkout Integration Test targeting the Japan storefront (-JP).
 * Drives the English multi-locale playbook for Japan.
 *
 * Schema: Feature_TargetStore_ScriptLanguage
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook(value = "playbooks/integration/VerlaGuestCheckout_Multi_English.yaml", recordingDirectory = "target/playbooks/integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AiOutcomeVerification(failOnError = false)
public class VerlaGuestCheckout_Jp_English extends BaseAiTest
{
    /**
     * Constructs a default VerlaGuestCheckout_Jp_English instance.
     */
    public VerlaGuestCheckout_Jp_English()
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

    @Order(1)
    @AiJudge(false)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("japan")
    @AiPlaybook
    public void testCheckoutLiveJapan()
    {
    }

    @Order(2)
    @AiJudge(false)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("japan")
    @AiPlaybook(recordingMethod = "testCheckoutLiveJapan")
    public void testCheckoutReplayJapan()
    {
    }

    @Order(3)
    @AiJudge(true)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("japan")
    @AiPlaybook
    public void testCheckoutLiveJapanWithJudge()
    {
    }

    @Order(4)
    @AiJudge(true)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("japan")
    @AiPlaybook(recordingMethod = "testCheckoutLiveJapanWithJudge")
    public void testCheckoutReplayJapanWithJudge()
    {
    }

    @Order(5)
    @AiJudge(false)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("japan")
    @AiPlaybook
    public void testCheckoutLiveJapanWithOutcome()
    {
    }

    @Order(6)
    @AiJudge(false)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("japan")
    @AiPlaybook(recordingMethod = "testCheckoutLiveJapanWithOutcome")
    public void testCheckoutReplayJapanWithOutcome()
    {
    }

    @Order(7)
    @AiJudge(true)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("japan")
    @AiPlaybook
    public void testCheckoutLiveJapanWithJudgeAndOutcome()
    {
    }

    @Order(8)
    @AiJudge(true)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("japan")
    @AiPlaybook(recordingMethod = "testCheckoutLiveJapanWithJudgeAndOutcome")
    public void testCheckoutReplayJapanWithJudgeAndOutcome()
    {
    }
}

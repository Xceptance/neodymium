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
package org.neodymium.ai.integration.verla.demo;

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
 * Integration Test demonstrating the Multi-Locale Auto-Translation pattern using promptAddon.
 * Uses a single playbook with translate('&lt;text&gt;') tokens and dynamic ${targetLocale} variable
 * interpolation in promptAddon to execute checkout across different language storefronts.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("AuraIntegration")
@Tag("flow:demo")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook(value = "playbooks/integration/VerlaAutoTranslateCheckout.yaml", recordingDirectory = "target/playbooks/integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AiOutcomeVerification(failOnError = false)
public class AutoTranslateCheckoutIntegrationTest extends BaseAiTest
{
    /**
     * Constructs a default AutoTranslateCheckoutIntegrationTest instance.
     */
    public AutoTranslateCheckoutIntegrationTest()
    {
    }

    /**
     * Set up dynamic test parameters and reset inventory before each run.
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
    @AiDataSet("ca_fr")
    @AiPlaybook
    public void testCheckoutLiveFrenchCanada()
    {
    }

    @Order(2)
    @AiJudge(false)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("ca_fr")
    @AiPlaybook(recordingMethod = "testCheckoutLiveFrenchCanada")
    public void testCheckoutReplayFrenchCanada()
    {
    }

    @Order(3)
    @AiJudge(false)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("de")
    @AiPlaybook
    public void testCheckoutLiveGerman()
    {
    }

    @Order(4)
    @AiJudge(false)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("de")
    @AiPlaybook(recordingMethod = "testCheckoutLiveGerman")
    public void testCheckoutReplayGerman()
    {
    }

    @Order(5)
    @AiJudge(true)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("ca_fr")
    @AiPlaybook
    public void testCheckoutLiveFrenchCanadaWithJudge()
    {
    }

    @Order(6)
    @AiJudge(true)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("ca_fr")
    @AiPlaybook(recordingMethod = "testCheckoutLiveFrenchCanadaWithJudge")
    public void testCheckoutReplayFrenchCanadaWithJudge()
    {
    }

    @Order(7)
    @AiJudge(true)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("de")
    @AiPlaybook
    public void testCheckoutLiveGermanWithJudge()
    {
    }

    @Order(8)
    @AiJudge(true)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("de")
    @AiPlaybook(recordingMethod = "testCheckoutLiveGermanWithJudge")
    public void testCheckoutReplayGermanWithJudge()
    {
    }

    @Order(9)
    @AiJudge(false)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("ca_fr")
    @AiPlaybook
    public void testCheckoutLiveFrenchCanadaWithOutcome()
    {
    }

    @Order(10)
    @AiJudge(false)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("ca_fr")
    @AiPlaybook(recordingMethod = "testCheckoutLiveFrenchCanadaWithOutcome")
    public void testCheckoutReplayFrenchCanadaWithOutcome()
    {
    }

    @Order(11)
    @AiJudge(false)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("de")
    @AiPlaybook
    public void testCheckoutLiveGermanWithOutcome()
    {
    }

    @Order(12)
    @AiJudge(false)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("de")
    @AiPlaybook(recordingMethod = "testCheckoutLiveGermanWithOutcome")
    public void testCheckoutReplayGermanWithOutcome()
    {
    }

    @Order(13)
    @AiJudge(true)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("ca_fr")
    @AiPlaybook
    public void testCheckoutLiveFrenchCanadaWithJudgeAndOutcome()
    {
    }

    @Order(14)
    @AiJudge(true)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("ca_fr")
    @AiPlaybook(recordingMethod = "testCheckoutLiveFrenchCanadaWithJudgeAndOutcome")
    public void testCheckoutReplayFrenchCanadaWithJudgeAndOutcome()
    {
    }

    @Order(15)
    @AiJudge(true)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("de")
    @AiPlaybook
    public void testCheckoutLiveGermanWithJudgeAndOutcome()
    {
    }

    @Order(16)
    @AiJudge(true)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("de")
    @AiPlaybook(recordingMethod = "testCheckoutLiveGermanWithJudgeAndOutcome")
    public void testCheckoutReplayGermanWithJudgeAndOutcome()
    {
    }
}

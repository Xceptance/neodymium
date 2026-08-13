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

import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.ai.util.EmbeddedHtmlServer;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * English Language Guest Checkout Integration Test targeting the US storefront.
 * Tests LLM action generation, recording, and strict replay modes across all 5 quality variants
 * (perfect, normal, bad, modern-bad, modern-bad-nowcag).
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
@AiPlaybook(value = "playbooks/integration/VerlaGuestCheckout_Us_English.yaml", recordingDirectory = "target/playbooks/integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VerlaGuestCheckout_Us_English extends BaseAiTest
{
    /**
     * Constructs a default VerlaGuestCheckout_Us_English instance.
     */
    public VerlaGuestCheckout_Us_English()
    {
    }

    /**
     * Set up dynamic test parameters and judge configuration before each run.
     *
     * @param testInfo the JUnit TestInfo context
     */
    @BeforeEach
    public void setup(final TestInfo testInfo)
    {
        EmbeddedHtmlServer.resetInventory();
        Neodymium.getData().put("verla.url", String.format("https://localhost:%d", server.getHttpsPort()));

        final String methodName = testInfo.getTestMethod().map(Method::getName).orElse("");
        if (methodName.contains("WithJudge"))
        {
            Neodymium.getData().put("neodymium.ai.judge.enabled", "true");
        }
        else
        {
            Neodymium.getData().put("neodymium.ai.judge.enabled", "false");
        }
        AiConfiguration.resetInstance();
    }

    @Order(1)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("perfect")
    @AiPlaybook
    public void testCheckoutLivePerfect()
    {
    }

    @Order(2)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("perfect")
    @AiPlaybook(recordingMethod = "testCheckoutLivePerfect")
    public void testCheckoutReplayPerfect()
    {
    }

    @Order(3)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("normal")
    @AiPlaybook
    public void testCheckoutLiveNormal()
    {
    }

    @Order(4)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("normal")
    @AiPlaybook(recordingMethod = "testCheckoutLiveNormal")
    public void testCheckoutReplayNormal()
    {
    }

    @Order(5)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("bad")
    @AiPlaybook
    public void testCheckoutLiveBad()
    {
    }

    @Order(6)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("bad")
    @AiPlaybook(recordingMethod = "testCheckoutLiveBad")
    public void testCheckoutReplayBad()
    {
    }

    @Order(7)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("modern-bad")
    @AiPlaybook
    public void testCheckoutLiveModernBad()
    {
    }

    @Order(8)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("modern-bad")
    @AiPlaybook(recordingMethod = "testCheckoutLiveModernBad")
    public void testCheckoutReplayModernBad()
    {
    }

    @Order(9)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("modern-bad-nowcag")
    @AiPlaybook
    public void testCheckoutLiveModernBadNoWcag()
    {
    }

    @Order(10)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("modern-bad-nowcag")
    @AiPlaybook(recordingMethod = "testCheckoutLiveModernBadNoWcag")
    public void testCheckoutReplayModernBadNoWcag()
    {
    }
}

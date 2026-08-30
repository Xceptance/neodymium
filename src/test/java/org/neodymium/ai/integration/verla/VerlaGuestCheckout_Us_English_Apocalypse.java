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
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * English Language Guest Checkout Integration Test targeting the VÉRLA Apocalypse storefront (/verla-apocalypse/).
 * Tests LLM action generation, recording, and strict replay modes against extreme web anti-patterns
 * (CLS layout shifts, progressive hydration ghost clicks, pure CSS pseudo-element text, floating overlays).
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook(value = "playbooks/integration/VerlaGuestCheckout_Us_English_Apocalypse.yaml", recordingDirectory = "target/playbooks/integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VerlaGuestCheckout_Us_English_Apocalypse extends BaseAiTest
{
    /**
     * Constructs a default VerlaGuestCheckout_Us_English_Apocalypse instance.
     */
    public VerlaGuestCheckout_Us_English_Apocalypse()
    {
    }

    /**
     * Set up dynamic test parameters before each run.
     */
    @BeforeEach
    public void setup()
    {
        this.server.resetInventory();
        Neodymium.getData().put("verla.url", String.format("https://localhost:%d", this.server.getHttpsPort()));
        Neodymium.getData().put("neodymium.ai.multilingual", "true");
    }

    @Order(1)
    @AiJudge(false)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("apocalypse")
    @AiPlaybook
    public void testCheckoutLive()
    {
    }

    @Order(2)
    @AiJudge(false)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("apocalypse")
    @AiPlaybook(recordingMethod = "testCheckoutLive")
    public void testCheckoutReplay()
    {
    }

    @Order(3)
    @AiJudge(true)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("apocalypse")
    @AiPlaybook
    public void testCheckoutLiveWithJudge()
    {
    }

    @Order(4)
    @AiJudge(true)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("apocalypse")
    @AiPlaybook(recordingMethod = "testCheckoutLiveWithJudge")
    public void testCheckoutReplayWithJudge()
    {
    }
}

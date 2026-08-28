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
 * Unified Multi-Locale Guest Checkout Integration Test.
 * Drives a single English playbook across all 9 international storefront locales.
 *
 * Runs FORCE_RECORDING and REPLAY_STRICT across both Quality Judge modes (false and true)
 * for each country individually.
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
@AiJudge({false, true})
@AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
@AiPlaybook(value = "playbooks/integration/VerlaGuestCheckout_Multi_English.yaml", recordingDirectory = "target/playbooks/integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VerlaGuestCheckout_Multi_English extends BaseAiTest
{
    /**
     * Constructs a default VerlaGuestCheckout_Multi_English instance.
     */
    public VerlaGuestCheckout_Multi_English()
    {
    }

    /**
     * Set up dynamic test parameters before each run.
     */
    @BeforeEach
    public void setup()
    {
        server.resetInventory();
        Neodymium.getData().put("verla.url", String.format("https://localhost:%d", server.getHttpsPort()));
        Neodymium.getData().put("neodymium.ai.multilingual", "true");
    }

    @Order(1)
    @AiDataSet("germany")
    @AiPlaybook
    public void testCheckoutGermany()
    {
    }

    @Order(2)
    @AiDataSet("united-states")
    @AiPlaybook
    public void testCheckoutUnitedStates()
    {
    }

    @Order(3)
    @AiDataSet("united-kingdom")
    @AiPlaybook
    public void testCheckoutUnitedKingdom()
    {
    }

    @Order(4)
    @AiDataSet("canada-en")
    @AiPlaybook
    public void testCheckoutCanadaEn()
    {
    }

    @Order(5)
    @AiDataSet("canada-fr")
    @AiPlaybook
    public void testCheckoutCanadaFr()
    {
    }

    @Order(6)
    @AiDataSet("poland")
    @AiPlaybook
    public void testCheckoutPoland()
    {
    }

    @Order(7)
    @AiDataSet("sweden")
    @AiPlaybook
    public void testCheckoutSweden()
    {
    }

    @Order(8)
    @AiDataSet("finland")
    @AiPlaybook
    public void testCheckoutFinland()
    {
    }

    @Order(9)
    @AiDataSet("japan")
    @AiPlaybook
    public void testCheckoutJapan()
    {
    }
}

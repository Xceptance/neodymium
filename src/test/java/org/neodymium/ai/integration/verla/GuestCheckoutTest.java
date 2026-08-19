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
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.ai.util.EmbeddedHtmlServer;
import com.xceptance.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Runs YAML-based VERLA integration tests for guest checkout flows
 * in recording mode first and strict replay mode second.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("integration")
@Tag("verla")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook(value = "verla/GuestCheckoutTest.yaml", recordingDirectory = "target/playbooks/integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class GuestCheckoutTest extends BaseAiTest
{
    /**
     * Constructs a default GuestCheckoutTest.
     */
    public GuestCheckoutTest()
    {
    }

    /**
     * Setup method to inject dynamic server URLs.
     */
    @BeforeEach
    public void setup()
    {
        EmbeddedHtmlServer.resetInventory();
        Neodymium.getData().put("verla.url", String.format("https://localhost:%d", server.getHttpsPort()));
    }

    /**
     * Live recording mode execution for dataset 'perfect'.
     */
    @Order(1)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("perfect")
    @AiPlaybook("verla/GuestCheckoutTest.yaml")
    public void testCheckoutLivePerfect()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'perfect'.
     */
    @Order(2)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("perfect")
    @AiPlaybook(value = "verla/GuestCheckoutTest.yaml", recordingMethod = "testCheckoutLivePerfect")
    public void testCheckoutReplayPerfect()
    {
    }

    /**
     * Live recording mode execution for dataset 'normal'.
     */
    @Order(3)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("normal")
    @AiPlaybook("verla/GuestCheckoutTest.yaml")
    public void testCheckoutLiveNormal()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'normal'.
     */
    @Order(4)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("normal")
    @AiPlaybook(value = "verla/GuestCheckoutTest.yaml", recordingMethod = "testCheckoutLiveNormal")
    public void testCheckoutReplayNormal()
    {
    }

    /**
     * Live recording mode execution for dataset 'bad'.
     */
    @Order(5)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("bad")
    @AiPlaybook("verla/GuestCheckoutTest.yaml")
    public void testCheckoutLiveBad()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'bad'.
     */
    @Order(6)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("bad")
    @AiPlaybook(value = "verla/GuestCheckoutTest.yaml", recordingMethod = "testCheckoutLiveBad")
    public void testCheckoutReplayBad()
    {
    }

    /**
     * Live recording mode execution across all datasets in the playbook.
     */
    @Order(7)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook("verla/GuestCheckoutTest.yaml")
    public void testCheckoutLiveAllDataSets()
    {
    }

    /**
     * Strict replay mode execution across all datasets in the playbook.
     */
    @Order(8)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiPlaybook(value = "verla/GuestCheckoutTest.yaml", recordingMethod = "testCheckoutLiveAllDataSets")
    public void testCheckoutReplayAllDataSets()
    {
    }
}

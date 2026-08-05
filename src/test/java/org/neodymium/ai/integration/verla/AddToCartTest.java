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
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Runs YAML-based VERLA integration tests for adding items to cart and applying coupons
 * in recording mode first and strict replay mode second.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("integration")
@Tag("verla")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class AddToCartTest extends BaseAiTest
{
    /**
     * Constructs a default AddToCartTest.
     */
    public AddToCartTest()
    {
    }

    /**
     * Setup method to inject dynamic server URLs.
     */
    @BeforeEach
    public void setup()
    {
        EmbeddedHtmlServer.resetInventory();
        Neodymium.getData().put("verla.url.host", String.format("localhost:%d", server.getPort()));
        Neodymium.getData().put("verla.url", String.format("http://localhost:%d", server.getPort()));
    }

    /**
     * Live recording mode execution for dataset 'perfect'.
     */
    @Order(1)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("perfect")
    @AiPlaybook("/verla/AddToCartTest.yaml")
    public void testCartLivePerfect()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'perfect'.
     */
    @Order(2)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("perfect")
    @AiPlaybook(value = "/verla/AddToCartTest.yaml", recordingMethod = "testCartLivePerfect")
    public void testCartReplayPerfect()
    {
    }

    /**
     * Live recording mode execution for dataset 'normal'.
     */
    @Order(3)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("normal")
    @AiPlaybook("/verla/AddToCartTest.yaml")
    public void testCartLiveNormal()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'normal'.
     */
    @Order(4)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("normal")
    @AiPlaybook(value = "/verla/AddToCartTest.yaml", recordingMethod = "testCartLiveNormal")
    public void testCartReplayNormal()
    {
    }

    /**
     * Live recording mode execution for dataset 'bad'.
     */
    @Order(5)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("bad")
    @AiPlaybook("/verla/AddToCartTest.yaml")
    public void testCartLiveBad()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'bad'.
     */
    @Order(6)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("bad")
    @AiPlaybook(value = "/verla/AddToCartTest.yaml", recordingMethod = "testCartLiveBad")
    public void testCartReplayBad()
    {
    }

    /**
     * Live recording mode execution across all datasets in the playbook.
     */
    @Order(7)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook("/verla/AddToCartTest.yaml")
    public void testCartLiveAllDataSets()
    {
    }

    /**
     * Strict replay mode execution across all datasets in the playbook.
     */
    @Order(8)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiPlaybook(value = "/verla/AddToCartTest.yaml", recordingMethod = "testCartLiveAllDataSets")
    public void testCartReplayAllDataSets()
    {
    }
}

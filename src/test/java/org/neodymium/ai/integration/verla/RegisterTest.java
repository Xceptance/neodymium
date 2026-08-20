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
 * Runs YAML-based VERLA integration tests for user registration and account creation flows
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
@AiPlaybook(value = "verla/RegisterTest.yaml", recordingDirectory = "target/playbooks/integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class RegisterTest extends BaseAiTest
{
    /**
     * Constructs a default RegisterTest.
     */
    public RegisterTest()
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
     * Live recording mode execution for dataset 'us'.
     */
    @Order(1)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("us")
    @AiPlaybook("/verla/RegisterTest.yaml")
    public void testRegisterLiveUs()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'us'.
     */
    @Order(2)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("us")
    @AiPlaybook(value = "/verla/RegisterTest.yaml", recordingMethod = "testRegisterLiveUs")
    public void testRegisterReplayUs()
    {
    }

    /**
     * Live recording mode execution for dataset 'de'.
     */
    @Order(3)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("de")
    @AiPlaybook("/verla/RegisterTest.yaml")
    public void testRegisterLiveDe()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'de'.
     */
    @Order(4)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("de")
    @AiPlaybook(value = "/verla/RegisterTest.yaml", recordingMethod = "testRegisterLiveDe")
    public void testRegisterReplayDe()
    {
    }

    /**
     * Live recording mode execution for dataset 'jp'.
     */
    @Order(5)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("jp")
    @AiPlaybook("/verla/RegisterTest.yaml")
    public void testRegisterLiveJp()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'jp'.
     */
    @Order(6)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("jp")
    @AiPlaybook(value = "/verla/RegisterTest.yaml", recordingMethod = "testRegisterLiveJp")
    public void testRegisterReplayJp()
    {
    }

    /**
     * Live recording mode execution across all datasets in the playbook.
     */
    @Order(7)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook("/verla/RegisterTest.yaml")
    public void testRegisterLiveAllDataSets()
    {
    }

    /**
     * Strict replay mode execution across all datasets in the playbook.
     */
    @Order(8)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiPlaybook(value = "/verla/RegisterTest.yaml", recordingMethod = "testRegisterLiveAllDataSets")
    public void testRegisterReplayAllDataSets()
    {
    }
}

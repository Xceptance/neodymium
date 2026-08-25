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
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Runs YAML-based VERLA integration tests for user registration and account creation flows
 * with PESAP, LLM Quality Judge, Semantic Outcome Verification, and Visual Root Cause Analysis (RCA) explicitly enabled.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("integration")
@Tag("verla")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@Tag("AuraJudge")
@Tag("AuraVerification")
@NeodymiumAiTest
@AiPlaybook(value = "verla/RegisterTest.yaml", recordingDirectory = "target/playbooks/integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class RegisterJudgeAndVerificationsTest extends BaseAiTest
{
    /**
     * Constructs a default RegisterJudgeAndVerificationsTest.
     */
    public RegisterJudgeAndVerificationsTest()
    {
    }

    /**
     * Setup method to inject dynamic server URLs and enable PESAP, Judge, Semantic Verification, and Visual RCA.
     */
    @BeforeEach
    public void setup()
    {
        // the server must exist here, otherwise something is wrong
        server.resetAll();
        Neodymium.getData().put("verla.url", String.format("https://localhost:%d", server.getHttpsPort()));
        Neodymium.getData().put("neodymium.ai.multilingual", "true");
        Neodymium.getData().put("random", String.valueOf(Neodymium.getRandom().nextInt(1_000, 100_000_000)));
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "true");
        Neodymium.getData().put("neodymium.ai.judge.enabled", "true");
        Neodymium.getData().put("neodymium.ai.semanticVerification.enabled", "true");
        Neodymium.getData().put("neodymium.ai.visualRca.enabled", "true");
        AiConfiguration.resetInstance();
    }

    /**
     * Live recording mode execution for dataset 'us'.
     */
    @Order(1)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("us")
    @AiPlaybook
    public void testRegisterLiveUs()
    {
    }

    /**
     * Live recording mode execution for dataset 'de'.
     */
    @Order(2)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("de")
    @AiPlaybook
    public void testRegisterLiveDe()
    {
    }

    /**
     * Live recording mode execution for dataset 'jp'.
     */
    @Order(3)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("jp")
    @AiPlaybook
    public void testRegisterLiveJp()
    {
    }

    /**
     * Live recording mode execution across all datasets in the playbook.
     */
    @Order(4)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook
    public void testRegisterLiveAllDataSets()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'us'.
     */
    @Order(5)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("us")
    @AiPlaybook(recordingMethod = "testRegisterLiveUs")
    public void testRegisterReplayUs()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'de'.
     */
    @Order(6)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("de")
    @AiPlaybook(recordingMethod = "testRegisterLiveDe")
    public void testRegisterReplayDe()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'jp'.
     */
    @Order(7)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("jp")
    @AiPlaybook(recordingMethod = "testRegisterLiveJp")
    public void testRegisterReplayJp()
    {
    }

    /**
     * Strict replay mode execution across all datasets in the playbook.
     */
    @Order(8)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiPlaybook(recordingMethod = "testRegisterLiveAllDataSets")
    public void testRegisterReplayAllDataSets()
    {
    }
}

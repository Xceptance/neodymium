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
 * Runs YAML-based VERLA integration tests for product search and search result filtering in English
 * in recording mode first and strict replay mode second, supporting Quality Judge and Outcome Verification modes.
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AiOutcomeVerification(failOnError = false)
public final class SearchTest extends BaseAiTest
{
    /**
     * Constructs a default SearchTest.
     */
    public SearchTest()
    {
    }

    /**
     * Setup method to inject dynamic server URLs.
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
     * Live recording mode execution for dataset 'perfect'.
     */
    @Order(1)
    @AiJudge(false)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("perfect")
    @AiPlaybook("/verla/SearchTest.yaml")
    public void testSearchLivePerfect()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'perfect'.
     */
    @Order(2)
    @AiJudge(false)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("perfect")
    @AiPlaybook(value = "/verla/SearchTest.yaml", recordingMethod = "testSearchLivePerfect")
    public void testSearchReplayPerfect()
    {
    }

    /**
     * Live recording mode execution for dataset 'normal'.
     */
    @Order(3)
    @AiJudge(false)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("normal")
    @AiPlaybook("/verla/SearchTest.yaml")
    public void testSearchLiveNormal()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'normal'.
     */
    @Order(4)
    @AiJudge(false)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("normal")
    @AiPlaybook(value = "/verla/SearchTest.yaml", recordingMethod = "testSearchLiveNormal")
    public void testSearchReplayNormal()
    {
    }

    /**
     * Live recording mode execution for dataset 'bad'.
     */
    @Order(5)
    @AiJudge(false)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("bad")
    @AiPlaybook("/verla/SearchTest.yaml")
    public void testSearchLiveBad()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'bad'.
     */
    @Order(6)
    @AiJudge(false)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("bad")
    @AiPlaybook(value = "/verla/SearchTest.yaml", recordingMethod = "testSearchLiveBad")
    public void testSearchReplayBad()
    {
    }

    /**
     * Live recording mode execution across all datasets in the playbook.
     */
    @Order(7)
    @AiJudge(false)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook("/verla/SearchTest.yaml")
    public void testSearchLiveAllDataSets()
    {
    }

    /**
     * Strict replay mode execution across all datasets in the playbook.
     */
    @Order(8)
    @AiJudge(false)
    @AiOutcomeVerification(false)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiPlaybook(value = "/verla/SearchTest.yaml", recordingMethod = "testSearchLiveAllDataSets")
    public void testSearchReplayAllDataSets()
    {
    }

    /**
     * Live recording mode execution for dataset 'perfect' with LLM Quality Judge and Semantic Outcome Verification combined.
     */
    @Order(9)
    @AiJudge(true)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("perfect")
    @AiPlaybook("/verla/SearchTest.yaml")
    public void testSearchLivePerfectWithJudgeAndOutcome()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'perfect' with LLM Quality Judge and Semantic Outcome Verification combined.
     */
    @Order(10)
    @AiJudge(true)
    @AiOutcomeVerification(true)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("perfect")
    @AiPlaybook(value = "/verla/SearchTest.yaml", recordingMethod = "testSearchLivePerfectWithJudgeAndOutcome")
    public void testSearchReplayPerfectWithJudgeAndOutcome()
    {
    }
}

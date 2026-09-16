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
package org.neodymium.ai.integration.verla.homepage.full;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;
import org.neodymium.ai.config.AiConfiguration;
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
 * Combined Quality Judge and Outcome Verification Homepage Navigation and Layout Verification Integration Test across multiple storefront datasets.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("integration")
@Tag("verla")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@Tag("flow:homepage")
@Tag("store:multi-dataset")
@Tag("mode:full")
@NeodymiumAiTest
@AiJudge(true)
@AiOutcomeVerification(value = true, failOnError = false)
@AiPlaybook(value = "verla/HomepageTest.yaml", recordingDirectory = "target/playbooks/integration/homepage")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VerlaFullHomepageTest extends BaseAiTest
{
    /**
     * Constructs a default VerlaFullHomepageTest instance.
     */
    public VerlaFullHomepageTest()
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
        Neodymium.getData().put("neodymium.ai.visualRca.enabled", "false");
        AiConfiguration.resetInstance();
    }

    /**
     * Live recording mode execution for dataset 'perfect'.
     */
    @Order(1)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("perfect")
    @AiPlaybook("verla/HomepageTest.yaml")
    public void testHomeLivePerfect()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'perfect'.
     */
    @Order(2)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("perfect")
    @AiPlaybook(value = "verla/HomepageTest.yaml", recordingMethod = "testHomeLivePerfect")
    public void testHomeReplayPerfect()
    {
    }

    /**
     * Live recording mode execution for dataset 'normal'.
     */
    @Order(3)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("normal")
    @AiPlaybook("verla/HomepageTest.yaml")
    public void testHomeLiveNormal()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'normal'.
     */
    @Order(4)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("normal")
    @AiPlaybook(value = "verla/HomepageTest.yaml", recordingMethod = "testHomeLiveNormal")
    public void testHomeReplayNormal()
    {
    }

    /**
     * Live recording mode execution for dataset 'bad'.
     */
    @Order(5)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("bad")
    @AiPlaybook("verla/HomepageTest.yaml")
    public void testHomeLiveBad()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'bad'.
     */
    @Order(6)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("bad")
    @AiPlaybook(value = "verla/HomepageTest.yaml", recordingMethod = "testHomeLiveBad")
    public void testHomeReplayBad()
    {
    }

    /**
     * Live recording mode execution across all datasets in the playbook.
     */
    @Order(7)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook("verla/HomepageTest.yaml")
    public void testHomeLiveAllDataSets()
    {
    }

    /**
     * Strict replay mode execution across all datasets in the playbook.
     */
    @Order(8)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiPlaybook(value = "verla/HomepageTest.yaml", recordingMethod = "testHomeLiveAllDataSets")
    public void testHomeReplayAllDataSets()
    {
    }
}

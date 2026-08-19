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
import org.junit.jupiter.api.TestMethodOrder;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.testing.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Runs YAML-based VERLA integration tests for product search and search result filtering in German
 * in recording mode first and strict replay mode second. This tests search but with a test 
 * script in German.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@NeodymiumAiTest
@AiPlaybook(value = "verla/SearchTest_German.yaml", recordingDirectory = "target/playbooks/integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class SearchTest_German extends BaseAiTest
{
    /**
     * Setup method to inject dynamic server URLs.
     */
    @BeforeEach
    public void setup()
    {
        Neodymium.getData().put("verla.url", String.format("https://localhost:%d", server.getHttpsPort()));
        Neodymium.getData().put("neodymium.ai.multilingual", "true");
    }

    /**
     * Live recording mode execution for dataset 'US'.
     */
    @Order(1)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("US")
    @AiPlaybook("/verla/SearchTest_German.yaml")
    public void testSearchDeLiveUs()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'US'.
     */
    @Order(2)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("US")
    @AiPlaybook(value = "/verla/SearchTest_German.yaml", recordingMethod = "testSearchDeLiveUs")
    public void testSearchDeReplayUs()
    {
    }

    /**
     * Live recording mode execution for dataset 'DE'.
     */
    @Order(3)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("DE")
    @AiPlaybook("/verla/SearchTest_German.yaml")
    public void testSearchDeLiveDe()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'DE'.
     */
    @Order(4)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("DE")
    @AiPlaybook(value = "/verla/SearchTest_German.yaml", recordingMethod = "testSearchDeLiveDe")
    public void testSearchDeReplayDe()
    {
    }

    /**
     * Live recording mode execution for dataset 'FIN'.
     */
    @Order(5)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("FIN")
    @AiPlaybook("/verla/SearchTest_German.yaml")
    public void testSearchDeLiveFin()
    {
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'FIN'.
     */
    @Order(6)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("FIN")
    @AiPlaybook(value = "/verla/SearchTest_German.yaml", recordingMethod = "testSearchDeLiveFin")
    public void testSearchDeReplayFin()
    {
    }

    /**
     * Live recording mode execution across all datasets in the playbook.
     */
    @Order(7)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook("/verla/SearchTest_German.yaml")
    public void testSearchDeLiveAllDataSets()
    {
    }

    /**
     * Strict replay mode execution across all datasets in the playbook.
     */
    @Order(8)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiPlaybook(value = "/verla/SearchTest_German.yaml", recordingMethod = "testSearchDeLiveAllDataSets")
    public void testSearchDeReplayAllDataSets()
    {
    }
}

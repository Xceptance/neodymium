/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

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
 * Guest Checkout Integration Test for Verla storefront across all datasets.
 * Tests LLM action generation, recording, and strict replay modes sequentially for each dataset
 * (perfect, normal, bad, modern-bad, modern-bad-nowcag) as well as dataset auto-replication.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VerlaGuestCheckoutIntegrationTest extends BaseAiTest
{
    /**
     * Constructs a default VerlaGuestCheckoutIntegrationTest.
     */
    public VerlaGuestCheckoutIntegrationTest()
    {
    }

    /**
     * Set up dynamic test parameters before each run.
     */
    @BeforeEach
    public void setup()
    {
        EmbeddedHtmlServer.resetInventory();
        // Resolve dynamic server port for the test execution
        Neodymium.getData().put("verla.url.host", String.format("localhost:%d", server.getPort()));
    }

    /**
     * Live mode execution with dynamic LLM query for dataset 'perfect'.
     */
    @Order(1)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("perfect")
    @AiPlaybook("/playbooks/integration/guest-checkout-verla.yaml")
    public void testCheckoutLivePerfect()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'perfect'.
     */
    @Order(2)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("perfect")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla.yaml", recordingMethod = "testCheckoutLivePerfect")
    public void testCheckoutReplayPerfect()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Replay with healing mode execution using recorded playbook for dataset 'perfect'.
     */
    @Order(3)
    @AiMode(ExecutionMode.REPLAY_WITH_HEALING)
    @AiDataSet("perfect")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla.yaml", recordingMethod = "testCheckoutLivePerfect")
    public void testCheckoutHealPerfect()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Live mode execution with dynamic LLM query for dataset 'normal'.
     */
    @Order(4)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("normal")
    @AiPlaybook("/playbooks/integration/guest-checkout-verla.yaml")
    public void testCheckoutLiveNormal()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'normal'.
     */
    @Order(5)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("normal")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla.yaml", recordingMethod = "testCheckoutLiveNormal")
    public void testCheckoutReplayNormal()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Replay with healing mode execution using recorded playbook for dataset 'normal'.
     */
    @Order(6)
    @AiMode(ExecutionMode.REPLAY_WITH_HEALING)
    @AiDataSet("normal")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla.yaml", recordingMethod = "testCheckoutLiveNormal")
    public void testCheckoutHealNormal()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Live mode execution with dynamic LLM query for dataset 'bad'.
     */
    @Order(7)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("bad")
    @AiPlaybook("/playbooks/integration/guest-checkout-verla.yaml")
    public void testCheckoutLiveBad()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'bad'.
     */
    @Order(8)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("bad")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla.yaml", recordingMethod = "testCheckoutLiveBad")
    public void testCheckoutReplayBad()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Replay with healing mode execution using recorded playbook for dataset 'bad'.
     */
    @Order(9)
    @AiMode(ExecutionMode.REPLAY_WITH_HEALING)
    @AiDataSet("bad")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla.yaml", recordingMethod = "testCheckoutLiveBad")
    public void testCheckoutHealBad()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Live mode execution with dynamic LLM query for dataset 'modern-bad'.
     */
    @Order(10)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("modern-bad")
    @AiPlaybook("/playbooks/integration/guest-checkout-verla.yaml")
    public void testCheckoutLiveModernBad()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'modern-bad'.
     */
    @Order(11)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("modern-bad")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla.yaml", recordingMethod = "testCheckoutLiveModernBad")
    public void testCheckoutReplayModernBad()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Replay with healing mode execution using recorded playbook for dataset 'modern-bad'.
     */
    @Order(12)
    @AiMode(ExecutionMode.REPLAY_WITH_HEALING)
    @AiDataSet("modern-bad")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla.yaml", recordingMethod = "testCheckoutLiveModernBad")
    public void testCheckoutHealModernBad()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Live mode execution with dynamic LLM query for dataset 'modern-bad-nowcag'.
     */
    @Order(13)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("modern-bad-nowcag")
    @AiPlaybook("/playbooks/integration/guest-checkout-verla.yaml")
    public void testCheckoutLiveModernBadNoWcag()
    {
        $("body").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Strict replay mode execution using recorded playbook for dataset 'modern-bad-nowcag'.
     */
    @Order(14)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("modern-bad-nowcag")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla.yaml", recordingMethod = "testCheckoutLiveModernBadNoWcag")
    public void testCheckoutReplayModernBadNoWcag()
    {
        $("body").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Replay with healing mode execution using recorded playbook for dataset 'modern-bad-nowcag'.
     */
    @Order(15)
    @AiMode(ExecutionMode.REPLAY_WITH_HEALING)
    @AiDataSet("modern-bad-nowcag")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla.yaml", recordingMethod = "testCheckoutLiveModernBadNoWcag")
    public void testCheckoutHealModernBadNoWcag()
    {
        $("body").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Live mode execution running with all datasets defined in the playbook.
     */
    @Order(16)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook("/playbooks/integration/guest-checkout-verla.yaml")
    public void testCheckoutLiveAllDataSets()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Strict replay mode execution running with all datasets defined in the playbook.
     */
    @Order(17)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla.yaml", recordingMethod = "testCheckoutLiveAllDataSets")
    public void testCheckoutReplayAllDataSets()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    /**
     * Healing replay mode execution running with all datasets defined in the playbook.
     */
    @Order(17)
    @AiMode(ExecutionMode.REPLAY_WITH_HEALING)
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla.yaml", recordingMethod = "testCheckoutLiveAllDataSets")
    public void testCheckoutHealAllDataSets()
    {
        $("body").shouldHave(text("Thank you for your purchase!"));
    }
}

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

import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.neodymium.ai.config.AiConfiguration;
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
 * German Language Guest Checkout Integration Test for Verla storefront.
 * Drives the US Verla SUT checkout flow using natural German step instructions across
 * all single-dataset execution modes (FORCE_RECORDING, REPLAY_STRICT, REPLAY_WITH_HEALING),
 * both with and without Quality Judge enabled.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VerlaGuestCheckoutGermanIntegrationTest extends BaseAiTest
{
    /**
     * Constructs a default VerlaGuestCheckoutGermanIntegrationTest.
     */
    public VerlaGuestCheckoutGermanIntegrationTest()
    {
    }

    /**
     * Set up dynamic test parameters and judge configuration before each run.
     *
     * @param testInfo the JUnit TestInfo context
     */
    @BeforeEach
    public void setup(final TestInfo testInfo)
    {
        EmbeddedHtmlServer.resetInventory();
        Neodymium.getData().put("verla.url", String.format("https://localhost:%d", server.getHttpsPort()));

        final String methodName = testInfo.getTestMethod().map(Method::getName).orElse("");
        if (methodName.contains("WithJudge"))
        {
            System.setProperty("neodymium.ai.judge.enabled", "true");
        }
        else
        {
            System.setProperty("neodymium.ai.judge.enabled", "false");
        }
        AiConfiguration.resetInstance();
    }

    // =========================================================================
    // Dataset 'perfect' Single Executions
    // =========================================================================

    @Order(1)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("perfect")
    @AiPlaybook("/playbooks/integration/guest-checkout-verla-de.yaml")
    public void testCheckoutLivePerfect()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    @Order(2)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("perfect")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla-de.yaml", recordingMethod = "testCheckoutLivePerfect")
    public void testCheckoutReplayPerfect()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    @Order(3)
    @AiMode(ExecutionMode.REPLAY_WITH_HEALING)
    @AiDataSet("perfect")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla-de.yaml", recordingMethod = "testCheckoutLivePerfect")
    public void testCheckoutHealPerfect()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    // =========================================================================
    // Dataset 'normal' Single Executions
    // =========================================================================

    @Order(4)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("normal")
    @AiPlaybook("/playbooks/integration/guest-checkout-verla-de.yaml")
    public void testCheckoutLiveNormal()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    @Order(5)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("normal")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla-de.yaml", recordingMethod = "testCheckoutLiveNormal")
    public void testCheckoutReplayNormal()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    @Order(6)
    @AiMode(ExecutionMode.REPLAY_WITH_HEALING)
    @AiDataSet("normal")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla-de.yaml", recordingMethod = "testCheckoutLiveNormal")
    public void testCheckoutHealNormal()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    // =========================================================================
    // Dataset 'bad' Single Executions
    // =========================================================================

    @Order(7)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("bad")
    @AiPlaybook("/playbooks/integration/guest-checkout-verla-de.yaml")
    public void testCheckoutLiveBad()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    @Order(8)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("bad")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla-de.yaml", recordingMethod = "testCheckoutLiveBad")
    public void testCheckoutReplayBad()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    @Order(9)
    @AiMode(ExecutionMode.REPLAY_WITH_HEALING)
    @AiDataSet("bad")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla-de.yaml", recordingMethod = "testCheckoutLiveBad")
    public void testCheckoutHealBad()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    // =========================================================================
    // Dataset 'modern-bad' Single Executions
    // =========================================================================

    @Order(10)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("modern-bad")
    @AiPlaybook("/playbooks/integration/guest-checkout-verla-de.yaml")
    public void testCheckoutLiveModernBad()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    @Order(11)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("modern-bad")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla-de.yaml", recordingMethod = "testCheckoutLiveModernBad")
    public void testCheckoutReplayModernBad()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    @Order(12)
    @AiMode(ExecutionMode.REPLAY_WITH_HEALING)
    @AiDataSet("modern-bad")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla-de.yaml", recordingMethod = "testCheckoutLiveModernBad")
    public void testCheckoutHealModernBad()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    // =========================================================================
    // Dataset 'modern-bad-nowcag' Single Executions
    // =========================================================================

    @Order(13)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("modern-bad-nowcag")
    @AiPlaybook("/playbooks/integration/guest-checkout-verla-de.yaml")
    public void testCheckoutLiveModernBadNoWcag()
    {
        $("body").shouldHave(text("Thank you for your purchase!"));
    }

    @Order(14)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("modern-bad-nowcag")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla-de.yaml", recordingMethod = "testCheckoutLiveModernBadNoWcag")
    public void testCheckoutReplayModernBadNoWcag()
    {
        $("body").shouldHave(text("Thank you for your purchase!"));
    }

    @Order(15)
    @AiMode(ExecutionMode.REPLAY_WITH_HEALING)
    @AiDataSet("modern-bad-nowcag")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla-de.yaml", recordingMethod = "testCheckoutLiveModernBadNoWcag")
    public void testCheckoutHealModernBadNoWcag()
    {
        $("body").shouldHave(text("Thank you for your purchase!"));
    }

    // =========================================================================
    // Quality Judge Mode Variants (neodymium.ai.judge.enabled=true)
    // =========================================================================

    @Order(16)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("perfect")
    @AiPlaybook("/playbooks/integration/guest-checkout-verla-de.yaml")
    public void testCheckoutLivePerfectWithJudge()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    @Order(17)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiDataSet("perfect")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla-de.yaml", recordingMethod = "testCheckoutLivePerfectWithJudge")
    public void testCheckoutReplayPerfectWithJudge()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }

    @Order(18)
    @AiMode(ExecutionMode.REPLAY_WITH_HEALING)
    @AiDataSet("perfect")
    @AiPlaybook(value = "/playbooks/integration/guest-checkout-verla-de.yaml", recordingMethod = "testCheckoutLivePerfectWithJudge")
    public void testCheckoutHealPerfectWithJudge()
    {
        $("h2").shouldHave(text("Thank you for your purchase!"));
    }
}

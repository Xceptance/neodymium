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
package org.neodymium.ai.integration.verla.checkout.i18n.ca.basic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiDataFile;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiJudge;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiOutcomeVerification;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.playbook.PlaybookParser;
import org.neodymium.ai.playbook.YamlPlaybookParser;
import org.neodymium.ai.prompt.VisualRcaResult;
import org.neodymium.ai.resources.ClasspathResourceManager;
import org.neodymium.ai.resources.PlaybookResourceManager;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * French Canadian Guest Checkout Integration Test verifying that when a required field (province)
 * is missing from the checkout form, execution fails in the SUT sense and triggers Visual RCA diagnosis
 * while remaining green as a JUnit test.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("integration")
@Tag("verla")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@Tag("flow:checkout")
@Tag("store:i18n-ca")
@Tag("mode:basic")
@Tag("rca")
@NeodymiumAiTest
@AiJudge(false)
@AiOutcomeVerification(value = false, failOnError = false)
@AiPlaybook(AiPlaybook.PROGRAMMATIC)
@AiDataFile("playbooks/integration/VerlaGuestCheckout_CaFr_French_MissingProvinceBug.yaml")
public class FrenchMissingProvinceCheckoutTest extends BaseAiTest
{
    /**
     * Constructs a default FrenchMissingProvinceCheckoutTest instance.
     */
    public FrenchMissingProvinceCheckoutTest()
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
        Neodymium.getData().put("neodymium.ai.visualRca.enabled", "true");
    }

    /**
     * Executes the missing province Canadian French checkout playbook programmatically,
     * verifying that the checkout fails conclusively at the purchase step, triggers Visual RCA,
     * and produces a diagnosis explaining the form validation blockage without failing the JUnit test build.
     *
     * @param session the thread-isolated AiSession injected by NeodymiumAiRunner
     * @throws Exception if unexpected runtime error occurs during setup
     */
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiDataSet("perfect")
    @AiPlaybook(AiPlaybook.PROGRAMMATIC)
    public void testMissingProvinceFailureTriggersVisualRca(final AiSession session) throws Exception
    {
        session.getSessionData().set("verla.url", String.format("https://localhost:%d", server.getHttpsPort()));

        final PlaybookResourceManager resourceManager = new ClasspathResourceManager();
        final PlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("playbooks/integration/VerlaGuestCheckout_CaFr_French_MissingProvinceBug.yaml", resourceManager);

        final Throwable failure = Assertions.assertThrows(Throwable.class, () -> session.execute(playbook, session.getSessionData()));

        Assertions.assertNotNull(failure, "Playbook execution was expected to fail because the missing province prevents checkout completion.");

        final ExecutionContext context = session.getExecutionContext();
        final String rcaExplanation = (String) context.getTransientData().get(ExecutionContext.KEY_VISUAL_RCA_EXPLANATION);

        Assertions.assertNotNull(rcaExplanation, "Visual RCA diagnosis should be populated in ExecutionContext transient data.");
        Assertions.assertFalse(rcaExplanation.isBlank(), "Visual RCA diagnosis should not be blank.");
        Assertions.assertTrue(session.getMetrics().getRcaCallCount() >= 1, "RCA call count metric should be at least 1.");

        final Object rcaResultObj = context.getTransientData().get(ExecutionContext.KEY_VISUAL_RCA_RESULT);
        Assertions.assertTrue(rcaResultObj instanceof VisualRcaResult, "Visual RCA result should be an instance of VisualRcaResult.");
        final VisualRcaResult rcaResult = (VisualRcaResult) rcaResultObj;
        Assertions.assertNotNull(rcaResult.getRootCause(), "Visual RCA root cause should not be null.");
        Assertions.assertFalse(rcaResult.getRootCause().isBlank(), "Visual RCA root cause should not be blank.");
        Assertions.assertNotNull(rcaResult.getRubrics(), "Visual RCA rubrics container should not be null.");
        Assertions.assertNotNull(rcaResult.getRubrics().formValidation(), "Form validation rubric should be evaluated.");
        Assertions.assertEquals("ERROR_PRESENT", rcaResult.getRubrics().formValidation().score(), "Form validation rubric should report ERROR_PRESENT.");
        Assertions.assertTrue(
            rcaResult.getRootCause().toLowerCase().contains("province") ||
            rcaResult.getRubrics().formValidation().analysis().toLowerCase().contains("province"),
            "Visual RCA diagnosis should explicitly identify the omitted province field as the root cause."
        );
    }
}

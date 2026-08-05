package com.xceptance.neodymium.aura.manager.ui;

import org.junit.jupiter.api.Tag;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;

import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import com.xceptance.neodymium.common.testdata.DataSet;

/**
 * Playbook-based UI test for Aura Manager test execution.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@NeodymiumAiTest
public class AuraManagerExecutionPlaybookUiTest extends BaseAuraManagerUiTest
{
    public AuraManagerExecutionPlaybookUiTest()
    {
        super(18140);
    }

    @AiPlaybook("ai-test-pages/aura-manager-execution-test.yaml")
    @DataSet(id = "Toggle_Config")
    public void testToggleConfig() throws Throwable
    {
    }
}

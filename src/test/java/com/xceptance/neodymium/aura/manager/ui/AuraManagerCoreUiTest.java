package com.xceptance.neodymium.aura.manager.ui;

import org.junit.jupiter.api.Tag;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;

import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import com.xceptance.neodymium.common.testdata.DataSet;

/**
 * Playbook-based UI test for Aura Manager core theme toggling and view switching.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@NeodymiumAiTest
public class AuraManagerCoreUiTest extends BaseAuraManagerUiTest
{
    public AuraManagerCoreUiTest()
    {
        super(18120);
    }

    @AiPlaybook("ai-test-pages/aura-manager-core-ui-theme-test.yaml")
    @DataSet(id = "Theme_Toggling")
    public void testThemeToggling() throws Throwable
    {
    }

    @AiPlaybook("ai-test-pages/aura-manager-core-ui-view-test.yaml")
    @DataSet(id = "View_Switching")
    public void testViewSwitching() throws Throwable
    {
    }
}

package com.xceptance.neodymium.aura.manager.ui;

import org.junit.jupiter.api.Tag;

import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Playbook-based UI test for Aura Manager test execution.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@DataFile("ai-test-pages/aura-manager-execution-test.yaml")
public class AuraManagerExecutionPlaybookUiTest extends BaseAuraManagerUiTest
{
    public AuraManagerExecutionPlaybookUiTest()
    {
        super(18140);
    }

    @NeodymiumTest
    @DataSet(id = "Toggle_Config")
    public void testToggleConfig() throws Throwable
    {
        Neodymium.ai().execute();
    }
}

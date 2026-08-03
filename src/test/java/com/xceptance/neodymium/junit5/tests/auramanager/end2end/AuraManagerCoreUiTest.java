package com.xceptance.neodymium.junit5.tests.auramanager.end2end;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import org.neodymium.ai.junit.NeodymiumAiTest;

@DataFile("ai-test-pages/aura-manager-core-ui-test.yaml")
@NeodymiumAiTest
public class AuraManagerCoreUiTest extends BaseAuraManagerUiTest
{
    public AuraManagerCoreUiTest()
    {
        super(18120);
    }

    @NeodymiumTest
    @DataSet(id = "Theme_Toggling")
    public void testThemeToggling() throws Throwable
    {
    }

    @NeodymiumTest
    @DataSet(id = "View_Switching")
    public void testViewSwitching() throws Throwable
    {
    }
}

package com.xceptance.neodymium.junit5.tests.auramanager.end2end;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import org.neodymium.ai.junit.NeodymiumAiTest;

@DataFile("ai-test-pages/aura-manager-execution-test.yaml")
@NeodymiumAiTest
public class AuraManagerExecutionTest extends BaseAuraManagerUiTest
{
    public AuraManagerExecutionTest()
    {
        super(18130);
    }

    @NeodymiumTest
    @DataSet(id = "Toggle_Config")
    public void testToggleConfig() throws Throwable
    {
    }
}

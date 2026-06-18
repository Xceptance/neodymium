package com.xceptance.neodymium.junit5.tests.auramanager.end2end;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@DataFile("ai-test-pages/aura-manager-history-test.yaml")
public class AuraManagerHistoryTest extends BaseAuraManagerUiTest
{
    public AuraManagerHistoryTest()
    {
        super(18140);
    }
    @NeodymiumTest
    @DataSet(id = "History_List")
    public void testHistoryList()
    {
        assertMultiPhaseExecution();
    }
}

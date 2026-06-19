package com.xceptance.neodymium.junit5.tests.auramanager.end2end;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@DataFile("ai-test-pages/aura-manager-chat-test.yaml")
public class AuraManagerChatTest extends BaseAuraManagerUiTest
{
    public AuraManagerChatTest()
    {
        super(18100);
    }
    @NeodymiumTest
    @DataSet(id = "Chat_Message")
    public void testChatMessage() throws Throwable
    {
        Neodymium.ai().execute();
    }
}

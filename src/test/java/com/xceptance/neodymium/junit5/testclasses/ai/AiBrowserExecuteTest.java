package com.xceptance.neodymium.junit5.testclasses.ai;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_1024x768")
public class AiBrowserExecuteTest {
    @NeodymiumTest
    public void testAiBrowserConnection() {
        Neodymium.ai().execute("Evaluate something complex to trigger LLM");
    }
}

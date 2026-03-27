package com.xceptance.neodymium.junit4.testclasses.ai;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser
public class AiBrowserExecuteTest
{
    @Test
    public void testAiBrowserConnection()
    {
        Neodymium.ai().execute("Evaluate something complex to trigger LLM");
    }
}

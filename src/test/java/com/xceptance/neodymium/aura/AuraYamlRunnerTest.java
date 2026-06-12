package com.xceptance.neodymium.aura;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.testdata.DataFolder;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser()
@DataFolder(".")
public final class AuraYamlRunnerTest
{
    @NeodymiumTest
    public final void executeYamlTest() throws Throwable
    {
        Neodymium.ai().execute();
    }
}

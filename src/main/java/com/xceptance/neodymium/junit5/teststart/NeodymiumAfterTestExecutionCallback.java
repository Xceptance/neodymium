package com.xceptance.neodymium.junit5.teststart;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.util.Neodymium;

public class NeodymiumAfterTestExecutionCallback implements AfterTestExecutionCallback
{
    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception
    {
        AiBrowser aiBrowser = Neodymium.ai();
        if (aiBrowser != null)
        {
            aiBrowser.close();
        }
    }
}

package com.xceptance.neodymium.junit5.teststart;

import com.xceptance.neodymium.util.NeodymiumRandom;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static com.xceptance.neodymium.util.NeodymiumRandom.reinitializeRandomSeed;

public class NeodymiumBeforeTestExecutionCallback implements BeforeTestExecutionCallback
{
    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception
    {
        // reset the random seed so every test starts with the same values for better reproducibility
        reinitializeRandomSeed(NeodymiumRandom.SeedState.INITIALIZED);
    }
}

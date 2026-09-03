package org.neodymium.junit5.teststart;

import org.neodymium.util.Neodymium;
import org.neodymium.util.NeodymiumRandom;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.neodymium.util.NeodymiumRandom.reinitializeRandomSeed;

public class NeodymiumBeforeTestExecutionCallback implements BeforeTestExecutionCallback
{
    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception
    {
        // reset the random seed so every test starts with the same values for better reproducibility
        reinitializeRandomSeed(NeodymiumRandom.SeedState.INITIALIZED);
        
        // exact unified test name calculation
        Neodymium.setTestName(context.getRequiredTestClass().getCanonicalName() + " :: " + context.getDisplayName());
    }
}

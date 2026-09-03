package com.xceptance.neodymium.junit5.tests;

import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.config.SystemPropertyTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class SystemConfigurationTest extends AbstractNeodymiumTest
{
    @Test
    public void testSystemPropertiesOverrideNeoConfig()
    {
        NeodymiumTestExecutionSummary summary = run(SystemPropertyTest.class);
        checkPass(summary, 1, 0);
    }
}

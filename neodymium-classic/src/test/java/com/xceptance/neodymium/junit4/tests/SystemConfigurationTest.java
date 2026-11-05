package com.xceptance.neodymium.junit4.tests;

import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.config.SystemPropertyTest;

public class SystemConfigurationTest extends NeodymiumTest
{
    @Test
    public void testNeoConfig()
    {
        Result result = run(SystemPropertyTest.class);
        checkPass(result, 1, 0);
    }
}

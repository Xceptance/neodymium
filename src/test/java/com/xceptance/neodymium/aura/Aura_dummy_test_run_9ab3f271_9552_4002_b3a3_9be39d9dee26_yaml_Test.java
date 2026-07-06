package com.xceptance.neodymium.aura;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.testdata.DataFolder;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.jupiter.api.DisplayName;

@Browser()
@DataFolder(".")
@DisplayName("YAML Test: dummy-test-run-9ab3f271-9552-4002-b3a3-9be39d9dee26.yaml")
public final class Aura_dummy_test_run_9ab3f271_9552_4002_b3a3_9be39d9dee26_yaml_Test
{
    @NeodymiumTest
    public final void executeYamlTest() throws Throwable
    {
        Neodymium.ai().execute();
    }
}

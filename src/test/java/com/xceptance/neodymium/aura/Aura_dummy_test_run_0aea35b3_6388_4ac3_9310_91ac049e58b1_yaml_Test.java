package com.xceptance.neodymium.aura;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.testdata.DataFolder;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.jupiter.api.DisplayName;

@Browser()
@DataFolder(".")
@DisplayName("YAML Test: dummy-test-run-0aea35b3-6388-4ac3-9310-91ac049e58b1.yaml")
public final class Aura_dummy_test_run_0aea35b3_6388_4ac3_9310_91ac049e58b1_yaml_Test
{
    @NeodymiumTest
    public final void executeYamlTest() throws Throwable
    {
        Neodymium.ai().execute();
    }
}

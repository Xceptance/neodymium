package com.xceptance.neodymium.aura;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.testdata.DataFolder;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.jupiter.api.DisplayName;

@Browser()
@DataFolder(".")
@DisplayName("YAML Test: dummy-test-run-fc818a0c-47f0-42ee-9ff2-13abd01b2947.yaml")
public final class Aura_dummy_test_run_fc818a0c_47f0_42ee_9ff2_13abd01b2947_yaml_Test
{
    @NeodymiumTest
    public final void executeYamlTest() throws Throwable
    {
        Neodymium.ai().execute();
    }
}

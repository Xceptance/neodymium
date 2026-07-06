package com.xceptance.neodymium.aura;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.testdata.DataFolder;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.jupiter.api.DisplayName;

@Browser()
@DataFolder(".")
@DisplayName("YAML Test: dummy-test-run-facf21ff-f011-4c9d-a717-20b01724f201.yaml")
public final class Aura_dummy_test_run_facf21ff_f011_4c9d_a717_20b01724f201_yaml_Test
{
    @NeodymiumTest
    public final void executeYamlTest() throws Throwable
    {
        Neodymium.ai().execute();
    }
}

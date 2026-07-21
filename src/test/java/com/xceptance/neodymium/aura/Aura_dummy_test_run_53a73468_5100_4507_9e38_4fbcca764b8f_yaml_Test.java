package com.xceptance.neodymium.aura;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.testdata.DataFolder;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.jupiter.api.DisplayName;

@Browser()
@DataFolder(".")
@DisplayName("YAML Test: dummy-test-run-53a73468-5100-4507-9e38-4fbcca764b8f.yaml")
public final class Aura_dummy_test_run_53a73468_5100_4507_9e38_4fbcca764b8f_yaml_Test
{
    @NeodymiumTest
    public final void executeYamlTest() throws Throwable
    {
        Neodymium.ai().execute();
    }
}

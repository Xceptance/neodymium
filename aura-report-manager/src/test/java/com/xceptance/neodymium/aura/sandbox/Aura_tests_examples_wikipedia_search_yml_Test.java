package com.xceptance.neodymium.aura.sandbox;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.testdata.DataFolder;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.junit.jupiter.api.DisplayName;

@Browser("Chrome_1024x768")
@DataFolder(".")
@NeodymiumAiTest
@DisplayName("YAML Test: tests/examples/wikipedia_search.yml")
public final class Aura_tests_examples_wikipedia_search_yml_Test
{
    @NeodymiumTest
    public final void executeYamlTest() throws Throwable
    {
    }
}

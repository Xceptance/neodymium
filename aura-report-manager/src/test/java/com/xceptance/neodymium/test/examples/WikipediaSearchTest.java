package com.xceptance.neodymium.test.examples;

import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import com.xceptance.neodymium.common.browser.Browser;

/**
 * JUnit 5 test runner for the Wikipedia Search example using Neodymium AI v2 framework.
 */
@Browser("Chrome_1500x1000")
@NeodymiumAiTest
public final class WikipediaSearchTest
{
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook("tests/examples/wikipedia_search.yml")
    public final void executeWikipediaSearch()
    {
    }
}

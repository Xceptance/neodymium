package com.xceptance.neodymium.test.examples;

import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;

import com.xceptance.neodymium.common.browser.Browser;

/**
 * JUnit 5 test runner for the Wikipedia Search example using Neodymium AI v2 framework.
 */
@Browser("Chrome_1500x1000")
@NeodymiumAiTest
public final class ProgrammaticPlaybookTest
{
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook(AiPlaybook.PROGRAMMATIC)
    public void wikipediaSearchTest(final AiSession session) throws Exception
    {
        session.execute("""
                                    steps: |
                                        Open https://www.wikipedia.org
                                        Select the 'English' language
                                        Type 'Neodymium' in the Search input field
                                        Press Enter in the Search input field
                                        Verify the main page heading contains 'Neodymium'
                            """);
    }
}

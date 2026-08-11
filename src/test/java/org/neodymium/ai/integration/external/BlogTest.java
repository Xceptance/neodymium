package org.neodymium.ai.integration.external;

import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiInlinePlaybook;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.common.browser.Browser;

@NeodymiumAiTest
@Browser("Chrome_1500x1000")
public class BlogTest
{
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiInlinePlaybook(
        """
            steps: |
                Open 'https://blog.xceptance.com/'.
                Search for 'Neodymium'.
                'Search Results for: neodymium' is displayed.
        """
     )
    public void bruteforceSearch(final AiSession session)
    {
    }

    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiInlinePlaybook(
        """
            steps: |
                Open 'https://blog.xceptance.com/'.
                Open the search input by clicking the magnifyiing glass icon.
                Type 'neodymium' into the search field.
                Press enter.
                'Search Results for: neodymium' is displayed.
        """
     )
    public void preciseSearch(final AiSession session)
    {
    }
}
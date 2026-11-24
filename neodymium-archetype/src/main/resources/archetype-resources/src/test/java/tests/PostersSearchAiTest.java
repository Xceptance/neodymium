package ${package}.tests;

import org.junit.jupiter.api.Tag;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;

/**
 * Executes the Posters Demo Store product search playbook.
 * Demonstrates natural language step execution, data-driven parameterization,
 * reusable step includes, and search result validation.
 */
@Browser("Chrome_1500x1000")
@AiPlaybook("tests/posters_search.yaml")
@NeodymiumAiTest
@Tag("regression")
@Tag("storefront")
@Tag("search")
public class PostersSearchAiTest
{
    /**
     * Runs the Posters search AI playbook.
     */
    @NeodymiumTest
    public void testPostersSearch()
    {
    }
}

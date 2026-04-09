package com.xceptance.neodymium.junit5.testclasses.ai;

import org.junit.jupiter.api.Test;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_headless")
public class AiBrowserPlaybookTest {
    
    @NeodymiumTest
    public void testPlaybookReplay() {
        // This implicitly reads the manually created JSON file for this test
        // and succeeds without hitting the LLM as it's purely a NAVIGATE action
        // navigated via Replay Mode.
        Neodymium.ai().execute("Click on the dummy button");
    }
}

package com.xceptance.neodymium.junit4.testclasses.ai;

import org.junit.Test;
import org.junit.runner.RunWith;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class AiBrowserPlaybookTest {
    
    @Test
    public void testPlaybookReplay() {
        // This implicitly reads the manually created JSON file for this test
        // and succeeds without hitting the LLM as it's purely a NAVIGATE action
        // navigated via Replay Mode.
        Neodymium.ai().execute("Click on the dummy button");
    }
}

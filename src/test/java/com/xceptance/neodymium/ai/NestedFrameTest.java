package com.xceptance.neodymium.ai;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_1024x768")
public class NestedFrameTest extends BaseAiTest {

    @NeodymiumTest
    public void testNestedFrames() {
        com.codeborne.selenide.Selenide.open(currentTestUrl);
        Neodymium.ai().execute("Type 'Hello' into the input field in the first frame. Then click the Deep Button in the second frame. Finally verify that the result text 'Button was clicked!' is shown.");
    }
}

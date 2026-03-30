package com.xceptance.neodymium.junit5.testclasses.ai;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

public class AiBrowserStateTest {
    @NeodymiumTest
    public void testAiBrowserInitialized() {
        Assertions.assertNotNull(Neodymium.ai(), "AiBrowser should be initialized and bound to the thread context");
    }
}

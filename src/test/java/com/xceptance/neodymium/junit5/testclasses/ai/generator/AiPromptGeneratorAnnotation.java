package com.xceptance.neodymium.junit5.testclasses.ai.generator;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.junit5.NeodymiumTestGenerator;
import com.xceptance.neodymium.util.Neodymium;

public class AiPromptGeneratorAnnotation {
    @NeodymiumTestGenerator
    public void testNeodymiumTestGeneratorAnnotation() {
        // This confirms that the test runner correctly executes this method
        // with the proper Neodymium Context initialized by the NeodymiumRunner
        Assertions.assertNotNull(Neodymium.configuration(), "Neodymium configuration should be injected and not null");
    }
}

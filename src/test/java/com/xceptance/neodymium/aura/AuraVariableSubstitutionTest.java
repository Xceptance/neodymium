package com.xceptance.neodymium.aura;

import org.junit.Test;
import org.junit.runner.RunWith;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.core.AiAgent;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.Assert;
import java.util.List;
import java.util.ArrayList;

@RunWith(NeodymiumRunner.class)
public class AuraVariableSubstitutionTest {
    
    @Test
    public void testVariableSubstitution() throws Throwable {
        String rawSteps = Neodymium.getData().asString("raw_steps");
        System.out.println("Raw steps from YAML data provider: " + rawSteps);
        
        Assert.assertNotNull("raw_steps should exist!", rawSteps);
        Assert.assertTrue("YAML should preserve variables in raw_steps!", rawSteps.contains("${myVar}"));
        
        // Test resolution
        List<com.xceptance.neodymium.ai.core.LookupDetails> lookups = new ArrayList<>();
        String resolved = AiBrowser.resolveTestDataToPrompt(rawSteps, lookups);
        System.out.println("Resolved text: " + resolved);
        Assert.assertTrue("YAML should substitute variables during resolve!", resolved.contains("https://www.wikipedia.org"));
    }
}

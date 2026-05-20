package com.xceptance.neodymium.junit5.testclasses.ai;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

public class AiBrowserDataResolutionTest
{
    @NeodymiumTest
    @DataSet(id = "NestedObjectTest")
    public void testDeepNestedResolution()
    {
        // Deep nested property (3 layers: user -> profile -> preferences -> theme)
        String template = "Theme is ${user.profile.preferences.theme}";
        String resolved = AiBrowser.resolveTestDataToPrompt(template);
        Assertions.assertEquals("Theme is dark", resolved);
    }

    @NeodymiumTest
    @DataSet(id = "NestedObjectTest")
    public void testJsonPathNotation()
    {
        // json path notation: [key]
        String template = "Theme is ${user[profile][preferences][theme]}";
        String resolved = AiBrowser.resolveTestDataToPrompt(template);
        Assertions.assertEquals("Theme is dark", resolved);
    }

    @NeodymiumTest
    @DataSet(id = "NestedObjectTest")
    public void testNestedPlaceholderResolution()
    {
        String template = "Greeting: ${user.profile.preferences.greetingTemplate}";
        String resolved = AiBrowser.resolveTestDataToPrompt(template);
        Assertions.assertEquals("Greeting: Welcome to dark theme!", resolved);
    }

    @NeodymiumTest
    @DataSet(id = "NestedObjectTest")
    public void testSimpleResolution()
    {
        String template = "Simple ${reference}";
        String resolved = AiBrowser.resolveTestDataToPrompt(template);
        Assertions.assertEquals("Simple value", resolved);
    }
}

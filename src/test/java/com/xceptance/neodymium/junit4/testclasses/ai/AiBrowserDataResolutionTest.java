package com.xceptance.neodymium.junit4.testclasses.ai;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class AiBrowserDataResolutionTest
{
    @Test
    @DataSet(id = "NestedObjectTest")
    public void testDeepNestedResolution()
    {
        // Deep nested property (3 layers: user -> profile -> preferences -> theme)
        String template = "Theme is ${user.profile.preferences.theme}";
        String resolved = AiBrowser.resolveTestDataToPrompt(template);
        Assert.assertEquals("Theme is dark", resolved);
    }

    @Test
    @DataSet(id = "NestedObjectTest")
    public void testJsonPathNotation()
    {
        // json path notation: [key]
        String template = "Theme is ${user[profile][preferences][theme]}";
        String resolved = AiBrowser.resolveTestDataToPrompt(template);
        Assert.assertEquals("Theme is dark", resolved);
    }

    @Test
    @DataSet(id = "NestedObjectTest")
    public void testNestedPlaceholderResolution()
    {
        String template = "Greeting: ${user.profile.preferences.greetingTemplate}";
        String resolved = AiBrowser.resolveTestDataToPrompt(template);
        Assert.assertEquals("Greeting: Welcome to dark theme!", resolved);
    }

    @Test
    @DataSet(id = "NestedObjectTest")
    public void testSimpleResolution()
    {
        String template = "Simple ${reference}";
        String resolved = AiBrowser.resolveTestDataToPrompt(template);
        Assert.assertEquals("Simple value", resolved);
    }
}

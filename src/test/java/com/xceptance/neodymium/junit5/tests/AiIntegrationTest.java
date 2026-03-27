package com.xceptance.neodymium.junit5.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.ai.AiBrowserDirectExecutionTest;
import com.xceptance.neodymium.junit5.testclasses.ai.AiBrowserExecuteTest;
import com.xceptance.neodymium.junit5.testclasses.ai.AiBrowserStateTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class AiIntegrationTest extends AbstractNeodymiumTest
{
    @Test
    public void testAiBrowserIsSet()
    {
        NeodymiumTestExecutionSummary result = run(AiBrowserStateTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testMissingKeyProvidesUsefulErrorMessage()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.ai.apiKey", ""); // Overrides with empty string
        addPropertiesForTest("testMissingKeyProvidesUsefulErrorMessage.properties", properties);

        NeodymiumTestExecutionSummary result = run(AiBrowserExecuteTest.class);

        checkFail(result, 1, 0, 1, "AI API key not configured. Set in your ai.properties, neodymium.properties or as an evironment variable.");
    }

    @Test
    public void testInvalidKeyFailsFastFromAPI()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.ai.apiKey", "invalid_mock_key_123");
        addPropertiesForTest("testInvalidKeyFailsFastFromAPI.properties", properties);

        NeodymiumTestExecutionSummary result = run(AiBrowserExecuteTest.class);
        checkFail(result, 1, 0, 1, "Instruction 'Evaluate something complex to trigger LLM");

        Assertions.assertTrue(result.getFailures().get(0).getException().getCause().getMessage().contains("API key not valid"),
                              "The exception trace should indicate an API rejection due to the invalid key (e.g. 'API key not valid').");
    }

    @Test
    public void testDirectExecutionWithoutValidKey()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.ai.apiKey", "invalid_mock_key_123");
        addPropertiesForTest("testDirectExecutionWithoutValidKey.properties", properties);

        NeodymiumTestExecutionSummary result = run(AiBrowserDirectExecutionTest.class);
        checkPass(result, 5, 0);
    }
}

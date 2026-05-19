package com.xceptance.neodymium.junit5.testclasses.ai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.List;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.generator.AiPromptGenerator;
import com.xceptance.neodymium.util.Neodymium;

public class AiPromptGeneratorTest
{
    @Test
    public void testGeneratorFailsWhenPropertyDisabled()
    {
        // Ensure flag is explicitly disabled
        Neodymium.aiConfiguration().setProperty("neodymium.ai.generate", "false");

        AiPromptGenerator generator = new AiPromptGenerator();
        
        AssertionFailedError error = Assertions.assertThrows(AssertionFailedError.class, () -> {
            generator.generate("https://example.com", "intent", null, "output.yml");
        });
        
        Assertions.assertTrue(error.getMessage().contains("neodymium.ai.generate=true"));
    }

    @Test
    public void testGeneratorLogsWarningAndDisablesPlaybookWhenEnabled()
    {
        // Temporarily enable for test
        boolean originalValue = Neodymium.aiConfiguration().aiGenerateEnabled();
        boolean originalPlaybookValue = Neodymium.aiConfiguration().playbookRecordEnabled();
        try
        {
            Neodymium.aiConfiguration().setProperty("neodymium.ai.generate", "true");
            Neodymium.aiConfiguration().setProperty("neodymium.ai.playbook.record", "true");

            class TestableGenerator extends AiPromptGenerator {

                @Override
                public List<Action> explore(com.xceptance.neodymium.ai.core.LlmClient llmClient, String url, String intent, String sutContext, String outputPath) { return new java.util.ArrayList<>(); }
            }

            TestableGenerator generator = new TestableGenerator();
            // Should not throw any assertion
            generator.generate("https://example.com", "intent", null, "output.yml");

            // Verify Playbook JSON recording was disabled by the generator
            Assertions.assertFalse(Neodymium.aiConfiguration().playbookRecordEnabled());
        }
        finally
        {
            // Restore config
            Neodymium.aiConfiguration().setProperty("neodymium.ai.generate", String.valueOf(originalValue));
            Neodymium.aiConfiguration().setProperty("neodymium.ai.playbook.record", String.valueOf(originalPlaybookValue));
        }
    }


    @Test
    public void testExploreTwoTurnConfirmation()
    {
        boolean originalValue = Neodymium.aiConfiguration().aiGenerateEnabled();
        try
        {
            Neodymium.aiConfiguration().setProperty("neodymium.ai.generate", "true");
            
            class TestableGenerator extends AiPromptGenerator {
                public int callCount = 0;
                
                @Override
                protected void openBrowser(String url) {}
                
                @Override
                protected String captureDom(com.xceptance.neodymium.ai.core.PageAnalyzer analyzer) { return "<html></html>"; }
                
                @Override
                protected void executeAction(com.xceptance.neodymium.ai.action.ActionExecutor executor, Action action) {}

                @Override
                protected String executeExplorationLlmCall(com.xceptance.neodymium.ai.core.LlmClient llmClient, String prompt) {
                    callCount++;
                    if (callCount == 1) {
                        return "{\"actions\": [{\"type\": \"CLICK\", \"target\": \"#btn\", \"description\": \"Click button\"}]}";
                    } else if (callCount == 2) {
                        // AI reports failure of previous action
                        return "{\"dropLastNActions\": 1, \"actions\": [{\"type\": \"NAVIGATE\", \"value\": \"http://example.com\", \"description\": \"try again\"}]}";
                    } else {
                        return "{\"previousActionSuccess\": true, \"overallIntentAchieved\": true}";
                    }
                }
                
                @Override
                public List<Action> explore(com.xceptance.neodymium.ai.core.LlmClient llmClient, String url, String intent, String sutContext, String outputPath) {
                    return super.explore(llmClient, url, intent, sutContext, outputPath);
                }
            }

            TestableGenerator generator = new TestableGenerator();
            List<Action> path = generator.explore(new com.xceptance.neodymium.ai.core.LlmClient(com.xceptance.neodymium.util.Neodymium.aiConfiguration(), new com.xceptance.neodymium.ai.core.AiStats()), "https://example.com", "intent", null, "output.yml");

            // We expect the path to contain ONLY the NAVIGATE action because the CLICK action was marked as failed
            Assertions.assertEquals(1, path.size());
            Assertions.assertEquals("NAVIGATE", path.get(0).getType());
        }
        finally
        {
            Neodymium.aiConfiguration().setProperty("neodymium.ai.generate", String.valueOf(originalValue));
        }
    }

    @Test
    public void testActionFormattingAndAssertions()
    {
        boolean originalValue = Neodymium.aiConfiguration().aiGenerateEnabled();
        try
        {
            Neodymium.aiConfiguration().setProperty("neodymium.ai.generate", "true");
            
            class TestableGenerator extends AiPromptGenerator {
                public int callCount = 0;
                
                @Override
                protected void openBrowser(String url) {}
                
                @Override
                protected String captureDom(com.xceptance.neodymium.ai.core.PageAnalyzer analyzer) { return "<html><body><input id='email'/><button id='login'>Login</button></body></html>"; }
                
                @Override
                protected void executeAction(com.xceptance.neodymium.ai.action.ActionExecutor executor, Action action) {}

                @Override
                protected String executeExplorationLlmCall(com.xceptance.neodymium.ai.core.LlmClient llmClient, String prompt) {
                    callCount++;
                    if (callCount == 1) {
                        return "{\"actions\": [{\"type\": \"" + com.xceptance.neodymium.ai.action.plugins.AssertAction.ACTION_NAME + "\", \"target\": \"#email\", \"description\": \"Validate there is an input field for the email address\"}]}";
                    } else if (callCount == 2) {
                        return "{\"actions\": [{\"type\": \"TYPE\", \"target\": \"#email\", \"value\": \"John Doe\", \"dataBindings\": {\"firstName\": \"John\", \"lastName\": \"Doe\"}, \"description\": \"Enter '${firstName} ${lastName}' into the user field\"}]}";
                    } else if (callCount == 3) {
                        return "{\"actions\": [{\"type\": \"CLICK\", \"target\": \"#login\", \"description\": \"Click the login button\"}]}";
                    } else {
                        return "{\"previousActionSuccess\": true, \"overallIntentAchieved\": true}";
                    }
                }
                
                @Override
                public List<Action> explore(com.xceptance.neodymium.ai.core.LlmClient llmClient, String url, String intent, String sutContext, String outputPath) {
                    return super.explore(llmClient, url, intent, sutContext, outputPath);
                }
            }

            TestableGenerator generator = new TestableGenerator();
            List<Action> path = generator.explore(new com.xceptance.neodymium.ai.core.LlmClient(com.xceptance.neodymium.util.Neodymium.aiConfiguration(), new com.xceptance.neodymium.ai.core.AiStats()), "https://example.com", "intent", null, "output.yml");

            Assertions.assertEquals(3, path.size());
            Assertions.assertEquals(com.xceptance.neodymium.ai.action.plugins.AssertAction.ACTION_NAME, path.get(0).getType());
            Assertions.assertEquals("Validate there is an input field for the email address", path.get(0).getDescription());
            
            Assertions.assertEquals("TYPE", path.get(1).getType());
            Assertions.assertEquals("Enter '${firstName} ${lastName}' into the user field", path.get(1).getDescription());
            Assertions.assertEquals(2, path.get(1).getDataBindings().size());
            Assertions.assertEquals("John", path.get(1).getDataBindings().get("firstName"));
            Assertions.assertEquals("Doe", path.get(1).getDataBindings().get("lastName"));
            
            Assertions.assertEquals("CLICK", path.get(2).getType());
            Assertions.assertEquals("Click the login button", path.get(2).getDescription());
        }
        finally
        {
            Neodymium.aiConfiguration().setProperty("neodymium.ai.generate", String.valueOf(originalValue));
        }
    }

    @Test
    public void testGeneratorSkipsDuplicateAsserts()
    {
        boolean originalValue = Neodymium.aiConfiguration().aiGenerateEnabled();
        try
        {
            Neodymium.aiConfiguration().setProperty("neodymium.ai.generate", "true");
            
            class TestableGenerator extends AiPromptGenerator {
                public int callCount = 0;
                
                @Override
                protected void openBrowser(String url) {}
                
                @Override
                protected String captureDom(com.xceptance.neodymium.ai.core.PageAnalyzer analyzer) { return "<html><body><div id='msg'>Hello</div></body></html>"; }
                
                @Override
                protected void executeAction(com.xceptance.neodymium.ai.action.ActionExecutor executor, Action action) {}

                @Override
                protected String executeExplorationLlmCall(com.xceptance.neodymium.ai.core.LlmClient llmClient, String prompt) {
                    callCount++;
                    if (callCount == 1) {
                        return "{\"actions\": [{\"type\": \"" + com.xceptance.neodymium.ai.action.plugins.AssertAction.ACTION_NAME + "\", \"target\": \"#msg\", \"description\": \"Validate msg\"}]}";
                    } else if (callCount == 2) {
                        // AI returns the EXACT SAME assert, should be skipped
                        return "{\"actions\": [{\"type\": \"" + com.xceptance.neodymium.ai.action.plugins.AssertAction.ACTION_NAME + "\", \"target\": \"#msg\", \"description\": \"Validate msg\"}]}";
                    } else if (callCount == 3) {
                        return "{\"actions\": [{\"type\": \"CLICK\", \"target\": \"#btn\", \"description\": \"Click button\"}]}";
                    } else {
                        return "{\"previousActionSuccess\": true, \"overallIntentAchieved\": true}";
                    }
                }
                
                @Override
                public List<Action> explore(com.xceptance.neodymium.ai.core.LlmClient llmClient, String url, String intent, String sutContext, String outputPath) {
                    return super.explore(llmClient, url, intent, sutContext, outputPath);
                }
            }

            TestableGenerator generator = new TestableGenerator();
            List<Action> path = generator.explore(new com.xceptance.neodymium.ai.core.LlmClient(com.xceptance.neodymium.util.Neodymium.aiConfiguration(), new com.xceptance.neodymium.ai.core.AiStats()), "https://example.com", "intent", null, "output.yml");

            // We expect the path to contain ONLY the first ASSERT and the CLICK action because the duplicate ASSERT was skipped
            Assertions.assertEquals(2, path.size());
            Assertions.assertEquals(com.xceptance.neodymium.ai.action.plugins.AssertAction.ACTION_NAME, path.get(0).getType());
            Assertions.assertEquals("CLICK", path.get(1).getType());
        }
        finally
        {
            Neodymium.aiConfiguration().setProperty("neodymium.ai.generate", String.valueOf(originalValue));
        }
    }
}

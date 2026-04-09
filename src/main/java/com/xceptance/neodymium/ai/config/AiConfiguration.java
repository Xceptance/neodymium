package com.xceptance.neodymium.ai.config;

import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.Mutable;

@LoadPolicy(LoadType.MERGE)
@Sources(
{
  "system:properties",
  "${neodymium.temporaryConfigFile}",
  "file:config/dev-neodymium.properties",
  "system:env",
  "file:config/credentials.properties",
  "file:config/ai.properties",
  "file:config/neodymium.properties"
})
public interface AiConfiguration extends Mutable
{
    @Key("neodymium.ai.apiKey")
    public String aiApiKey();

    @Key("neodymium.ai.model")
    @DefaultValue("gemini-2.5-flash")
    public String aiModel();

    @Key("neodymium.ai.timeoutSeconds")
    @DefaultValue("180")
    public int geminiTimeoutSeconds();

    @Key("neodymium.ai.agent.maxRetries")
    @DefaultValue("3")
    public int agentMaxRetries();

    @Key("neodymium.ai.agent.screenshotBeforeAction")
    @DefaultValue("true")
    public boolean agentScreenshotBeforeAction();

    @Key("neodymium.ai.report.attachTokenUsage")
    @DefaultValue("true")
    public boolean attachTokenUsageToReport();

    @Key("neodymium.ai.report.attachFullDiscussion")
    @DefaultValue("true")
    public boolean attachFullDiscussionToReport();

    @Key("neodymium.ai.playbook.healing.enabled")
    @DefaultValue("true")
    public boolean playbookHealingEnabled();
}

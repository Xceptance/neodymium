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

    @Key("neodymium.ai.playbook.record")
    @DefaultValue("true")
    public boolean playbookRecordEnabled();



    @Key("neodymium.ai.temperature")
    @DefaultValue("0.1")
    public double aiTemperature();

    @Key("neodymium.ai.generate")
    @DefaultValue("false")
    public boolean aiGenerateEnabled();

    @Key("neodymium.ai.generate.temperature")
    @DefaultValue("1.0")
    public double aiGenerateTemperature();

    @Key("neodymium.ai.generate.maxSteps")
    @DefaultValue("100")
    public int aiGenerateMaxSteps();

    @Key("neodymium.ai.generate.maxFailures")
    @DefaultValue("5")
    public int aiGenerateMaxFailures();

    /**
     * This is work in progress, the generated assertions are not stable right now.
     */
    @Key("neodymium.ai.generate.validations")
    @DefaultValue("false")
    public boolean aiGenerateValidations();

    @Key("neodymium.ai.generate.v2")
    @DefaultValue("false")
    public boolean aiGenerateV2();

    @Key("neodymium.ai.generate.v2.diagnosticLogs")
    @DefaultValue("false")
    public boolean aiGenerateV2DiagnosticLogs();

    @Key("neodymium.ai.interactive")
    @DefaultValue("false")
    public boolean aiInteractive();

    @Key("neodymium.ai.plugins")
    @DefaultValue("")
    public java.util.List<String> aiPlugins();
}

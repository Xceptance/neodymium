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

    @Key("neodymium.ai.agent.pattern.url")
    @DefaultValue("(?i)^(?:open|go\\s+to|navigate\\s+to|visit|[Öö]ffne|browse\\s+to)\\s+(https?:\\/\\/\\S+?)(?=[.,!?;]?(?:\\s|$))(\\.)*$")
    public String agentPatternUrl();

    @Key("neodymium.ai.agent.pattern.urlWithBasicAuth")
    @DefaultValue("(?i)^(?:open|go\\s+to|navigate\\s+to|visit|[Öö]ffne|browse\\s+to)\\s+(https?:\\/\\/\\S+?)(?=[.,!?;]?(?:\\s|$))(?:\\s+.*?\\b(?:with|using)?\\s*basic\\s+auth\\s+(?:username|user)\\s+['\"](?<username>.*?)['\"]\\s+(?:and\\s+)?(?:password|pass)\\s+['\"](?<password>.*?)['\"])?.*$")
    public String agentPatternUrlWithBasicAuth();
    
    @Key("neodymium.ai.agent.pattern.javaMethod")
    @DefaultValue("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)")
    public String agentPatternJavaMethod();

    @Key("neodymium.ai.agent.pattern.validation")
    @DefaultValue("(?i)^(?:verify|check|validate|ensure|assert|prüfe|verifiziere|überprüfe|bestätige|checke)\\b.*")
    public String agentPatternValidation();

    @Key("neodymium.ai.agent.pattern.back")
    @DefaultValue("(?i)^(?:go\\s+)?back$|^navigate\\s+back$")
    public String agentPatternBack();

    @Key("neodymium.ai.agent.pattern.forward")
    @DefaultValue("(?i)^(?:go\\s+)?forward$|^navigate\\s+forward$")
    public String agentPatternForward();

    @Key("neodymium.ai.agent.pattern.refresh")
    @DefaultValue("(?i)^(?:refresh|reload)(?:\\s+page)?$")
    public String agentPatternRefresh();

    @Key("neodymium.ai.agent.pattern.clearCookies")
    @DefaultValue("(?i)^(?:clear\\s+cookies|reset\\s+session|clear\\s+all\\s+cookies)$")
    public String agentPatternClearCookies();
    
    @Key("neodymium.ai.agent.pattern.ifStatmentPattern")
    @DefaultValue("(?i)if\\s+(.*?)(?:,\\s+then\\s+|\\s+then\\s+|,\\s+)(.*)$")
    public String agentPatternIfStatement();
}

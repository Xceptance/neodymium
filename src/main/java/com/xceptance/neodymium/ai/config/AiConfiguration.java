/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.ai.config;

import java.util.List;
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
    @DefaultValue("false")
    public boolean agentScreenshotBeforeAction();
    @Key("neodymium.ai.pesap.enabled")
    @DefaultValue("true")
    public boolean pesapEnabled();
    @Key("neodymium.ai.pesap.classify.enabled")
    @DefaultValue("true")
    public boolean pesapClassifyEnabled();


    @Key("neodymium.ai.pesap.linter.enabled")
    @DefaultValue("false")
    public boolean pesapLinterEnabled();

    /**
     * Path to a custom rules file to extend the PESAP semantic linter.
     * The value is resolved first as a classpath resource, then as a filesystem path.
     * Must be a valid file if configured.
     *
     * @return the path to the custom rules file, or null if not configured
     */
    @Key("neodymium.ai.pesap.custom.file")
    public String pesapCustomFile();

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
    @DefaultValue("0.0")
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

    @Key("neodymium.ai.interactive.autoSkip")
    @DefaultValue("false")
    public boolean aiInteractiveAutoSkip();

    @Key("neodymium.ai.plugins")
    @DefaultValue("")
    public List<String> aiPlugins();

    /**
     * Comma-separated list of fully qualified class names that the {@code JAVA_METHOD} action
     * plugin should scan for static utility methods when a method is not found on the test
     * instance itself. The built-in {@code AiAssertions} class is included by default.
     * <p>
     * To add project-specific utility classes, append them to this property:
     * <pre>
     * neodymium.ai.agent.javaMethod.utilityClasses=com.xceptance.neodymium.ai.util.AiAssertions,com.myproject.MyAssertions
     * </pre>
     */
    @Key("neodymium.ai.agent.javaMethod.utilityClasses")
    @DefaultValue("com.xceptance.neodymium.ai.util.AiAssertions")
    public List<String> aiJavaMethodUtilityClasses();

    @Key("neodymium.ai.aura.manager.shutdownDelay")
    @DefaultValue("5")
    public int auraManagerShutdownDelay();

    @Key("neodymium.ai.agent.methods.classes")
    @DefaultValue("com.xceptance.neodymium.ai.util.AiAssertions")
    public List<String> aiJavaMethodClasses();

    @Key("neodymium.ai.agent.methods.packages")
    @DefaultValue("")
    public List<String> aiJavaMethodPackages();

    /**
     * API key for assertion LLM calls. If not configured, falls back to the standard API key.
     *
     * @return the assertion API key, or null if not configured
     */
    @Key("neodymium.ai.assertion.apiKey")
    public String aiAssertionApiKey();

    /**
     * Model name for assertion LLM calls. If not configured, falls back to the standard model name.
     *
     * @return the assertion model name, or null if not configured
     */
    @Key("neodymium.ai.assertion.model")
    public String aiAssertionModel();

    /**
     * Timeout in seconds for assertion LLM calls. If not configured, falls back to the standard timeout.
     *
     * @return the assertion timeout in seconds, or null if not configured
     */
    @Key("neodymium.ai.assertion.timeoutSeconds")
    public Integer aiAssertionTimeoutSeconds();

    /**
     * Temperature for assertion LLM calls. If not configured, falls back to 0.0.
     *
     * @return the assertion temperature, or null if not configured
     */
    @Key("neodymium.ai.assertion.temperature")
    public Double aiAssertionTemperature();
}

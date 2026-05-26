/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
  *
 * // AI-generated: Gemini 2.0 Flash
*/
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
    @DefaultValue("false")
    public boolean agentScreenshotBeforeAction();

    @Key("neodymium.ai.pesap.enabled")
    @DefaultValue("true")
    public boolean pesapEnabled();

    @Key("neodymium.ai.pesap.classify.enabled")
    @DefaultValue("true")
    public boolean pesapClassifyEnabled();

    @Key("neodymium.ai.pesap.linter.enabled")
    @DefaultValue("true")
    public boolean pesapLinterEnabled();

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
    public java.util.List<String> aiJavaMethodUtilityClasses();
}

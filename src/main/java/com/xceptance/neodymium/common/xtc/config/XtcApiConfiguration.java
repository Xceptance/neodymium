package com.xceptance.neodymium.common.xtc.config;

import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.Mutable;

@LoadPolicy(LoadType.MERGE)
@Sources(
    {
        "system:properties",
        // "${xtc-api.temporaryConfigFile}",
        // "${neodymium.temporaryConfigFile}",
        "file:config/dev-xtc-api.properties",
        "file:config/dev-neodymium.properties",
        "system:env",
        "file:config/credentials.properties",
        "file:config/xtc-api.properties",
        "file:config/neodymium.properties"
    })
public interface XtcApiConfiguration extends Mutable
{
    // TODO URLs as configuration properties? Makes it easier to overwrite them if they change without a new Neo version

    // TODO add result and report directories as configuration properties?

    @Key("xtc.api.enable")
    @DefaultValue("false")
    public boolean xtcApiIsEnabled();

    @Key("xtc.api.organization")
    public String xtcApiOrganization();

    @Key("xtc.api.project")
    public String xtcApiProject();

    @Key("xtc.api.key")
    public String xtcApiKey();

    @Key("xtc.api.secret")
    public String xtcApiSecret();

    @Key("xtc.api.profile")
    @DefaultValue("default")
    public String xtcApiProfile();

    @Key("xtc.api.pipelineLink")
    public String xtcApiPipelineLink();

    @Key("xtc.api.buildNumber")
    public String xtcApiBuildNumber();

    @Key("xtc.api.testInstance")
    @DefaultValue("default")
    public String xtcApiTestInstance();

    @Key("xtc.api.description")
    public String xtcApiDescription();

    @Key("xtc.api.expectedDuration")
    @DefaultValue("0")
    public int xtcApiExpectedDuration();

    @Key("xtc.api.numberOfRetries")
    @DefaultValue("3")
    public int xtcApiNumberOfRetries();

    @Key("xtc.api.retryDelay")
    @DefaultValue("1000")
    public int xtcApiRetryDelay();

    @Key("xtc.api.timeout")
    @DefaultValue("30000")
    public int xtcApiTimeout();

    @Key("xtc.api.scope")
    @DefaultValue("TESTEXECUTION_CREATE TESTEXECUTION_FINISH TESTEXECUTION_LIST TESTEXECUTION_REPORT_UPLOAD TESTEXECUTION_UPDATE")
    public String xtcApiScope();
}

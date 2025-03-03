package com.xceptance.neodymium.util;

import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.Mutable;

@LoadPolicy(LoadType.MERGE)
@Sources(
{
  "system:env",
  "system:properties",
  "file:config/dev-testconfiguration.properties"
})
public interface TestConfiguration extends Mutable
{
    @Key("BROWSERSTACK_USERNAME")
    public String browserstackUsername();

    @Key("BROWSERSTACK_ACCESS_KEY")
    public String browserstackAccessKey();

    @Key("PROXY_HOST")
    public String proxyHost();

    @Key("PROXY_PORT")
    public String proxyPort();

    @Key("PROXY_USERNAME")
    public String proxyUsername();

    @Key("PROXY_PASSWORD")
    public String proxyPassword();
}

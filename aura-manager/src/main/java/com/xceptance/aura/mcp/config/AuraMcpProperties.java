package com.xceptance.aura.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Neodymium Aura MCP Server.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Component
@ConfigurationProperties(prefix = "neodymium.aura.mcp")
public class AuraMcpProperties
{
    private boolean enabled = true;

    private Auth auth = new Auth();

    private int heartbeatIntervalSeconds = 45;

    private int sessionTimeoutSeconds = 120;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(final boolean enabled)
    {
        this.enabled = enabled;
    }

    public Auth getAuth()
    {
        return auth;
    }

    public void setAuth(final Auth auth)
    {
        this.auth = auth;
    }

    public int getHeartbeatIntervalSeconds()
    {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(final int heartbeatIntervalSeconds)
    {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    public int getSessionTimeoutSeconds()
    {
        return sessionTimeoutSeconds;
    }

    public void setSessionTimeoutSeconds(final int sessionTimeoutSeconds)
    {
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
    }

    public static class Auth
    {
        private String token;

        public String getToken()
        {
            return token;
        }

        public void setToken(final String token)
        {
            this.token = token;
        }
    }
}

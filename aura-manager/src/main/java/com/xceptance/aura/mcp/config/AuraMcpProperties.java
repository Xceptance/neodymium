/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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

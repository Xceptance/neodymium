package com.xceptance.aura.mcp.security;

import com.xceptance.aura.mcp.config.AuraMcpProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for enforcing Bearer token authentication and security policies on the MCP endpoint.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Component
public class McpSecurityInterceptor implements HandlerInterceptor
{
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuraMcpProperties properties;

    public McpSecurityInterceptor(final AuraMcpProperties properties)
    {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
        throws Exception
    {
        if (!properties.isEnabled())
        {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "MCP Server is disabled");
            return false;
        }

        final String configuredToken = properties.getAuth() != null ? properties.getAuth().getToken() : null;

        // If no token is configured, allow local access
        if (configuredToken == null || configuredToken.isBlank())
        {
            return true;
        }

        final String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX))
        {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Bearer realm=\"Aura MCP\"");
            response.getWriter().write("{\"error\": \"Missing or invalid Authorization header\"}");
            return false;
        }

        final String presentedToken = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (!configuredToken.equals(presentedToken))
        {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Invalid MCP authentication token\"}");
            return false;
        }

        return true;
    }
}

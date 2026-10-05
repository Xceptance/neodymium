package com.xceptance.aura.mcp.config;

import com.xceptance.aura.mcp.security.McpSecurityInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc configuration for registering MCP security interceptors.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Configuration
public class AuraMcpWebMvcConfiguration implements WebMvcConfigurer
{
    private final McpSecurityInterceptor securityInterceptor;

    public AuraMcpWebMvcConfiguration(final McpSecurityInterceptor securityInterceptor)
    {
        this.securityInterceptor = securityInterceptor;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry)
    {
        registry.addInterceptor(securityInterceptor)
            .addPathPatterns("/mcp", "/mcp/**");
    }
}

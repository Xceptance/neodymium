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
package com.xceptance.aura.report.config;

import jakarta.servlet.Servlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit Servlet configuration to register the H2 Web Console directly with Tomcat 11 (Jakarta Servlet API).
 * Uses reflection to instantiate H2 JakartaWebServlet because the h2 dependency is scoped as runtime in pom.xml.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Configuration
public class H2ConsoleConfig
{
    @Bean
    public ServletRegistrationBean<Servlet> h2ServletRegistration()
    {
        try
        {
            final Class<?> servletClass = Class.forName("org.h2.server.web.JakartaWebServlet");
            final Servlet servlet = (Servlet) servletClass.getDeclaredConstructor().newInstance();
            final ServletRegistrationBean<Servlet> registrationBean = new ServletRegistrationBean<>(servlet);
            registrationBean.addUrlMappings("/h2-console/*", "/h2-console");
            return registrationBean;
        }
        catch (final Exception e)
        {
            throw new IllegalStateException("Failed to instantiate H2 JakartaWebServlet", e);
        }
    }
}

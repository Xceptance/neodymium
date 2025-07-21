package com.xceptance.neodymium.common.xtc;

import com.xceptance.neodymium.common.xtc.config.XtcApiConfiguration;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XtcApiContext
{
    public static XtcApiConfiguration configuration = ConfigFactory.create(XtcApiConfiguration.class);

    private static final Logger LOGGER = LoggerFactory.getLogger(XtcApiContext.class);

    public static boolean isXtcApiEnabled()
    {
        return configuration.xtcApiIsEnabled();
    }

    /**
     * Ensures that the required configuration for the XTC API is set. If any required configuration is missing, an {@link IllegalStateException} is thrown.
     */
    public static void ensureRequiredConfiguration()
    {
        if (!isXtcApiEnabled())
        {
            return;
        }

        if (StringUtils.isBlank(configuration.xtcApiOrganization()))
        {
            LOGGER.error("XTC API organization is not configured.");
            throw new IllegalStateException("XTC API organization is not configured.");
        }

        if (StringUtils.isBlank(configuration.xtcApiProject()))
        {
            LOGGER.error("XTC API project is not configured.");
            throw new IllegalStateException("XTC API project is not configured.");
        }

        if (StringUtils.isBlank(configuration.xtcApiSecret()))
        {
            LOGGER.error("XTC API secret is not configured.");
            throw new IllegalStateException("XTC API secret is not configured.");
        }

        if (StringUtils.isBlank(configuration.xtcApiKey()))
        {
            LOGGER.error("XTC API key is not configured.");
            throw new IllegalStateException("XTC API key is not configured.");
        }
    }
}

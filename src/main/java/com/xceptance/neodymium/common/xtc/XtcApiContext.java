package com.xceptance.neodymium.common.xtc;

import com.xceptance.neodymium.common.xtc.config.XtcApiConfiguration;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.xceptance.neodymium.util.Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME;

public class XtcApiContext
{
    public static XtcApiConfiguration configuration;

    public final static String TEMPORARY_XTC_API_CONFIG_FILE_PROPERTY_NAME = "xtc-api.temporaryConfigFile";

    private static final Logger LOGGER = LoggerFactory.getLogger(XtcApiContext.class);

    static
    {
        initialize();
    }

    /**
     * Initializes the XTC API context by loading the configuration. This method is called automatically when the class is loaded.
     */
    private static void initialize()
    {
        // the property needs to be a valid URI in order to satisfy the Owner framework
        if (null == ConfigFactory.getProperty(TEMPORARY_XTC_API_CONFIG_FILE_PROPERTY_NAME))
        {
            ConfigFactory.setProperty(TEMPORARY_XTC_API_CONFIG_FILE_PROPERTY_NAME, "file:this/path/should/never/exist/noOneShouldCreateMe.properties");
        }
        if (null == ConfigFactory.getProperty(TEMPORARY_CONFIG_FILE_PROPERTY_NAME))
        {
            ConfigFactory.setProperty(TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:this/path/should/never/exist/noOneShouldCreateMe.properties");
        }
        configuration = ConfigFactory.create(XtcApiConfiguration.class, System.getProperties(), System.getenv());
    }

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

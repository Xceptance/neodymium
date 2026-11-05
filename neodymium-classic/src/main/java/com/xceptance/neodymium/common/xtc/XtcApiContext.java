package com.xceptance.neodymium.common.xtc;

import com.xceptance.neodymium.common.xtc.config.XtcApiConfiguration;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.xceptance.neodymium.util.Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME;

public class XtcApiContext
{
    public static XtcApiConfiguration configuration;

    public final static String TEMPORARY_XTC_API_CONFIG_FILE_PROPERTY_NAME = "xtc-api.temporaryConfigFile";

    private static final Logger LOGGER = LoggerFactory.getLogger(XtcApiContext.class);

    static
    {
        initialize();
        ensureRequiredConfiguration();
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

    /**
     * Ensures that the required configuration for the XTC API is set. If any required configuration is missing, an {@link IllegalStateException} is thrown.
     */
    public static void ensureRequiredConfiguration()
    {
        if (!isXtcApiEnabled())
        {
            return;
        }

        List<String> validationErrors = new ArrayList<>();

        if (StringUtils.isBlank(configuration.xtcApiOrganization()))
        {
            String error = "XTC API organization is not configured.";
            LOGGER.error(error);
            validationErrors.add(error);
        }

        if (StringUtils.isBlank(configuration.xtcApiProject()))
        {
            String error = "XTC API project is not configured.";
            LOGGER.error(error);
            validationErrors.add(error);
        }

        if (StringUtils.isBlank(configuration.xtcApiSecret()))
        {
            String error = "XTC API secret is not configured.";
            LOGGER.error(error);
            validationErrors.add(error);
        }

        if (StringUtils.isBlank(configuration.xtcApiKey()))
        {
            String error = "XTC API key is not configured.";
            LOGGER.error(error);
            validationErrors.add(error);
        }

        if (StringUtils.isBlank(configuration.xtcApiScope()))
        {
            String error = "XTC API scope is not configured.";
            LOGGER.error(error);
            validationErrors.add(error);
        }

        if (StringUtils.isBlank(configuration.xtcApiName()))
        {
            String error = "XTC API name is not configured.";
            LOGGER.error(error);
            validationErrors.add(error);
        }

        if (StringUtils.isBlank(configuration.xtcApiSurefireResultDirectory()))
        {
            String error = "XTC API surefire result directory is not configured.";
            LOGGER.error(error);
            validationErrors.add(error);
        }

        if (StringUtils.isBlank(configuration.xtcApiAllureResultDirectory()))
        {
            String error = "XTC API allure result directory is not configured.";
            LOGGER.error(error);
            validationErrors.add(error);
        }

        if (StringUtils.isBlank(configuration.xtcApiAllureReportDirectory()))
        {
            String error = "XTC API allure report directory is not configured.";
            LOGGER.error(error);
            validationErrors.add(error);
        }

        // Only throw exception if toggle is enabled and there are validation errors
        if (!validationErrors.isEmpty() && configuration.xtcApiValidateConfigurationWithException())
        {
            throw new IllegalStateException("Configuration validation failed: " + String.join(", ", validationErrors));
        }
    }

    public static boolean isXtcApiEnabled()
    {
        return configuration.xtcApiIsEnabled();
    }
}

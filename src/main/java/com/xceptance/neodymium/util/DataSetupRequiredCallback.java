package com.xceptance.neodymium.util;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DataSetupRequiredCallback implements BeforeEachCallback
{
    @Override
    public void beforeEach(ExtensionContext context) throws Exception
    {
        if (context.getTags().stream().anyMatch(tag -> tag.equals("RequiresDataSetup")))
        {
            Neodymium.configuration().setProperty("RequiresDataSetup", "true");
        }
        if (context.getTags().stream().anyMatch(tag -> tag.equals("KnownIssue")))
        {
            Neodymium.configuration().setProperty("KnownIssue", "true");
        }
    }

}

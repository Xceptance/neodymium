package com.xceptance.neodymium.common.xtc;

import com.xceptance.neodymium.common.xtc.config.XtcApiConfiguration;
import org.aeonbits.owner.ConfigFactory;

public class XtcApiContext
{
    public static XtcApiConfiguration configuration = ConfigFactory.create(XtcApiConfiguration.class);
}

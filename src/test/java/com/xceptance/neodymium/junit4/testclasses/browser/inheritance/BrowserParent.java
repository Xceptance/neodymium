package com.xceptance.neodymium.junit4.testclasses.browser.inheritance;

import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000")
@RunWith(NeodymiumRunner.class)
public abstract class BrowserParent
{

}

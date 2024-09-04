package com.xceptance.neodymium.junit4.testclasses.browser.inheritance;

import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.RandomBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000")
@Browser("FF_1024x768")
@Browser("FF_1500x1000")
@RandomBrowsers(2)
@RunWith(NeodymiumRunner.class)
public abstract class RandomBrowsersParent
{

}

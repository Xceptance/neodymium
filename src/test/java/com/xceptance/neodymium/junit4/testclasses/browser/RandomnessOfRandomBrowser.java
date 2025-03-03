package com.xceptance.neodymium.junit4.testclasses.browser;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.RandomBrowsers;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@Browser("browser1")
@Browser("browser2")
@Browser("browser3")
@Browser("browser4")
@Browser("browser5")
@Browser("browser6")
@Browser("browser7")
@Browser("browser8")
@Browser("browser9")
@Browser("browser10")
@Browser("browser11")
@Browser("browser12")
@Browser("browser13")
@Browser("browser14")
@Browser("browser15")
@Browser("browser16")
@Browser("browser17")
@Browser("browser18")
@Browser("browser19")
@Browser("browser20")
@Browser("browser21")
@Browser("browser22")
@Browser("browser23")
@Browser("browser24")
@Browser("browser25")
@Browser("browser26")
@Browser("browser27")
@Browser("browser28")
@Browser("browser29")
@Browser("browser30")
@Browser("browser31")
@Browser("browser32")
@Browser("browser33")
@Browser("browser34")
@Browser("browser35")
@Browser("browser36")
@Browser("browser37")
@Browser("browser38")
@Browser("browser39")
@Browser("browser40")
@Browser("browser41")
@Browser("browser42")
@Browser("browser43")
@Browser("browser44")
@Browser("browser45")
@Browser("browser46")
@Browser("browser47")
@Browser("browser48")
@Browser("browser49")
@Browser("browser50")
@Browser("browser51")
@Browser("browser52")
@Browser("browser53")
@Browser("browser54")
@Browser("browser55")
@Browser("browser56")
@Browser("browser57")
@Browser("browser58")
@Browser("browser59")
@Browser("browser60")
@Browser("browser61")
@Browser("browser62")
@Browser("browser63")
@Browser("browser64")
@Browser("browser65")
@Browser("browser66")
@Browser("browser67")
@Browser("browser68")
@Browser("browser69")
@Browser("browser70")
@Browser("browser71")
@Browser("browser72")
@Browser("browser73")
@Browser("browser74")
@Browser("browser75")
@Browser("browser76")
@Browser("browser77")
@Browser("browser78")
@Browser("browser79")
@Browser("browser80")
@Browser("browser81")
@Browser("browser82")
@Browser("browser83")
@Browser("browser84")
@Browser("browser85")
@Browser("browser86")
@Browser("browser87")
@Browser("browser88")
@Browser("browser89")
@Browser("browser90")
@Browser("browser91")
@Browser("browser92")
@Browser("browser93")
@Browser("browser94")
@Browser("browser95")
@Browser("browser96")
@Browser("browser97")
@Browser("browser98")
@Browser("browser99")
@Browser("browser100")
@RandomBrowsers(10)
@RunWith(NeodymiumRunner.class)
public class RandomnessOfRandomBrowser
{
    private static ArrayList<String> browsers = new ArrayList<>();

    @Test
    public void test1()
    {
        browsers.add(Neodymium.getBrowserProfileName());
    }

    @Test
    @SuppressBrowsers
    public void test2()
    {
        Assert.assertEquals(10, browsers.size());
        boolean changedOrder = false;
        for (int i = 1; i < browsers.size(); i++)
        {
            if (!browsers.get(i).equals("browser" + i))
            {
                changedOrder = true;
                break;
            }
        }
        Assert.assertTrue(changedOrder);
    }
}

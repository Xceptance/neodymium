package com.xceptance.neodymium.junit4.order;

import com.xceptance.neodymium.junit4.statement.browser.BrowserStatement;
import com.xceptance.neodymium.junit4.statement.parameter.ParameterStatement;
import com.xceptance.neodymium.junit4.statement.repeat.RepeatStatement;
import com.xceptance.neodymium.junit4.statement.testdata.TestdataStatement;

public class DefaultStatementRunOrder extends StatementRunOrder
{
    public DefaultStatementRunOrder()
    {
        runOrder.add(RepeatStatement.class);
        runOrder.add(BrowserStatement.class);
        runOrder.add(ParameterStatement.class);
        runOrder.add(TestdataStatement.class);
    }
}

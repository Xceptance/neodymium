package org.neodymium.junit4.order;

import org.neodymium.junit4.statement.browser.BrowserStatement;
import org.neodymium.junit4.statement.parameter.ParameterStatement;
import org.neodymium.junit4.statement.repeat.RepeatStatement;
import org.neodymium.junit4.statement.testdata.TestdataStatement;

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

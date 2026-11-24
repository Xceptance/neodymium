/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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

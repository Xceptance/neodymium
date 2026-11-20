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
package org.neodymium.junit4.statement.parameter;

import java.util.List;

import org.junit.runners.model.FrameworkField;

public class ParameterStatementData
{
    private int parameterIndex;

    private Object[] parameter;

    private List<FrameworkField> parameterFrameworkFields;

    public ParameterStatementData(int parameterIndex, Object[] parameter, List<FrameworkField> parameterFrameworkFields)
    {
        this.parameterIndex = parameterIndex;
        this.parameter = parameter;
        this.parameterFrameworkFields = parameterFrameworkFields;
    }

    public int getParameterIndex()
    {
        return parameterIndex;
    }

    public Object[] getParameter()
    {
        return parameter;
    }

    public List<FrameworkField> getParameterFrameworkFields()
    {
        return parameterFrameworkFields;
    }

    public void setParameterFrameworkFields(List<FrameworkField> parameterFrameworkFields)
    {
        this.parameterFrameworkFields = parameterFrameworkFields;
    }
}

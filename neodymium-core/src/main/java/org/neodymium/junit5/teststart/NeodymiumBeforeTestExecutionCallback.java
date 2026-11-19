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
package org.neodymium.junit5.teststart;

import org.neodymium.util.Neodymium;
import org.neodymium.util.NeodymiumRandom;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.neodymium.util.NeodymiumRandom.reinitializeRandomSeed;

public class NeodymiumBeforeTestExecutionCallback implements BeforeTestExecutionCallback
{
    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception
    {
        // reset the random seed so every test starts with the same values for better reproducibility
        reinitializeRandomSeed(NeodymiumRandom.SeedState.INITIALIZED);
        
        // exact unified test name calculation
        Neodymium.setTestName(context.getRequiredTestClass().getCanonicalName() + " :: " + context.getDisplayName());
    }
}

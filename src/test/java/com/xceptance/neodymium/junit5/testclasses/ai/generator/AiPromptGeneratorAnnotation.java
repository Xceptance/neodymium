/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.junit5.testclasses.ai.generator;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.junit5.NeodymiumTestGenerator;
import com.xceptance.neodymium.util.Neodymium;

public class AiPromptGeneratorAnnotation {
    @NeodymiumTestGenerator
    public void testNeodymiumTestGeneratorAnnotation() {
        // This confirms that the test runner correctly executes this method
        // with the proper Neodymium Context initialized by the NeodymiumRunner
        Assertions.assertNotNull(Neodymium.configuration(), "Neodymium configuration should be injected and not null");
    }
}

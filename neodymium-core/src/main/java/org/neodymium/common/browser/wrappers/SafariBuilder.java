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
package org.neodymium.common.browser.wrappers;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.safari.SafariDriverService.Builder;

public class SafariBuilder extends Builder {
	private List<String> arguments;

	public SafariBuilder(List<String> args) {
		this.arguments = args;
		if (this.arguments != null && !this.arguments.isEmpty()) {
			List<String> portArgs = this.arguments.stream().filter(arg -> arg.contains("--port="))
					.collect(Collectors.toList());
			if (!portArgs.isEmpty()) {
				usingPort(Integer.parseInt(portArgs.get(portArgs.size() - 1).replace("--port=", "")));
				this.arguments.removeAll(portArgs);
			}
			List<String> diagnose = this.arguments.stream().filter(arg -> arg.contains("--diagnose"))
					.collect(Collectors.toList());
			if (!diagnose.isEmpty()) {
				withLogging(true);
				this.arguments.removeAll(diagnose);
			}
		}
	}

	@Override
	protected List<String> createArgs() {
		List<String> args = super.createArgs();
		args.addAll(arguments);
		return args;
	}
}
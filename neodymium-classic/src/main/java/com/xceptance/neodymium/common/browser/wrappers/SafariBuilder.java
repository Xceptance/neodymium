package com.xceptance.neodymium.common.browser.wrappers;

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
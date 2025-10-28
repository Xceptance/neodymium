package com.xceptance.neodymium.common.browser.wrappers;

import static java.util.Collections.unmodifiableList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.GeckoDriverService.Builder;

public class GeckoBuilder extends Builder
{
    private final List<String> arguments;

    public GeckoBuilder(final List<String> args)
    {
        this.arguments = args != null ? new ArrayList<String>(args) : new ArrayList<String>();
        final List<String> logPaths = arguments.stream().filter(arg -> arg.contains("--log-path=")).collect(Collectors.toList());
        if (!logPaths.isEmpty())
        {
            final String logPath = logPaths.get(logPaths.size() - 1);
            withLogFile(new File(logPath.replace("--log-path=", "")));
            arguments.remove(arguments.indexOf(logPath));
        }
        final List<String> portArgs = this.arguments.stream().filter(arg -> arg.contains("--port=")).collect(Collectors.toList());
        if (!portArgs.isEmpty())
        {
            usingPort(Integer.parseInt(portArgs.get(portArgs.size() - 1).replace("--port=", "")));
            this.arguments.removeAll(portArgs);
        }
		final List<String> wsPorts = this.arguments.stream()
				.filter(arg -> arg.contains("--websocket-port=")).collect(Collectors.toList());
		if (!wsPorts.isEmpty()) {
			int webSocketPort = Integer
					.parseInt(wsPorts.get(wsPorts.size() - 1).replace("--websocket-port=", ""));
			// Remove the first occurrence of the websocket port argument using the wsPorts
			// list
			this.arguments.remove(wsPorts.get(0));
			withWebSocketPort(webSocketPort);
		}
    }

    @Override
    protected List<String> createArgs()
    {
        final List<String> args = new ArrayList<>();
        if (arguments != null && !arguments.isEmpty())
        {
            final int indexOfLogs = arguments.indexOf("--log");
            if (indexOfLogs > -1)
            {
                withLogLevel(FirefoxDriverLogLevel.fromString(arguments.get(indexOfLogs + 1)));
                arguments.remove(indexOfLogs);
                arguments.remove(indexOfLogs);
            }

            args.addAll(super.createArgs());
            final int indexOfAllowHosts = arguments.indexOf("--allow-hosts");
            if (indexOfAllowHosts > -1)
            {
                final int indexOfOriginalAllowHosts = args.indexOf("--allow-hosts");
                final int initArgsSize = arguments.size();
                arguments.remove(indexOfAllowHosts);
                for (int i = indexOfAllowHosts; i < initArgsSize - indexOfAllowHosts && !arguments.get(indexOfAllowHosts).contains("-"); ++i)
                {
                    args.add(indexOfOriginalAllowHosts + 1, arguments.get(indexOfAllowHosts));
                    arguments.remove(indexOfAllowHosts);
                }
            }
            final int indexOfProfileRoot = arguments.indexOf("--profile-root");
            if (indexOfProfileRoot > -1)
            {
                final int indexOfOriginalProfileRoot = args.indexOf("--profile-root");
                if (indexOfOriginalProfileRoot > -1)
                {
                    args.remove(indexOfOriginalProfileRoot);
                    args.remove(indexOfOriginalProfileRoot + 1);
                }
                args.add(arguments.get(indexOfProfileRoot));
                args.add(new File(arguments.get(indexOfProfileRoot + 1)).getAbsolutePath());
                arguments.remove(indexOfProfileRoot);
                arguments.remove(indexOfProfileRoot + 1);
            }

            final int indexOfAllowOrings = arguments.indexOf("--allow-origins");
            if (indexOfAllowOrings > -1)
            {
                final int indexOfOriginalAllowOrings = args.indexOf("--allow-origins");
                final int initArgsSize = arguments.size();
                arguments.remove(indexOfAllowOrings);
                for (int i = indexOfAllowOrings; i < initArgsSize - indexOfAllowOrings && !arguments.get(indexOfAllowOrings).contains("-"); ++i)
                {
                    args.add(indexOfOriginalAllowOrings + 1, arguments.get(indexOfAllowOrings));
                    arguments.remove(indexOfAllowOrings);
                }
            }

            args.addAll(arguments);
        }
        else
        {
            args.addAll(super.createArgs());
        }
        return unmodifiableList(args);
    }
}
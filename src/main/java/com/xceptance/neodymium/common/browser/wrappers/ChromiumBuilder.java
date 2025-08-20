package com.xceptance.neodymium.common.browser.wrappers;

import com.google.common.collect.ImmutableList;
import org.openqa.selenium.chrome.ChromeDriverService.Builder;

import java.util.List;
import java.util.stream.Collectors;

public class ChromiumBuilder extends Builder
{
    private List<String> arguments;

    public ChromiumBuilder(List<String> args)
    {
        this.arguments = args;
        if (this.arguments != null && !this.arguments.isEmpty())
        {
            List<String> portArgs = this.arguments.stream().filter(arg -> arg.contains("--port=")).collect(Collectors.toList());
            if (!portArgs.isEmpty())
            {
                usingPort(Integer.parseInt(portArgs.get(portArgs.size() - 1).replace("--port=", "")));
                arguments.removeAll(portArgs);
            }
        }
    }

    @Override
    protected ImmutableList<String> createArgs()
    {
        ImmutableList.Builder<String> argsBuilder = ImmutableList.builder();
        argsBuilder.addAll(super.createArgs());
        if (arguments != null)
        {
            argsBuilder.addAll(arguments);
        }
        return argsBuilder.build();
    }
}

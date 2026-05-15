/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
  *
 * // AI-generated: Gemini 2.0 Flash
*/
package com.xceptance.neodymium.ai.action.plugins;

import java.lang.reflect.Method;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class JavaMethodAction implements AiActionPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(JavaMethodAction.class);

    @Override
    public String getActionName() { return "JAVA_METHOD"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) {
        if (instruction.toLowerCase().contains("java")) {
            String patternStr = com.xceptance.neodymium.util.Neodymium.configuration().getProperty("neodymium.ai.agent.pattern.javaMethod", "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)");
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(patternStr).matcher(instruction.strip());
            if (matcher.find()) {
                final String method = matcher.group(1);
                final String param = matcher.group(2);
                LOG.debug("▶️ [EXEC] Direct call Java Method {} with param {}", method, param);
                return List.of(new Action("JAVA_METHOD", method, List.of(param), "Call " + method + " with param " + param));
            }
        }
        return null;
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "JAVA_METHOD: Invoke a Java method via reflection. target = fully-qualified 'ClassName.methodName' (static) or 'ClassName' for an instance obtained via a no-arg constructor. value = the single String argument passed to the method."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        final String target = action.getTarget();
        if (target == null || target.isBlank()) {
            throw new ActionExecutor.ActionExecutionException("JAVA_METHOD action requires a 'target' containing only the class name");
        }

        final int lastDot = target.lastIndexOf('.');
        if (lastDot >= 0) {
            throw new ActionExecutor.ActionExecutionException("JAVA_METHOD target can not be fully qualified. Only use simple method name, got: " + target);
        }

        final String methodName = target;
        final Object[] param = action.getValues().toArray(); // may be null

        LOG.debug("JAVA_METHOD: method='{}', param='{}'", methodName, param);

        try {
            Method method = null;
            boolean isStatic = false;
            boolean hasParam = false;
            Class<? extends Object> clazz = testInstance.getClass();
            try {
                method = clazz.getMethod(methodName, String.class);
                isStatic = java.lang.reflect.Modifier.isStatic(method.getModifiers());
                hasParam = true;
            } catch (final NoSuchMethodException e) {
                try {
                    method = clazz.getMethod(methodName);
                    isStatic = java.lang.reflect.Modifier.isStatic(method.getModifiers());
                    hasParam = false;
                } catch (final NoSuchMethodException e1) {
                    throw new ActionExecutor.ActionExecutionException(String.format("JAVA_METHOD: no public method '%s(String)' on class '%s'", methodName, testInstance.getClass().getSimpleName()), e);
                }
            }

            // run static
            if (isStatic) {
                if (hasParam) {
                    LOG.debug("Invoking static method {}(\"{}\")", methodName, param);
                    method.invoke(null, param);
                } else {
                    LOG.debug("Invoking static method {}()", methodName);
                    method.invoke(null);
                }
                return;
            }

            if (hasParam) {
                LOG.debug("Invoking method {}(\"{}\")", methodName, param);
                method.invoke(testInstance, param);
            } else {
                LOG.debug("Invoking method {}()", methodName);
                method.invoke(testInstance);
            }

        } catch (final java.lang.reflect.InvocationTargetException e) {
            final Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new ActionExecutor.ActionExecutionException(String.format("JAVA_METHOD: '%s.%s' threw an exception: %s", testInstance.getClass().getSimpleName(), methodName, cause.getMessage()), cause);
        } catch (final Exception e) {
            throw new ActionExecutor.ActionExecutionException(String.format("JAVA_METHOD: failed to invoke '%s.%s': %s", testInstance.getClass().getSimpleName(), methodName, e.getMessage()), e);
        }
    }
}

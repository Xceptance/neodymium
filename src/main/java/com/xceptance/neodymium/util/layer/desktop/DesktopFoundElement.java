package com.xceptance.neodymium.util.layer.desktop;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import javax.swing.JWindow;
import javax.swing.Timer;

import com.xceptance.neodymium.util.layer.ElementCondition;
import com.xceptance.neodymium.util.layer.FoundElement;

/**
 * Represents a desktop element defined entirely by screen coordinates.
 * <p>
 * Does not support DOM operations like getAttribute or getText.
 * </p>
 * 
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class DesktopFoundElement implements FoundElement
{
    private final int x;
    private final int y;
    private final Robot robot;

    public DesktopFoundElement(final int x, final int y)
    {
        this.x = x;
        this.y = y;
        try
        {
            this.robot = new Robot();
            this.robot.setAutoDelay(20);
        }
        catch (final AWTException e)
        {
            throw new RuntimeException("Failed to initialize java.awt.Robot for Desktop layer", e);
        }
    }

    @Override
    public void click()
    {
        ClickHighlighter.showHighlight(new Point(x, y));

        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    @Override
    public void hover()
    {
        ClickHighlighter.showHighlight(new Point(x, y));

        robot.mouseMove(x, y);
    }

    @Override
    public void clear()
    {
        // Highlight all and delete
        click();
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        
        robot.keyPress(KeyEvent.VK_BACK_SPACE);
        robot.keyRelease(KeyEvent.VK_BACK_SPACE);
    }

    @Override
    public void sendKeys(final CharSequence text)
    {
        // First click to focus
        click();
        
        final String str = text.toString().trim();
        
        // Handle chord expressions like "Ctrl+S" or "Ctrl+Q"
        if (str.contains("+"))
        {
            final String[] parts = str.split("\\+");
            final int[] keyCodes = new int[parts.length];
            for (int i = 0; i < parts.length; i++)
            {
                keyCodes[i] = getAwtKeyCode(parts[i].trim());
            }
            
            // Press keys in order
            for (final int kc : keyCodes)
            {
                if (kc != KeyEvent.VK_UNDEFINED)
                {
                    robot.keyPress(kc);
                }
            }
            
            // Release keys in reverse order
            for (int i = keyCodes.length - 1; i >= 0; i--)
            {
                final int kc = keyCodes[i];
                if (kc != KeyEvent.VK_UNDEFINED)
                {
                    robot.keyRelease(kc);
                }
            }
            return;
        }
        
        // Handle single named keys like "Escape", "Enter", "Tab"
        final int singleKeyCode = getAwtKeyCode(str);
        if (singleKeyCode != KeyEvent.VK_UNDEFINED)
        {
            robot.keyPress(singleKeyCode);
            robot.keyRelease(singleKeyCode);
            return;
        }
        
        // Otherwise, type as raw string characters
        for (int i = 0; i < str.length(); i++)
        {
            final char c = str.charAt(i);
            final int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            
            // Check if character requires SHIFT (very basic check)
            final boolean shift = Character.isUpperCase(c) || "~!@#$%^&*()_+{}|:\"<>?".indexOf(c) != -1;
            if (shift)
            {
                robot.keyPress(KeyEvent.VK_SHIFT);
            }
            
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
            
            if (shift)
            {
                robot.keyRelease(KeyEvent.VK_SHIFT);
            }
        }
    }

    private int getAwtKeyCode(final String keyName)
    {
        return switch (keyName.toUpperCase())
        {
            case "CTRL", "CONTROL" -> KeyEvent.VK_CONTROL;
            case "SHIFT"           -> KeyEvent.VK_SHIFT;
            case "ALT"             -> KeyEvent.VK_ALT;
            case "META", "CMD", "COMMAND" -> KeyEvent.VK_META;
            case "ENTER", "RETURN" -> KeyEvent.VK_ENTER;
            case "TAB"             -> KeyEvent.VK_TAB;
            case "ESCAPE", "ESC"   -> KeyEvent.VK_ESCAPE;
            case "BACKSPACE"       -> KeyEvent.VK_BACK_SPACE;
            case "DELETE"          -> KeyEvent.VK_DELETE;
            case "SPACE"           -> KeyEvent.VK_SPACE;
            case "UP"              -> KeyEvent.VK_UP;
            case "DOWN"            -> KeyEvent.VK_DOWN;
            case "LEFT"            -> KeyEvent.VK_LEFT;
            case "RIGHT"           -> KeyEvent.VK_RIGHT;
            case "A"               -> KeyEvent.VK_A;
            case "B"               -> KeyEvent.VK_B;
            case "C"               -> KeyEvent.VK_C;
            case "D"               -> KeyEvent.VK_D;
            case "E"               -> KeyEvent.VK_E;
            case "F"               -> KeyEvent.VK_F;
            case "G"               -> KeyEvent.VK_G;
            case "H"               -> KeyEvent.VK_H;
            case "I"               -> KeyEvent.VK_I;
            case "J"               -> KeyEvent.VK_J;
            case "K"               -> KeyEvent.VK_K;
            case "L"               -> KeyEvent.VK_L;
            case "M"               -> KeyEvent.VK_M;
            case "N"               -> KeyEvent.VK_N;
            case "O"               -> KeyEvent.VK_O;
            case "P"               -> KeyEvent.VK_P;
            case "Q"               -> KeyEvent.VK_Q;
            case "R"               -> KeyEvent.VK_R;
            case "S"               -> KeyEvent.VK_S;
            case "T"               -> KeyEvent.VK_T;
            case "U"               -> KeyEvent.VK_U;
            case "V"               -> KeyEvent.VK_V;
            case "W"               -> KeyEvent.VK_W;
            case "X"               -> KeyEvent.VK_X;
            case "Y"               -> KeyEvent.VK_Y;
            case "Z"               -> KeyEvent.VK_Z;
            default -> KeyEvent.VK_UNDEFINED;
        };
    }

    @Override
    public void selectOption(final String optionText)
    {
        throw new UnsupportedOperationException("selectOption not supported in visual desktop layer");
    }

    @Override
    public void scrollIntoView()
    {
        // Do nothing for now, assume element is visible on desktop
    }

    @Override
    public String getText()
    {
        throw new UnsupportedOperationException("getText not supported in visual desktop layer");
    }

    @Override
    public String getTextContent()
    {
        throw new UnsupportedOperationException("getTextContent not supported in visual desktop layer");
    }

    @Override
    public String getAttribute(final String name)
    {
        throw new UnsupportedOperationException("getAttribute not supported in visual desktop layer");
    }

    @Override
    public String getTagName()
    {
        return "desktop-element";
    }

    @Override
    public boolean isDisplayed()
    {
        return true;
    }

    @Override
    public boolean exists()
    {
        return true;
    }

    @Override
    public boolean isVisible()
    {
        return true;
    }

    @Override
    public boolean isHidden()
    {
        return false;
    }

    @Override
    public boolean isFocused()
    {
        throw new UnsupportedOperationException("isFocused not supported in visual desktop layer");
    }

    @Override
    public boolean isSelected()
    {
        throw new UnsupportedOperationException("isSelected not supported in visual desktop layer");
    }

    @Override
    public Map<String, String> getAllAttributes()
    {
        return Collections.emptyMap();
    }

    @Override
    public void waitUntilVisible(final Duration timeout)
    {
    }

    @Override
    public void waitUntilExist(final Duration timeout)
    {
    }

    @Override
    public boolean matchesCondition(final ElementCondition condition)
    {
        return true;
    }

    @Override
    public void assertCondition(final ElementCondition condition)
    {
    }

    private static class ClickHighlighter extends JWindow
{

    private final Point point;

    public ClickHighlighter(Point point)
    {
        this.point = point;

        setBackground(new Color(0, 0, 0, 0));
        setBounds(point.x - 30, point.y - 30, 60, 60);
        setAlwaysOnTop(true);
    }

    @Override
    public void paint(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(4));
        g2.setColor(Color.RED);
        g2.drawOval(5, 5, 50, 50);
    }

    public static void showHighlight(Point p)
    {
        ClickHighlighter h = new ClickHighlighter(p);
        h.setVisible(true);

        new Timer(500, e -> {
            h.dispose();
            ((Timer) e.getSource()).stop();
        }).start();
    }
}
}

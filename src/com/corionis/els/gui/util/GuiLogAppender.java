package com.corionis.els.gui.util;

import com.corionis.els.Configuration;
import com.corionis.els.Context;

import com.corionis.els.Utils;
import com.corionis.els.gui.Startup;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;

@Plugin(name = "GuiLogAppender", category = "Core", elementType = "appender", printObject = true)
public class GuiLogAppender extends AbstractAppender
{
    private static ArrayList<String> preBuffer = null;
    private static Startup startup = null;
    private Context context = null;
    private boolean hasBeenShown = false;

    public GuiLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions)
    {
        super(name, filter, layout, ignoreExceptions);
    }

    @Override
    public synchronized void append(LogEvent event)
    {
        byte[] data = getLayout().toByteArray(event);
        String line = new String(data).trim() + System.getProperty("line.separator");

        if (isGuiInitializing())
        {
            preBuffer(line);
            if (context.cfg.isDefaultNavigator() || context.cfg.isGui())
                showStartup(line);
        }
        else
        {
            if (preBuffer != null)
            {
                hideStartup();
                dumpPreBuffer();
            }
            appendGuiLogs(line);
        }
    }

    private void appendGuiLogs(String line)
    {
        context.mainFrame.textAreaLog.append(line);
    }

    @PluginFactory
    public static GuiLogAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("otherAttribute") String otherAttribute)
    {
        if (name == null)
        {
            LOGGER.error("No name provided for GuiLogAppender");
            return null;
        }
        if (layout == null)
        {
            layout = PatternLayout.createDefaultLayout();
        }
        GuiLogAppender appender = new GuiLogAppender(name, filter, layout, true);
        return appender;
    }

    private void dumpPreBuffer()
    {
        for (String preLine : preBuffer)
        {
            if (preLine != null)
            {
                appendGuiLogs(preLine);
            }
        }
        preBuffer = null;
    }

    public Context getContext()
    {
        return this.context;
    }

    public Component getStartup()
    {
        return startup;
    }

    private void hideStartup()
    {
        if (startup != null)
        {
            startup.setVisible(false);
        }
        hasBeenShown = true;
        startup = null;
    }

    public boolean isGuiInitializing()
    {
        if (!hasBeenShown)
        {
            if (context == null ||
                    context.mainFrame == null ||
                    context.navigator == null ||
                    context.mainFrame.textAreaLog == null ||
                    (context.main.primaryExecution ? context.mainFrame.textAreaLog.isVisible() == false : context.mainFrame.textAreaLog.isShowing() == false))
                return true;
        }
        return false;
    }

    public boolean isStartupActive()
    {
        if (startup != null && startup.isVisible())
            return true;
        return false;
    }

    private void preBuffer(String line)
    {
        if (preBuffer == null)
            preBuffer = new ArrayList<String>();
        preBuffer.add(line);
    }

    private void redraw()
    {
        Graphics gfx = startup.startupTextField.getGraphics();
        if (gfx != null)
            startup.startupTextField.update(gfx);
        startup.startupTextField.repaint();
        startup.repaint();
    }

    public void setContext(Context context)
    {
        this.context = context;
    }

    private void showStartup(String msg)
    {
        if (startup == null)
        {
            try
            {
                context.preferences.initLookAndFeel(context.cfg.APPLICATION_NAME, true);
                startup = new Startup();
                if (startup != null)
                {
                    startup.setIconImage(new ImageIcon(getClass().getResource("/els-logo-98px.png")).getImage());
                    startup.setTitle(context.cfg.getNavigatorName());
                    startup.labelVersion.setText("Version " + Configuration.getBuildVersionName());

                    if (context.preferences != null && Utils.isOnScreen(context.preferences.getAppXpos(), context.preferences.getAppYpos()))
                    {
                        int x = context.preferences.getAppXpos() + (context.preferences.getAppWidth() / 2) - (startup.getWidth() / 2);
                        int y = context.preferences.getAppYpos() + (context.preferences.getAppHeight() / 2) - (startup.getHeight() / 2);
                        startup.setLocation(x, y);
                    }
                    else
                    {
                        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                        int x = screenSize.width / 2 - startup.getWidth() / 2;
                        if (x < 0)
                            x = 1;
                        int y = screenSize.height / 2 - startup.getHeight() / 2;
                        if (y < 0)
                            y = 1;
                        startup.setLocation(x, y);
                    }
                    startup.setVisible(true);
                }
            }
            catch (Exception ignoredEx)
            {
            }
        }

        startup.startupTextField.setText(msg.substring(29));
        redraw();
    }

    @Override
    public void stop()
    {
    }

}

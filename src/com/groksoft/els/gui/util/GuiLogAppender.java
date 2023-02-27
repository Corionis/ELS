package com.groksoft.els.gui.util;

import com.groksoft.els.Context;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.ArrayList;

@Plugin(name = "GuiLogAppender", category = "Core", elementType = "appender", printObject = true)
public class GuiLogAppender extends AbstractAppender
{
    private static ArrayList<String> preBuffer = null;
    private Context context = null;

    public GuiLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions)
    {
        super(name, filter, layout, ignoreExceptions, null);
    }

    @Override
    public synchronized void append(LogEvent event)
    {
        byte[] data = getLayout().toByteArray(event);
        String line = new String(data).trim() + System.getProperty("line.separator");

        if (context == null || context.navigator == null)
        {
            if (preBuffer == null)
                preBuffer = new ArrayList<String>();
            preBuffer.add(line);
        }
        else
        {
            if (preBuffer != null)
            {
                for (String preLine : preBuffer)
                {
                    appendGuiLogs(preLine);
                }
                preBuffer = null;
            }
            appendGuiLogs(line);
        }
    }

    private void appendGuiLogs(String line)
    {
        context.mainFrame.textAreaLog.append(line);
        context.mainFrame.textAreaOperationLog.append(line);
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

    public void setContext(Context context)
    {
        this.context = context;
    }

    @Override
    public void stop()
    {
    }

}

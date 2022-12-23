package com.groksoft.els.gui.util;

import com.groksoft.els.gui.GuiContext;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Plugin(name = "GuiLogAppender", category = "Core", elementType = "appender", printObject = true)
public class GuiLogAppender extends AbstractAppender
{
    private GuiContext guiContext = null;

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

    public GuiLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions)
    {
        super(name, filter, layout);
    }

    @Override
    public void append(LogEvent event)
    {
        if (guiContext != null)
        {
            byte[] data = getLayout().toByteArray(event);
            guiContext.mainFrame.textAreaLog.append(new String(data).trim() + System.getProperty("line.separator"));
            guiContext.mainFrame.textAreaOperationLog.append(new String(data).trim() + System.getProperty("line.separator"));
        }
    }

    public void setTextArea(GuiContext guiContext)
    {
        this.guiContext = guiContext;
    }

    @Override
    public void stop()
    {
    }

}

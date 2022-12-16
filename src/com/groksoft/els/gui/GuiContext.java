package com.groksoft.els.gui;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.gui.operations.Operations;
import com.groksoft.els.gui.browser.Browser;

/**
 * GuiContext helper class to shorter arguments
 */
public class GuiContext
{
    // Base ELS
    public Configuration cfg;
    public Context context;

    // Navigator-related
    public Navigator navigator;
    public Browser browser;
    public Operations operations = null;
    public MainFrame mainFrame;
    public Preferences preferences;
    public Progress progress;
}

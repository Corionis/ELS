package com.groksoft.els.gui;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.gui.operations.OperationsUI;
import com.groksoft.els.gui.browser.Browser;
import com.groksoft.els.repository.Hints;

/**
 * GuiContext helper class to shorter arguments
 */
public class GuiContext
{
    // Base ELS
    public Configuration cfg;
    public Context context;
    public Hints hints;

    // Navigator-related
    public Browser browser;
    public MainFrame mainFrame;
    public Navigator navigator;
    public OperationsUI operationsUI = null;
    public Preferences preferences;
    public Progress progress;
}

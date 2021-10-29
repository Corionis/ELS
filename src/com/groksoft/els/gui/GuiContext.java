package com.groksoft.els.gui;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Main;

import javax.swing.filechooser.FileSystemView;

/**
 * GuiContext helper class to shorter arguments
 */
public class GuiContext
{
    // Base ELS
    Main els;
    Configuration cfg;
    Context context;

    // Navigator-related
    Navigator navigator;
    Browser browser;
    MainFrame form;
    Preferences preferences;
    FileSystemView fileSystemView;
}

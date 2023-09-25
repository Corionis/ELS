package com.corionis.els.gui.jobs;

import com.corionis.els.tools.AbstractTool;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

abstract public class AbstractToolDialog extends JDialog
{
    protected ArrayList<AbstractTool> deletedTools;

    public AbstractToolDialog(Window owner)
    {
        super(owner);
        deletedTools = new ArrayList<AbstractTool>();
    }

    public ArrayList<AbstractTool> getDeletedTools()
    {
        return deletedTools;
    }

    abstract     public JTable getConfigItems();

}

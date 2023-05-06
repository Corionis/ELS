package com.groksoft.els.jobs;

import com.groksoft.els.gui.browser.NavTreeUserObject;

import javax.swing.*;
import javax.swing.tree.TreePath;

public class Origin
{
    private String location;
    private int type;

    transient NavTreeUserObject tuo;
    transient JTree sourceTree = null;
    transient TreePath treePath = null;
    transient JTable sourceTable = null;
    transient int tableRow = -1;

    private Origin()
    {
    }

    public Origin(JTree sourceTree, TreePath treePath, NavTreeUserObject tuo)
    {
        this.sourceTree = sourceTree;
        this.treePath = treePath;
        this.tuo = tuo;
        this.location = tuo.getPath();
        this.type = tuo.type;
    }

    public Origin(JTable sourceTable, TreePath treePath, int tableRow, NavTreeUserObject tuo)
    {
        this.sourceTable = sourceTable;
        this.treePath = treePath;
        this.tableRow = tableRow;
        this.tuo = tuo;
        this.location = tuo.getPath();
        this.type = tuo.type;
    }

    public Origin clone()
    {
        Origin o = new Origin();
        o.sourceTree = this.sourceTree;
        o.treePath = this.treePath;
        o.sourceTable = this.sourceTable;
        o.tableRow = this.tableRow;
        o.tuo = this.tuo;
        o.location = this.location;
        o.type = this.type;
        return o;
    }

    public String getLocation()
    {
        return location;
    }

    public int getType()
    {
        return type;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public void setType(int type)
    {
        this.type = type;
    }

}

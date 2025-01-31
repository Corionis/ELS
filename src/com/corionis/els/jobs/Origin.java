package com.corionis.els.jobs;

import com.corionis.els.gui.browser.NavTreeUserObject;

import javax.swing.*;
import javax.swing.tree.TreePath;

public class Origin
{
    private String location;
    private int type;

    public transient NavTreeUserObject tuo;
    public transient JTree sourceTree = null;
    public transient TreePath treePath = null;
    public transient JTable sourceTable = null;
    public transient int tableRow = -1;

    private Origin()
    {
    }

    public Origin(JTree sourceTree, TreePath treePath, NavTreeUserObject tuo)
    {
        this.sourceTree = sourceTree;
        this.treePath = treePath;
        this.tuo = tuo;
        this.location = tuo.getRelativePath();
        this.type = tuo.type;
    }

    public Origin(JTable sourceTable, TreePath treePath, int tableRow, NavTreeUserObject tuo)
    {
        this.sourceTable = sourceTable;
        this.treePath = treePath;
        this.tableRow = tableRow;
        this.tuo = tuo;
        this.location = tuo.getRelativePath();
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
